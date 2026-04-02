# TodoIt — Complete Android Implementation Blueprint

> **TL;DR:** Build a production-quality, offline-first Todo app on top of the existing Compose shell using **MVVM + Clean Architecture**. Data is persisted locally in Room and synced bidirectionally with Google Sheets as the cloud backend. Smart scheduling, geofencing-based location awareness, and a deterministic "What To Do Now" scoring engine round out the feature set.

---

## A. Requirement Validation

### ✅ Confirmed Complete

All core requirements are internally consistent. Key edge cases to address explicitly during implementation:

| Edge Case | Risk | Mitigation |
|---|---|---|
| Circular group nesting | Data corruption | Enforce DAG-only writes; validate `parent_id` chain at write time |
| Task recurrence + group schedule conflict | Missed/duplicate notifications | Task-level schedule always wins; fallback to group schedule if `inherit_schedule = true` |
| Task recurrence + task deletion | Orphan occurrences | Soft-delete with `deleted_at`; recurrence expander checks flag |
| Geofence + app killed | Missed proximity alert | Use `GeofencingClient` (system-level, survives app kill) |
| Offline CRUD during pending sync | Sync conflict | All local writes carry `updated_at` (epoch ms); last-write-wins on sync merge |
| Thousands of tasks + scoring | UI jank | Score pre-computed in background; cached in Room column `score_cache` |
| Google Sheets API quota (300 req/min) | Rate limiting | Batch all reads/writes; exponential backoff with jitter |

---

## B. System Architecture

### Layer Breakdown

```
┌─────────────────────────────────────────────┐
│           Presentation Layer                │
│  Compose Screens → ViewModels → StateFlow   │
├─────────────────────────────────────────────┤
│              Domain Layer                   │
│  Use Cases · Domain Models · Repository     │
│  Interfaces · Scoring Engine · Scheduler    │
├─────────────────────────────────────────────┤
│               Data Layer                    │
│  Room (local) ←→ SyncManager ←→ Sheets API │
│  LocationRepository · NotificationScheduler │
└─────────────────────────────────────────────┘
```

### Package Structure

```
com.example.todoit/
├── di/                         # Hilt modules
├── presentation/
│   ├── home/                   # "What To Do Now" screen
│   ├── groups/                 # Group tree browser
│   ├── todos/                  # Todo detail + task list
│   ├── settings/               # Auth + sync prefs
│   └── common/                 # Shared Compose components
├── domain/
│   ├── model/                  # Pure Kotlin data classes (no Android)
│   ├── usecase/
│   ├── repository/             # Interfaces only
│   └── engine/                 # Scoring, Scheduler, Recurrence
├── data/
│   ├── local/
│   │   ├── db/                 # RoomDatabase, DAOs
│   │   └── entity/             # @Entity classes
│   ├── remote/
│   │   ├── sheets/             # Sheets API service + DTOs
│   │   └── auth/               # Google Sign-In wrapper
│   ├── sync/                   # SyncWorker (WorkManager)
│   ├── location/               # FusedLocation + Geofencing
│   └── repository/             # Implementations of domain interfaces
└── notification/               # AlarmManager + BroadcastReceivers
```

### State Management Pattern

```
Screen ←observes— ViewModel.uiState: StateFlow<UiState>
         ↕ sends events
ViewModel —calls→ UseCase —calls→ Repository Interface
                                    ↓           ↓
                                 RoomDao    SheetsApiService
```

All ViewModels expose a single sealed `UiState` class: `Loading`, `Success(data)`, `Error(message)`.

---

## C. Data Modeling

### Entity: `Group`

| Field | Type | Notes |
|---|---|---|
| `id` | `String` (UUID) | Primary key |
| `parent_id` | `String?` | `null` = root; enables unlimited nesting |
| `name` | `String` | |
| `color` | `String?` | Hex color for UI |
| `schedule_id` | `String?` | FK → Schedule |
| `created_at` | `Long` | Epoch ms |
| `updated_at` | `Long` | For sync conflict resolution |
| `deleted_at` | `Long?` | Soft delete |
| `synced_at` | `Long?` | Last confirmed sync to Sheets |

**Nesting strategy:** Adjacency list (`parent_id`). Fetch all groups → build in-memory tree with recursive `buildTree()`. Acceptable for thousands of groups; no CTE SQL needed.

---

### Entity: `TodoItem`

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | UUID |
| `group_id` | `String` | FK → Group |
| `title` | `String` | |
| `description` | `String?` | |
| `created_at` | `Long` | |
| `updated_at` | `Long` | |
| `deleted_at` | `Long?` | |
| `synced_at` | `Long?` | |

---

### Entity: `Task`

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | UUID |
| `todo_id` | `String` | FK → TodoItem |
| `title` | `String` | |
| `status` | `Enum` | `PENDING`, `IN_PROGRESS`, `DONE` |
| `priority` | `Int` | 1=Critical, 2=High, 3=Medium, 4=Low |
| `due_date` | `Long` | Epoch ms; required |
| `start_time` | `Long?` | Epoch ms; optional |
| `reminder_at` | `Long?` | Exact epoch ms for alarm |
| `location_id` | `String?` | FK → Location |
| `schedule_id` | `String?` | If null, inherits from parent Group |
| `inherit_schedule` | `Boolean` | Default `true` |
| `recurrence_id` | `String?` | FK → Recurrence; null = one-time |
| `recurrence_instance_date` | `Long?` | For expanded recurrence instances |
| `parent_task_id` | `String?` | Root task for recurrence series |
| `score_cache` | `Float` | Pre-computed "What To Do Now" score |
| `created_at` | `Long` | |
| `updated_at` | `Long` | |
| `deleted_at` | `Long?` | |
| `synced_at` | `Long?` | |

**Priority levels:**

| Value | Label | Score Weight |
|---|---|---|
| 1 | Critical | 40 pts |
| 2 | High | 30 pts |
| 3 | Medium | 20 pts |
| 4 | Low | 10 pts |

---

### Entity: `Schedule`

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | UUID |
| `name` | `String` | e.g., "Weekdays only" |
| `active_days` | `String` | JSON array: `[1,2,3,4,5]` (Mon–Fri; 1=Mon, 7=Sun) |
| `start_time_of_day` | `Int?` | Minutes from midnight (e.g., 540 = 9:00 AM) |
| `end_time_of_day` | `Int?` | Minutes from midnight |
| `updated_at` | `Long` | |
| `deleted_at` | `Long?` | |

---

### Entity: `Recurrence`

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | UUID |
| `type` | `Enum` | `DAILY`, `WEEKLY`, `CUSTOM` |
| `interval` | `Int` | Every N days (CUSTOM); 1 for DAILY/WEEKLY |
| `days_of_week` | `String?` | JSON array; used only for WEEKLY (e.g., `[1,3,5]`) |
| `end_date` | `Long?` | Epoch ms; null = no end |
| `max_occurrences` | `Int?` | Alternative to end_date |
| `updated_at` | `Long` | |

---

### Entity: `Location`

| Field | Type | Notes |
|---|---|---|
| `id` | `String` | UUID |
| `label` | `String` | Human-readable name |
| `latitude` | `Double` | |
| `longitude` | `Double` | |
| `radius_meters` | `Float` | Geofence radius; min 100m recommended |
| `updated_at` | `Long` | |
| `deleted_at` | `Long?` | |

---

## D. Google Sheets Design

### Sheet Layout — One Sheet Per Entity

| Sheet Name | Columns (in order) |
|---|---|
| `groups` | `id, parent_id, name, color, schedule_id, created_at, updated_at, deleted_at` |
| `todo_items` | `id, group_id, title, description, created_at, updated_at, deleted_at` |
| `tasks` | `id, todo_id, title, status, priority, due_date, start_time, reminder_at, location_id, schedule_id, inherit_schedule, recurrence_id, recurrence_instance_date, parent_task_id, created_at, updated_at, deleted_at` |
| `schedules` | `id, name, active_days, start_time_of_day, end_time_of_day, updated_at, deleted_at` |
| `recurrences` | `id, type, interval, days_of_week, end_date, max_occurrences, updated_at` |
| `locations` | `id, label, latitude, longitude, radius_meters, updated_at, deleted_at` |
| `sync_meta` | `entity_name, last_sync_at` (one row per entity for delta sync) |

### Example Row — `tasks` sheet

```
abc-123 | todo-xyz | Buy groceries | PENDING | 3 | 1746000000000 | | 1745999700000 | loc-001 | | true | | | | 1745800000000 | 1745900000000 |
```

### Relationship Mapping

All foreign keys are stored as plain string IDs. The app resolves joins in the data layer (no Sheets-side joins). Reference integrity is enforced by the app, not Sheets.

### API Quota Considerations

| Concern | Mitigation |
|---|---|
| 300 read requests/min (free tier) | One `batchGet` call per sync covers all 7 sheets |
| 300 write requests/min | One `batchUpdate` per sync push; group all dirty rows |
| Row limit (10M cells/sheet) | Thousands of tasks ≪ limit; no concern |
| Rate-limit errors (HTTP 429) | Exponential backoff with jitter |

**Total: 2 API calls per full sync cycle** (1 batchGet + 1 batchUpdate)

---

## E. Offline & Sync Strategy

### Core Principle

All reads/writes go to **Room first**. The app never reads directly from Sheets at query time. Google Sheets is a backup store and sync target only.

### Sync Flow

```
PULL (Sheets → Room):
  1. Read sync_meta.last_sync_at per entity
  2. batchGet all sheets (full read; delta via updated_at filter in-memory)
  3. For each remote row:
     a. If local row missing → INSERT
     b. If remote.updated_at > local.updated_at → UPDATE local
     c. If remote.deleted_at set → soft-delete local
  4. Update sync_meta.last_sync_at = now()

PUSH (Room → Sheets):
  1. Query Room for all rows WHERE updated_at > last_sync_at AND synced_at < updated_at
  2. Batch all dirty rows into one batchUpdate call
  3. On success: UPDATE synced_at = now() for all pushed rows
  4. Update sync_meta.last_sync_at = now()
```

### Conflict Resolution

**Strategy: Last-Write-Wins using `updated_at` timestamp**
- All local writes set `updated_at = System.currentTimeMillis()`
- On merge: higher `updated_at` wins
- **Trade-off:** Acceptable for single-user; would need vector clocks for multi-user

### Background Sync: WorkManager

```kotlin
// Periodic sync every 15 minutes (WorkManager minimum)
PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
    .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build())
    .build()

// Expedited one-time sync on every CRUD operation (debounced 30s)
OneTimeWorkRequestBuilder<SyncWorker>()
    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
    .build()
```

On HTTP 429 or 5xx: `return Result.retry()` — WorkManager handles exponential backoff automatically.

---

## F. API Integration

### Authentication

```kotlin
GoogleSignIn.getClient(context, GoogleSignInOptions.Builder()
    .requestScopes(Scope(SheetsScopes.SPREADSHEETS))
    .requestEmail()
    .build())

// Credential passed to Sheets client
val credential = GoogleAccountCredential
    .usingOAuth2(context, listOf(SheetsScopes.SPREADSHEETS))
    .setSelectedAccount(googleAccount.account)

val sheetsService = Sheets.Builder(AndroidHttp.newCompatibleTransport(), 
    JacksonFactory.getDefaultInstance(), credential)
    .setApplicationName("TodoIt")
    .build()
```

### Batching Pattern

```kotlin
// Pull — 1 API call
sheetsService.spreadsheets().values()
    .batchGet(spreadsheetId)
    .setRanges(listOf("groups!A:Z", "todo_items!A:Z", "tasks!A:Z", ...))
    .execute()

// Push — 1 API call
sheetsService.spreadsheets().values()
    .batchUpdate(spreadsheetId, BatchUpdateValuesRequest()
        .setValueInputOption("RAW")
        .setData(dirtyRowsList))
    .execute()
```

### Error Handling & Retry

```pseudocode
fun executeWithRetry(block, maxRetries = 5):
  attempt = 0
  while attempt < maxRetries:
    try:
      return block()
    catch GoogleJsonResponseException e:
      if e.statusCode in [429, 500, 503]:
        delay = min(2^attempt * 1000ms + random(0..1000ms), 64000ms)
        sleep(delay)
        attempt++
      else if e.statusCode == 401:
        refreshToken() then retry once
      else:
        throw e  // 403, 404 → propagate to UI
  throw MaxRetriesExceededException
```

---

## G. Key Algorithms

### 1. "What To Do Now" Scoring

```pseudocode
fun scoreTask(task, userLocation, now): Float
    if task.status == DONE: return -1.0          // excluded
    if not isScheduleActive(task, now): return -1.0  // excluded

    score = 0.0

    // Priority component (0–40 pts)
    score += when task.priority:
        1 (Critical) → 40
        2 (High)     → 30
        3 (Medium)   → 20
        4 (Low)      → 10

    // Due date urgency component (0–40 pts)
    hoursUntilDue = (task.due_date - now) / 3_600_000
    score += when:
        hoursUntilDue < 0  → 40  // overdue
        hoursUntilDue < 1  → 35
        hoursUntilDue < 6  → 25
        hoursUntilDue < 24 → 15
        hoursUntilDue < 72 → 5
        else               → 0

    // In-progress bonus (0–10 pts)
    if task.status == IN_PROGRESS: score += 10

    // Location proximity component (0–20 pts)
    if task.location != null and userLocation != null:
        dist = haversine(userLocation, task.location.latLng)
        score += when:
            dist <= task.location.radius      → 20
            dist <= task.location.radius * 3  → 10
            dist <= 5000m                     → 5
            else                              → 0
    elif task.location == null:
        score += 5  // no location constraint = neutral bonus

    return score

// Sort descending; ties broken by due_date ascending
// Max possible score: 40 + 40 + 10 + 20 = 110 pts
```

---

### 2. Schedule Evaluation

```pseudocode
fun isScheduleActive(task, now): Boolean
    schedule = task.schedule
               ?: (if task.inherit_schedule: task.todo.group.schedule else null)
               ?: return true  // no schedule = always active

    dayOfWeek = now.dayOfWeek   // 1=Mon, 7=Sun
    if dayOfWeek not in schedule.active_days: return false

    if schedule.start_time_of_day != null:
        minuteOfDay = now.hour * 60 + now.minute
        if minuteOfDay < schedule.start_time_of_day: return false
        if schedule.end_time_of_day != null:
            if minuteOfDay > schedule.end_time_of_day: return false

    return true
```

---

### 3. Recurrence Expansion

```pseudocode
// Eagerly generate instances up to 90 days ahead; stored as real Room rows
fun expandRecurrence(rootTask, recurrence, from, upTo): List<Task>
    occurrences = []
    current = rootTask.due_date

    while current <= upTo:
        if current >= from and not occurrenceExists(rootTask.id, current):
            occurrences.add(rootTask.copy(
                id = newUUID(),
                parent_task_id = rootTask.id,
                due_date = current,
                recurrence_instance_date = current,
                status = PENDING,
                score_cache = 0f
            ))
        current = nextOccurrence(current, recurrence)
        if recurrence.max_occurrences != null
            and occurrences.size >= recurrence.max_occurrences: break
        if recurrence.end_date != null
            and current > recurrence.end_date: break

    return occurrences

fun nextOccurrence(current, recurrence): Long
    return when recurrence.type:
        DAILY  → current + 86_400_000L * recurrence.interval
        WEEKLY → advance to next matching day_of_week in recurrence.days_of_week
        CUSTOM → current + 86_400_000L * recurrence.interval
```

---

### 4. Location Proximity Detection (Battery-Efficient)

**Strategy:** Never poll GPS. Use two complementary system APIs.

```pseudocode
// PRIMARY: GeofencingClient (system-managed, 0 app wakeups, ~0% battery)
fun registerGeofencesForActiveTasks(tasks):
    geofences = tasks
        .filter { it.location != null and it.status != DONE }
        .map { task →
            Geofence.Builder()
                .setRequestId(task.id)
                .setCircularRegion(lat, lng, radius)
                .setTransitionTypes(GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(NEVER_EXPIRE)
                .build()
        }
    geofencingClient.addGeofences(GeofencingRequest(geofences), pendingIntent)

// SECONDARY: Passive location updates for score_cache refresh (opportunistic)
fusedLocationClient.requestLocationUpdates(
    LocationRequest.Builder(Priority.PRIORITY_PASSIVE, 300_000L).build(),
    passiveLocationCallback,
    Looper.getMainLooper()
)

// On GEOFENCE_TRANSITION_ENTER (GeofenceBroadcastReceiver):
fun onGeofenceEnter(geofenceId):
    task = roomDb.taskDao().findById(geofenceId)
    if task != null and task.status != DONE:
        notificationManager.showProximityNotification(task)
```

---

## H. Android Implementation

### Key Libraries

| Library | Version Target | Purpose |
|---|---|---|
| `hilt-android` + `hilt-compiler` | 2.51+ | Dependency injection |
| `room-runtime` + `room-ktx` + `room-compiler` | 2.6+ | Local database |
| `lifecycle-viewmodel-compose` | 2.8+ | ViewModel + StateFlow |
| `navigation-compose` | 2.8+ | Screen navigation |
| `work-runtime-ktx` | 2.9+ | Background sync |
| `play-services-location` | 21+ | FusedLocation + Geofencing |
| `play-services-auth` | 21+ | Google Sign-In |
| `google-api-services-sheets` | v4-rev612 | Sheets API client |
| `google-api-client-android` | 2.4+ | Auth + HTTP transport |
| `paging3-compose` | 3.3+ | Lazy task list |
| `kotlinx-serialization-json` | 1.7+ | JSON for schedule/recurrence fields |
| `mockk` + `turbine` + `junit` | latest | Unit tests |
| `room-testing` | 2.6+ | DAO integration tests |

---

## I. Notification System

### Time-Based Reminders (Exact)

```kotlin
// On task save with reminder_at set:
alarmManager.setExactAndAllowWhileIdle(
    AlarmManager.RTC_WAKEUP,
    task.reminderAt,
    PendingIntent.getBroadcast(context, task.id.hashCode(),
        Intent(context, ReminderBroadcastReceiver::class.java)
            .putExtra("TASK_ID", task.id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
)
```

### Reboot Persistence

```xml
<receiver android:name=".notification.BootReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
    </intent-filter>
</receiver>
```

`BootReceiver.onReceive()` → queries Room for all tasks with `reminder_at > now` → re-registers all alarms and geofences.

### Permission Handling Matrix

| Feature | Permission Required | Denied Fallback |
|---|---|---|
| Exact alarms (API 31+) | `SCHEDULE_EXACT_ALARM` | Downgrade to `setAndAllowWhileIdle` |
| Location geofencing | `ACCESS_FINE_LOCATION` + `ACCESS_BACKGROUND_LOCATION` | Disable location features; show in-app explanation |
| Notifications (API 33+) | `POST_NOTIFICATIONS` | Show in-app banner instead |

---

## J. Performance Strategy

### Room Indexing

```kotlin
@Entity(
    tableName = "tasks",
    indices = [
        Index("todo_id"),
        Index("status"),
        Index("due_date"),
        Index("updated_at"),      // dirty-row sync query
        Index("deleted_at"),
        Index("score_cache")      // "What To Do Now" sort
    ]
)
```

### Paging 3 for Task Lists

```kotlin
// DAO
@Query("SELECT * FROM tasks WHERE deleted_at IS NULL ORDER BY score_cache DESC")
fun getTasksPaged(): PagingSource<Int, TaskEntity>

// ViewModel
val tasks = Pager(PagingConfig(pageSize = 30)) { taskDao.getTasksPaged() }
    .flow.cachedIn(viewModelScope)
```

### Minimizing Sheets API Calls

| Strategy | Detail |
|---|---|
| Full sheet read once per sync | `batchGet` all 7 sheets in 1 call; filter delta in-memory |
| Dirty tracking via `synced_at` | Only push rows where `updated_at > synced_at` |
| No real-time API reads | UI always reads from Room; Sheets is backup only |
| Sync debounce | Max 1 expedited push per 30 seconds after edits |
| `score_cache` invalidation | Recomputed on passive location update + screen resume |

---

## K. Security Considerations

| Concern | Implementation |
|---|---|
| OAuth scope | `SheetsScopes.SPREADSHEETS` only — narrowest possible |
| Token storage | Google Sign-In manages tokens in system account; never stored in SharedPreferences/Room |
| No hardcoded credentials | OAuth client ID in `google-services.json` (excluded via `.gitignore`) |
| Sheet access | Created in user's own Drive; private by default; not shared |
| No sensitive data in logs | Strip at INFO+ level in release builds via ProGuard |
| ProGuard | `isMinifyEnabled = true` in release; add rules for Google API client classes |
| Spreadsheet auto-creation | Requires additional `DriveScopes.FILE` scope — acceptable trade-off for smoother UX |

---

## L. Development Roadmap

| Phase | Scope | Complexity | Est. Weeks |
|---|---|---|---|
| **1 — MVP** | Room setup, domain models, Hilt DI, full CRUD screens (Groups → Todos → Tasks), NavigationSuite routing, basic ViewModel + StateFlow | Medium | 3–4 |
| **2 — Offline + Sync** | Google Sign-In, Sheets API integration, SyncWorker (pull + push), conflict resolution, sync status UI | High | 3–4 |
| **3 — Smart Filtering** | Schedule entity + evaluation, recurrence expansion engine, "What To Do Now" scoring + screen | High | 3–4 |
| **4 — Location Features** | Location entity, Geofencing registration, BootReceiver, AlarmManager exact reminders, permission flows + fallbacks | Medium–High | 2–3 |
| **5 — Polish & Optimization** | Paging 3, Room indexes, score_cache background precomputation, Sheets batching tuning, ProGuard, UI polish | Medium | 2–3 |

**Total estimated range: 13–18 developer-weeks**

---

## M. Testing Strategy

### Unit Tests (Domain Layer — Pure Kotlin, no Android deps)

| Test Target | Cases |
|---|---|
| `scoreTask()` | Overdue critical + nearby location = max; DONE task = -1; no schedule = always active; no location = neutral bonus |
| `isScheduleActive()` | Weekend task on weekday = false; within time window = true; null schedule = always true |
| `expandRecurrence()` | Daily 7 days → 7 rows; max_occurrences cap; end_date cap; no duplicate instances |
| `buildGroupTree()` | Root groups; N-level nesting; orphaned parent_id (deleted parent) handled gracefully |

### Integration Tests (Data Layer — Room)

| Test Target | Cases |
|---|---|
| `TaskDao` | Insert + query by status; soft-delete excludes from active queries; dirty row query |
| `SyncWorker` | Mock Sheets API; pull merges correctly; push sends only dirty rows; `synced_at` updated |
| Conflict resolution | `remote.updated_at > local.updated_at` → remote wins; local newer → local retained |

### Edge Case Scenarios

| Scenario | Test Type |
|---|---|
| Recurrence instance + parent soft-deleted | Unit: orphan instances marked deleted in cascade |
| Group deleted with child groups | Integration: recursive soft-delete cascade |
| Offline 7 days then reconnect + sync | Integration: all dirty rows pushed; no data loss; no duplicates |
| `SCHEDULE_EXACT_ALARM` permission denied | UI test: downgrade path triggers; no crash |
| Background location permission denied | Unit: geofence registration skipped gracefully |
| Score tied tasks | Unit: sorted by `due_date` ascending as tiebreaker |

---

## Further Considerations

1. **Spreadsheet creation UX:** App should auto-create the Sheets spreadsheet on first sign-in using Drive API. Requires adding `DriveScopes.FILE` scope — worth the small scope expansion for smoother onboarding. Store the created `spreadsheetId` in `DataStore<Preferences>`.

2. **Recurrence approach trade-off:** This plan uses **eager expansion** (90-day lookahead stored as real Room rows). Alternative is lazy/virtual expansion (compute at query time) — simpler storage but impossible to query, notify, or score efficiently. Eager is strongly preferred.

3. **`score_cache` invalidation strategy — Recommended: Option C** — Recompute triggered by passive location update + screen resume lifecycle event. This avoids continuous recomputation while keeping scores fresh when location actually changes.
