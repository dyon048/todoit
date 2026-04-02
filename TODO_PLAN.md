# TodoIt — Implementation Todo Plan

> Grounded in the current codebase state:
> - ✅ Compose + Material3 + NavigationSuiteScaffold shell exists (`MainActivity.kt`)
> - ✅ Basic theme exists (`ui/theme/`)
> - ❌ No Room, Hilt, WorkManager, Sheets API, navigation, or domain logic yet

---

## Phase 1 — MVP (Local CRUD, No Sync)
**Goal:** A fully functional offline-only todo app with groups, todos, and tasks.

---

### 1.1 — Project Setup & Dependencies

- [ ] **Add Kotlin plugins to `libs.versions.toml` and `build.gradle.kts`**
  - Add `kotlin-android` plugin alias
  - Add `kotlin-kapt` plugin alias (for Room/Hilt annotation processing)
  - Add `hilt-android-gradle-plugin` plugin alias

- [ ] **Add all library versions to `libs.versions.toml`**
  - `hilt = "2.51.1"`
  - `room = "2.7.0"`
  - `lifecycle-viewmodel = "2.8.7"`
  - `navigation-compose = "2.8.9"`
  - `kotlinx-serialization = "1.7.3"`
  - `datastore = "1.1.4"`
  - `work = "2.10.1"`
  - `play-services-location = "21.3.0"`
  - `play-services-auth = "21.3.0"`
  - `google-api-services-sheets = "v4-rev20250304-2.0.0"`
  - `google-api-client-android = "2.7.2"`
  - `paging = "3.3.6"`
  - `mockk = "1.14.0"`
  - `turbine = "1.2.0"`
  - `coroutines = "1.10.2"`

- [ ] **Add all library entries to `[libraries]` in `libs.versions.toml`**
  - `hilt-android`, `hilt-compiler`
  - `room-runtime`, `room-ktx`, `room-compiler`, `room-testing`
  - `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`
  - `navigation-compose`
  - `kotlinx-serialization-json`
  - `androidx-datastore-preferences`
  - `work-runtime-ktx`
  - `play-services-location`, `play-services-auth`
  - `google-api-services-sheets`, `google-api-client-android`, `google-http-client-android`
  - `paging-runtime`, `paging-compose`
  - `mockk`, `turbine`, `kotlinx-coroutines-test`

- [ ] **Apply plugins in `app/build.gradle.kts`**
  - Add `alias(libs.plugins.kotlin.android)`
  - Add `alias(libs.plugins.kotlin.kapt)`
  - Add `alias(libs.plugins.hilt)`
  - Add `alias(libs.plugins.kotlin.serialization)`

- [ ] **Add `implementation` dependencies in `app/build.gradle.kts`**
  - Hilt, Room, ViewModel, Navigation, Serialization, DataStore, WorkManager, Paging
  - `kapt` entries for `hilt-compiler` and `room-compiler`
  - Test dependencies: MockK, Turbine, coroutines-test, room-testing

- [ ] **Enable `buildFeatures { buildConfig = true }` in `app/build.gradle.kts`**

- [ ] **Add `google-services.json`** to `app/` (download from Google Cloud Console after creating OAuth client)

- [ ] **Configure `kotlin { jvmToolchain(11) }` block** in `app/build.gradle.kts`

---

### 1.2 — Package Skeleton

- [ ] **Create all package directories under `com/example/todoit/`**
  ```
  di/
  presentation/
    home/
    groups/
    todos/
    tasks/
    settings/
    common/
  domain/
    model/
    usecase/
      group/
      todo/
      task/
    repository/
    engine/
  data/
    local/
      db/
      entity/
      dao/
    remote/
      sheets/
      auth/
    sync/
    location/
    repository/
  notification/
  ```

---

### 1.3 — Domain Models (Pure Kotlin — no Android imports)

- [ ] **Create `domain/model/Group.kt`**
  ```kotlin
  data class Group(
      val id: String,
      val parentId: String?,
      val name: String,
      val color: String?,
      val scheduleId: String?,
      val createdAt: Long,
      val updatedAt: Long,
      val deletedAt: Long?,
  )
  ```

- [ ] **Create `domain/model/TodoItem.kt`**
  ```kotlin
  data class TodoItem(
      val id: String,
      val groupId: String,
      val title: String,
      val description: String?,
      val createdAt: Long,
      val updatedAt: Long,
      val deletedAt: Long?,
  )
  ```

- [ ] **Create `domain/model/Task.kt`**
  - Fields: `id, todoId, title, status (TaskStatus), priority (Int 1–4),`
    `dueDate, startTime?, reminderAt?, locationId?, scheduleId?,`
    `inheritSchedule, recurrenceId?, recurrenceInstanceDate?, parentTaskId?,`
    `scoreCache, createdAt, updatedAt, deletedAt`

- [ ] **Create `domain/model/TaskStatus.kt`** — enum: `PENDING`, `IN_PROGRESS`, `DONE`

- [ ] **Create `domain/model/Schedule.kt`**
  - Fields: `id, name, activeDays (List<Int>), startTimeOfDay (Int?), endTimeOfDay (Int?), updatedAt, deletedAt`

- [ ] **Create `domain/model/Recurrence.kt`**
  - Fields: `id, type (RecurrenceType), interval, daysOfWeek (List<Int>?), endDate?, maxOccurrences?, updatedAt`

- [ ] **Create `domain/model/RecurrenceType.kt`** — enum: `DAILY`, `WEEKLY`, `CUSTOM`

- [ ] **Create `domain/model/Location.kt`**
  - Fields: `id, label, latitude, longitude, radiusMeters, updatedAt, deletedAt`

- [ ] **Create `domain/model/GroupNode.kt`** — tree wrapper:
  ```kotlin
  data class GroupNode(val group: Group, val children: List<GroupNode>)
  ```

---

### 1.4 — Repository Interfaces (Domain Layer)

- [ ] **Create `domain/repository/GroupRepository.kt`**
  - `suspend fun getAllGroups(): List<Group>`
  - `suspend fun getGroupById(id: String): Group?`
  - `suspend fun upsertGroup(group: Group)`
  - `suspend fun softDeleteGroup(id: String)`
  - `fun observeGroups(): Flow<List<Group>>`

- [ ] **Create `domain/repository/TodoRepository.kt`**
  - `suspend fun getTodosByGroup(groupId: String): List<TodoItem>`
  - `suspend fun upsertTodo(todo: TodoItem)`
  - `suspend fun softDeleteTodo(id: String)`
  - `fun observeTodosByGroup(groupId: String): Flow<List<TodoItem>>`

- [ ] **Create `domain/repository/TaskRepository.kt`**
  - `suspend fun getTasksByTodo(todoId: String): List<Task>`
  - `suspend fun upsertTask(task: Task)`
  - `suspend fun softDeleteTask(id: String)`
  - `fun observeTasksByTodo(todoId: String): Flow<List<Task>>`
  - `fun getTopScoredTasksPaged(): PagingSource<Int, Task>`
  - `suspend fun getDirtyTasks(): List<Task>` (for sync)

- [ ] **Create `domain/repository/ScheduleRepository.kt`** — basic CRUD + observe

- [ ] **Create `domain/repository/LocationRepository.kt`** — basic CRUD + observe

---

### 1.5 — Room Database

- [ ] **Create `data/local/entity/GroupEntity.kt`** — `@Entity(tableName = "groups", indices = [...])`
  - Map all Group fields + `syncedAt: Long?`
  - Index on: `parentId`, `updatedAt`, `deletedAt`

- [ ] **Create `data/local/entity/TodoEntity.kt`** — `@Entity(tableName = "todo_items")`
  - Map all TodoItem fields + `syncedAt`
  - Index on: `groupId`, `updatedAt`, `deletedAt`

- [ ] **Create `data/local/entity/TaskEntity.kt`** — `@Entity(tableName = "tasks")`
  - Map all Task fields + `syncedAt`
  - Index on: `todoId`, `status`, `dueDate`, `updatedAt`, `deletedAt`, `scoreCache`

- [ ] **Create `data/local/entity/ScheduleEntity.kt`** — `@Entity(tableName = "schedules")`
  - Store `activeDays` as JSON String via `@TypeConverter`

- [ ] **Create `data/local/entity/RecurrenceEntity.kt`** — `@Entity(tableName = "recurrences")`
  - Store `daysOfWeek` as JSON String via `@TypeConverter`

- [ ] **Create `data/local/entity/LocationEntity.kt`** — `@Entity(tableName = "locations")`

- [ ] **Create `data/local/db/Converters.kt`** — `@TypeConverter` for:
  - `List<Int>` ↔ JSON String (using `kotlinx.serialization`)
  - `Long?` null safety (Room handles this natively, but confirm)

- [ ] **Create `data/local/dao/GroupDao.kt`**
  - `@Query("SELECT * FROM groups WHERE deleted_at IS NULL")` → `Flow<List<GroupEntity>>`
  - `@Upsert suspend fun upsert(entity: GroupEntity)`
  - `@Query("UPDATE groups SET deleted_at = :ts, updated_at = :ts WHERE id = :id") suspend fun softDelete(...)`
  - `@Query("SELECT * FROM groups WHERE updated_at > :since AND (synced_at IS NULL OR synced_at < updated_at)")` (dirty rows)

- [ ] **Create `data/local/dao/TodoDao.kt`** — same pattern as GroupDao, filtered by `groupId`

- [ ] **Create `data/local/dao/TaskDao.kt`**
  - All CRUD + observe patterns
  - `@Query("SELECT * FROM tasks WHERE deleted_at IS NULL ORDER BY score_cache DESC") fun getTasksPaged(): PagingSource<Int, TaskEntity>`
  - Dirty row query for sync
  - `@Query("SELECT * FROM tasks WHERE reminder_at > :now AND deleted_at IS NULL AND status != 'DONE'")` (for BootReceiver)

- [ ] **Create `data/local/dao/ScheduleDao.kt`** — CRUD + observe

- [ ] **Create `data/local/dao/LocationDao.kt`** — CRUD + observe

- [ ] **Create `data/local/dao/RecurrenceDao.kt`** — CRUD

- [ ] **Create `data/local/db/TodoItDatabase.kt`**
  - `@Database(entities = [...], version = 1)`
  - `@TypeConverters(Converters::class)`
  - Register all DAOs
  - `companion object { fun create(context: Context): TodoItDatabase }`

---

### 1.6 — Entity ↔ Domain Mappers

- [ ] **Create `data/local/entity/GroupEntity.kt` extension functions**
  - `GroupEntity.toDomain(): Group`
  - `Group.toEntity(): GroupEntity`

- [ ] **Repeat for all entities:** TodoEntity, TaskEntity, ScheduleEntity, RecurrenceEntity, LocationEntity

---

### 1.7 — Repository Implementations

- [ ] **Create `data/repository/GroupRepositoryImpl.kt`**
  - Implements `GroupRepository`
  - All writes: set `updatedAt = System.currentTimeMillis()`, clear `syncedAt = null`
  - Inject `GroupDao`

- [ ] **Create `data/repository/TodoRepositoryImpl.kt`** — same pattern

- [ ] **Create `data/repository/TaskRepositoryImpl.kt`** — same pattern

- [ ] **Create `data/repository/ScheduleRepositoryImpl.kt`** — same pattern

- [ ] **Create `data/repository/LocationRepositoryImpl.kt`** — same pattern

---

### 1.8 — Dependency Injection (Hilt)

- [ ] **Annotate `TodoItApplication` with `@HiltAndroidApp`**
  - Create `TodoItApplication.kt` if not exists
  - Register in `AndroidManifest.xml` via `android:name=".TodoItApplication"`

- [ ] **Annotate `MainActivity` with `@AndroidEntryPoint`**

- [ ] **Create `di/DatabaseModule.kt`**
  - `@Provides @Singleton fun provideDatabase(app: Application): TodoItDatabase`
  - `@Provides fun provideGroupDao(db: TodoItDatabase): GroupDao`
  - Repeat for all DAOs

- [ ] **Create `di/RepositoryModule.kt`**
  - Bind `GroupRepositoryImpl` → `GroupRepository` (and all others)

---

### 1.9 — Use Cases

- [ ] **`domain/usecase/group/GetGroupTreeUseCase.kt`**
  - Fetch all groups → build `List<GroupNode>` tree recursively
  - Handle orphaned nodes (parent deleted) gracefully

- [ ] **`domain/usecase/group/UpsertGroupUseCase.kt`**
  - Validate no circular reference before saving

- [ ] **`domain/usecase/group/DeleteGroupUseCase.kt`**
  - Recursively soft-delete all child groups and their todos/tasks

- [ ] **`domain/usecase/todo/GetTodosForGroupUseCase.kt`**

- [ ] **`domain/usecase/todo/UpsertTodoUseCase.kt`**

- [ ] **`domain/usecase/todo/DeleteTodoUseCase.kt`** — soft-delete todo + all its tasks

- [ ] **`domain/usecase/task/GetTasksForTodoUseCase.kt`**

- [ ] **`domain/usecase/task/UpsertTaskUseCase.kt`**

- [ ] **`domain/usecase/task/DeleteTaskUseCase.kt`**

---

### 1.10 — Navigation

- [ ] **Create `presentation/common/NavGraph.kt`**
  - Define sealed class / object `Screen` with routes:
    - `Home` (What To Do Now)
    - `Groups` (group tree browser)
    - `GroupDetail(groupId)` (todos in a group)
    - `TodoDetail(todoId)` (tasks in a todo)
    - `TaskEdit(taskId?)` (create/edit task)
    - `Settings`
  - Set up `NavHost` with `rememberNavController()`

- [ ] **Update `AppDestinations` enum in `MainActivity.kt`**
  - Change `FAVORITES` → `GROUPS`
  - Keep `HOME` and `PROFILE` (rename to `SETTINGS`)
  - Wire `NavigationSuiteScaffold` click to `NavHost` navigation

- [ ] **Integrate `NavHost` into the `NavigationSuiteScaffold` content lambda**

---

### 1.11 — Group Screens

- [ ] **Create `presentation/groups/GroupsViewModel.kt`**
  - Inject `GetGroupTreeUseCase`, `UpsertGroupUseCase`, `DeleteGroupUseCase`
  - `uiState: StateFlow<GroupsUiState>` with sealed class `Loading | Success(tree) | Error`

- [ ] **Create `presentation/groups/GroupsScreen.kt`**
  - Lazy column showing group tree (expandable nodes)
  - FAB to create new group
  - Long press → delete/edit options
  - Navigate to `GroupDetail` on tap

- [ ] **Create `presentation/groups/GroupDetailScreen.kt`**
  - List todos in the group
  - FAB to create new todo
  - Navigate to `TodoDetail` on tap

- [ ] **Create `presentation/groups/GroupEditDialog.kt`**
  - Name field, color picker, parent group selector, schedule selector

---

### 1.12 — Todo Screens

- [ ] **Create `presentation/todos/TodoDetailViewModel.kt`**
  - Inject `GetTasksForTodoUseCase`, `UpsertTaskUseCase`, `DeleteTaskUseCase`

- [ ] **Create `presentation/todos/TodoDetailScreen.kt`**
  - List tasks with status chips and priority indicators
  - FAB to create new task
  - Swipe to delete task

---

### 1.13 — Task Screens

- [ ] **Create `presentation/tasks/TaskEditViewModel.kt`**
  - Inject `UpsertTaskUseCase`
  - Handle create vs. edit (nullable `taskId`)

- [ ] **Create `presentation/tasks/TaskEditScreen.kt`**
  - Fields: title, status dropdown, priority selector (1–4), due date picker, start time picker, reminder time picker
  - Recurrence selector (None / Daily / Weekly / Custom)
  - Location picker (lat/lng + radius — manual entry for MVP; map in later phase)
  - Schedule override toggle

---

### 1.14 — Common UI Components

- [ ] **Create `presentation/common/PriorityChip.kt`** — colored chip for priority 1–4

- [ ] **Create `presentation/common/StatusChip.kt`** — chip for PENDING / IN_PROGRESS / DONE

- [ ] **Create `presentation/common/DateTimePicker.kt`** — wraps Material3 `DatePickerDialog` + `TimePickerDialog`

- [ ] **Create `presentation/common/ConfirmDeleteDialog.kt`**

- [ ] **Create `presentation/common/EmptyStateView.kt`**

---

### ✅ Phase 1 Done When:
- User can create/edit/delete groups (nested), todos, and tasks fully offline
- App survives process death (data persists in Room)
- Navigation between all screens works
- No crashes on fresh install

---

## Phase 2 — Offline + Google Sheets Sync
**Goal:** Bidirectional sync between Room and Google Sheets with conflict resolution.

---

### 2.1 — Google Cloud Setup (Manual — not code)

- [ ] **Create Google Cloud project** at console.cloud.google.com
- [ ] **Enable Google Sheets API** and **Google Drive API**
- [ ] **Create OAuth 2.0 Client ID** (Android type; use app's SHA-1 fingerprint)
- [ ] **Download `google-services.json`** and place in `app/`
- [ ] **Apply `com.google.gms.google-services` plugin** in `build.gradle.kts`

---

### 2.2 — Google Sign-In

- [ ] **Create `data/remote/auth/GoogleAuthManager.kt`**
  - `fun signIn(activity: Activity): Intent` (launch sign-in flow)
  - `fun getSignedInAccount(context: Context): GoogleSignInAccount?`
  - `fun signOut(context: Context)`
  - Scope: `SheetsScopes.SPREADSHEETS` + `DriveScopes.FILE`

- [ ] **Create `presentation/settings/SettingsViewModel.kt`**
  - Expose signed-in account state
  - `fun signIn()`, `fun signOut()`

- [ ] **Create `presentation/settings/SettingsScreen.kt`**
  - Show signed-in account email
  - "Sign In with Google" / "Sign Out" button
  - Manual "Sync Now" button
  - Last sync timestamp display

---

### 2.3 — Spreadsheet Bootstrap

- [ ] **Create `data/remote/sheets/SpreadsheetBootstrapper.kt`**
  - On first sign-in: check DataStore for saved `spreadsheetId`
  - If none: create a new spreadsheet via Drive API with name "TodoIt Data"
  - Create 7 sheets (tabs): `groups`, `todo_items`, `tasks`, `schedules`, `recurrences`, `locations`, `sync_meta`
  - Write header row to each sheet
  - Save `spreadsheetId` to DataStore

- [ ] **Create `di/DataStoreModule.kt`**
  - `@Provides @Singleton fun provideDataStore(app: Application): DataStore<Preferences>`

- [ ] **Create `data/local/AppPreferences.kt`**
  - Keys: `SPREADSHEET_ID`, `LAST_SYNC_AT`
  - Suspend funs: `getSpreadsheetId()`, `setSpreadsheetId(id)`, `getLastSyncAt()`, `setLastSyncAt(ts)`

---

### 2.4 — Sheets API Service

- [ ] **Create `data/remote/sheets/SheetsApiService.kt`**
  - `suspend fun batchReadAllSheets(spreadsheetId: String): Map<String, List<List<Any>>>`
    - One `spreadsheets.values.batchGet` call for all 7 sheets
  - `suspend fun batchWriteRows(spreadsheetId: String, updates: List<ValueRange>)`
    - One `spreadsheets.values.batchUpdate` call
  - `suspend fun appendRows(spreadsheetId: String, sheetName: String, rows: List<List<Any>>)`

- [ ] **Create `data/remote/sheets/RowMapper.kt`**
  - `GroupEntity.toSheetRow(): List<Any>`
  - `List<Any>.toGroupEntity(): GroupEntity?` (with null/index safety)
  - Repeat for all entities

- [ ] **Create `data/remote/sheets/RetryPolicy.kt`**
  - `suspend fun <T> executeWithRetry(maxRetries: Int = 5, block: suspend () -> T): T`
  - Exponential backoff with jitter for HTTP 429, 500, 503
  - On 401: call `GoogleAuthManager.refreshToken()` then retry once

---

### 2.5 — Sync Engine

- [ ] **Create `data/sync/SyncManager.kt`**
  - `suspend fun pull(spreadsheetId: String)` — merges remote rows into Room
    - For each entity: compare `remote.updated_at` vs `local.updated_at` → higher wins
    - If remote has `deleted_at` set → soft-delete locally
    - After merge: `UPDATE sync_meta SET last_sync_at = now` per entity
  - `suspend fun push(spreadsheetId: String)` — writes dirty Room rows to Sheets
    - Query all entities where `updated_at > synced_at` (or `synced_at IS NULL`)
    - `batchWriteRows()` with all dirty data
    - On success: `UPDATE synced_at = now` for all pushed rows
  - `suspend fun fullSync()` — pull then push

- [ ] **Create `data/sync/SyncWorker.kt`** (`CoroutineWorker`)
  - Inject `SyncManager`, `AppPreferences`
  - `doWork()`: get `spreadsheetId` → `SyncManager.fullSync()`
  - Return `Result.retry()` on network/quota errors
  - Return `Result.failure()` on auth errors (propagate to UI)

- [ ] **Create `di/WorkManagerModule.kt`**
  - `@HiltWorker` on `SyncWorker` (use `HiltWorkerFactory`)
  - Override `WorkerFactory` in `TodoItApplication`

- [ ] **Create `data/sync/SyncScheduler.kt`**
  - `fun schedulePeriodicSync()` — `PeriodicWorkRequest` every 15 min, `CONNECTED + NOT_LOW_BATTERY`
  - `fun triggerImmediateSync()` — expedited `OneTimeWorkRequest`, debounced 30s

- [ ] **Trigger `SyncScheduler.triggerImmediateSync()`** from every repository write operation

- [ ] **Show sync status** in `SettingsScreen` (observe WorkManager `WorkInfo`)

---

### ✅ Phase 2 Done When:
- User can sign in with Google
- Data syncs to their Google Sheets spreadsheet automatically
- Conflicts resolved by `updated_at` timestamp
- App works fully offline; sync runs when connection restored

---

## Phase 3 — Smart Filtering ("What To Do Now")
**Goal:** The scoring engine, schedule system, and recurrence expansion all work.

---

### 3.1 — Schedule Engine

- [ ] **Create `domain/engine/ScheduleEvaluator.kt`**
  ```kotlin
  fun isScheduleActive(schedule: Schedule?, now: LocalDateTime): Boolean
  fun resolveSchedule(task: Task, group: Group?): Schedule?
  ```
  - Implements the pseudocode from the blueprint exactly
  - Handles `inherit_schedule = true` fallback chain

- [ ] **Create schedule management UI**
  - `presentation/settings/ScheduleListScreen.kt` — list all schedules
  - `presentation/settings/ScheduleEditScreen.kt` — day-of-week toggles + time range pickers

- [ ] **Wire schedule selector** into `GroupEditDialog` and `TaskEditScreen`

---

### 3.2 — Recurrence Engine

- [ ] **Create `domain/engine/RecurrenceExpander.kt`**
  ```kotlin
  fun expand(rootTask: Task, recurrence: Recurrence, from: Long, upTo: Long): List<Task>
  fun nextOccurrence(currentMs: Long, recurrence: Recurrence): Long
  fun occurrenceExists(parentTaskId: String, instanceDate: Long, dao: TaskDao): Boolean
  ```
  - Generates instances up to 90 days ahead
  - Respects `maxOccurrences` and `endDate` caps
  - Each instance gets a new UUID, `parentTaskId` set, `status = PENDING`

- [ ] **Create `domain/usecase/task/ExpandRecurrenceUseCase.kt`**
  - Called on task save (if recurrence set) and during sync pull

- [ ] **Trigger recurrence expansion** inside `UpsertTaskUseCase` when `recurrenceId` is set

- [ ] **Handle parent task soft-delete** — cascade soft-delete all instances with same `parentTaskId`

---

### 3.3 — Scoring Engine

- [ ] **Create `domain/engine/TaskScorer.kt`**
  ```kotlin
  fun score(task: Task, location: Location?, userLatLng: LatLng?, now: Long, schedule: Schedule?): Float
  fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double
  ```
  - Implements the exact scoring formula from the blueprint (max 110 pts)
  - Returns `-1f` for excluded tasks (DONE or schedule-inactive)

- [ ] **Create `domain/usecase/task/RefreshScoreCacheUseCase.kt`**
  - Queries all non-DONE tasks from Room
  - Scores each with `TaskScorer`
  - Batch updates `score_cache` column in Room
  - Called on: screen resume + passive location update

---

### 3.4 — "What To Do Now" Screen

- [ ] **Create `presentation/home/HomeViewModel.kt`**
  - Inject `GetTopScoredTasksUseCase`, `RefreshScoreCacheUseCase`
  - `uiState: StateFlow<HomeUiState>` with `Loading | Success(PagingData<Task>) | Error`
  - Call `refreshScoreCache()` on `init` and when location updates

- [ ] **Create `presentation/home/HomeScreen.kt`**
  - `LazyColumn` with `collectAsLazyPagingItems()` (Paging 3)
  - Each item shows: title, priority chip, status chip, due date, score (debug mode), location label
  - Pull-to-refresh triggers `RefreshScoreCacheUseCase`
  - Tap task → navigate to `TaskEditScreen` for quick status update

- [ ] **Create `domain/usecase/task/GetTopScoredTasksUseCase.kt`**
  - Returns `PagingSource` from `TaskDao.getTasksPaged()` (sorted by `score_cache DESC`)

---

### ✅ Phase 3 Done When:
- "What To Do Now" shows deterministic scored list
- Schedule filtering works (weekday-only tasks hidden on weekends)
- Recurring tasks auto-expand 90 days ahead
- Score updates when user returns to app

---

## Phase 4 — Location Features
**Goal:** Geofence proximity notifications and location-aware scoring.

---

### 4.1 — Permissions

- [ ] **Add to `AndroidManifest.xml`:**
  ```xml
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
  <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION"/>
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
  <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
  ```

- [ ] **Create `presentation/common/PermissionHandler.kt`**
  - Composable wrapper using `rememberPermissionState` (Accompanist or Compose built-in)
  - Rationale dialog before requesting background location

---

### 4.2 — Location Repository

- [ ] **Create `data/location/FusedLocationProvider.kt`**
  - `fun startPassiveUpdates(onLocation: (LatLng) -> Unit)` — `PRIORITY_PASSIVE`, interval 5 min
  - `fun stopUpdates()`
  - **Never request `PRIORITY_HIGH_ACCURACY` or active GPS**

- [ ] **Expose current location** via `SharedFlow<LatLng>` injected into `HomeViewModel`

---

### 4.3 — Geofencing

- [ ] **Create `data/location/GeofenceManager.kt`**
  - `suspend fun registerGeofencesForActiveTasks(tasks: List<Task>)` — registers all tasks with non-null location
  - `suspend fun removeGeofence(taskId: String)`
  - `suspend fun clearAllGeofences()`
  - Max 100 geofences (system limit) — prioritize by `score_cache` if over limit

- [ ] **Create `notification/GeofenceBroadcastReceiver.kt`**
  - Handles `GEOFENCE_TRANSITION_ENTER`
  - Loads task from Room by geofence request ID
  - Shows proximity notification if task is not DONE
  - Register in `AndroidManifest.xml`

- [ ] **Trigger `GeofenceManager.registerGeofencesForActiveTasks()`** on:
  - App startup
  - After sync completes
  - After task save/delete

---

### 4.4 — Exact Alarm Reminders

- [ ] **Create `notification/ReminderScheduler.kt`**
  - `fun scheduleReminder(task: Task)` — `AlarmManager.setExactAndAllowWhileIdle()`
  - `fun cancelReminder(taskId: String)`
  - `fun rescheduleAll(tasks: List<Task>)` — called by `BootReceiver`

- [ ] **Create `notification/ReminderBroadcastReceiver.kt`**
  - Receives alarm intent with `TASK_ID` extra
  - Loads task from Room
  - Shows time-based notification
  - Register in `AndroidManifest.xml`

- [ ] **Create `notification/BootReceiver.kt`**
  - `onReceive(BOOT_COMPLETED)`:
    1. Query Room: all tasks with `reminder_at > now` and status != DONE
    2. Call `ReminderScheduler.rescheduleAll(tasks)`
    3. Call `GeofenceManager.registerGeofencesForActiveTasks(tasks)`
  - Register in `AndroidManifest.xml`

- [ ] **Create `notification/NotificationHelper.kt`**
  - Create notification channels (`REMINDERS`, `PROXIMITY`) on app start
  - `fun showReminderNotification(task: Task)`
  - `fun showProximityNotification(task: Task)`

- [ ] **Trigger `ReminderScheduler.scheduleReminder(task)`** from `UpsertTaskUseCase` when `reminderAt` is set

---

### 4.5 — Location in Task UI

- [ ] **Add location section to `TaskEditScreen.kt`**
  - Toggle: "Enable location trigger"
  - Latitude / Longitude text fields (manual entry)
  - Radius slider (100m – 5000m)
  - Location label text field
  - "Enable proximity notification" toggle

- [ ] **Update `TaskEditViewModel`** to save/update `LocationEntity` and link to task

---

### ✅ Phase 4 Done When:
- User receives exact-time reminders even when app is killed
- User receives proximity notification when near task location
- Alarms and geofences survive device reboot
- Permission denials are handled gracefully (no crashes)

---

## Phase 5 — Optimization & Polish
**Goal:** Production-ready performance, security, and UX.

---

### 5.1 — Performance

- [ ] **Verify all Room indices** are applied (status, dueDate, scoreCache, updatedAt, deletedAt)

- [ ] **Profile `score_cache` refresh** — ensure it runs on a background dispatcher (`Dispatchers.Default`)

- [ ] **Verify Paging 3 is used** for all large list screens (Home, Group task lists)

- [ ] **Add `LIMIT` to sync dirty-row queries** — cap at 500 rows per push to avoid oversized API calls

- [ ] **Add Sheets response caching** — store last raw pull response in a file cache; skip pull if last sync < 5 min ago

---

### 5.2 — Security

- [ ] **Set `isMinifyEnabled = true`** in release build type (`app/build.gradle.kts`)

- [ ] **Add ProGuard rules** to `proguard-rules.pro`:
  - Keep Google API client classes
  - Keep Room entities
  - Keep Hilt generated classes

- [ ] **Add `google-services.json` to `.gitignore`** if not already excluded

- [ ] **Remove all `Log.*` calls** from release build (or use a debug-only logger wrapper)

- [ ] **Verify OAuth scope** is minimal — only `SPREADSHEETS` + `FILE`; no `DRIVE` full access

---

### 5.3 — Edge Cases & Robustness

- [ ] **Handle geofence limit (100)** — implement priority-based geofence eviction:
  - Drop geofences for DONE tasks first
  - Then drop geofences for tasks with lowest `score_cache`

- [ ] **Handle `SCHEDULE_EXACT_ALARM` permission denied (API 31+)**:
  - Fall back to `setAndAllowWhileIdle()`
  - Show in-app banner explaining reduced accuracy

- [ ] **Handle background location permission denied**:
  - Show explanation screen
  - Disable geofencing gracefully; keep time-based reminders

- [ ] **Handle sync failure mid-push** (partial write to Sheets):
  - Do not update `synced_at` on failure
  - Rows will be retried on next sync cycle

- [ ] **Handle orphaned recurrence instances** when parent task deleted:
  - `DeleteTaskUseCase` must soft-delete all instances with `parentTaskId == task.id`

- [ ] **Validate no circular group references** in `UpsertGroupUseCase`:
  - Walk up the `parentId` chain; reject if `id` appears in ancestors

---

### 5.4 — UX Polish

- [ ] **Add loading skeletons** (shimmer effect) for list screens during first load

- [ ] **Add empty state illustrations** for groups/todos/tasks with no items

- [ ] **Add undo snackbar** for delete operations (Room soft-delete makes this easy)

- [ ] **Add "sync failed" banner** in `SettingsScreen` when last WorkManager result was failure

- [ ] **Animate task score changes** in "What To Do Now" list (optional, `animateItemPlacement`)

- [ ] **Add dark mode** verification — confirm all custom colors use MaterialTheme tokens

- [ ] **Add widget** (optional stretch goal) — small 4×1 widget showing top 3 scored tasks

---

### 5.5 — Testing

- [ ] **Unit test `TaskScorer`** — all scoring branches (overdue, priority levels, location proximity, schedule exclusion)

- [ ] **Unit test `ScheduleEvaluator`** — weekday/weekend, time window, null schedule, inherit chain

- [ ] **Unit test `RecurrenceExpander`** — DAILY, WEEKLY, CUSTOM; maxOccurrences; endDate; no duplicates

- [ ] **Unit test `buildGroupTree()`** — nested groups, orphaned nodes, root-only

- [ ] **Integration test `GroupDao`** — upsert, soft-delete, observe, dirty-row query

- [ ] **Integration test `TaskDao`** — paged query, score sort, status filter

- [ ] **Integration test `SyncManager.pull()`** — mock Sheets response; verify merge logic; conflict wins

- [ ] **Integration test `SyncManager.push()`** — verify only dirty rows sent; `synced_at` updated

- [ ] **Edge case tests:**
  - Offline 7 days → sync → no data loss
  - Recurrence parent deleted → all instances soft-deleted
  - Group with 3-level nesting → tree built correctly
  - Task score tie → sorted by `dueDate` ascending

---

### ✅ Phase 5 Done When:
- Release build is minified + ProGuard clean
- All unit + integration tests pass
- No crashes in edge cases
- App is performant with 1000+ tasks

---

## Quick Reference — File Count Per Phase

| Phase | New Files (approx) |
|---|---|
| 1 — MVP | ~55 files |
| 2 — Sync | ~15 files |
| 3 — Smart Filtering | ~12 files |
| 4 — Location | ~10 files |
| 5 — Polish | ~5 files + tests |
| **Total** | **~97 files** |

---

## Dependency on External Setup (Manual Steps)

| Step | When |
|---|---|
| Create Google Cloud project + enable APIs | Before Phase 2 |
| Create OAuth client ID (Android) | Before Phase 2 |
| Add SHA-1 fingerprint to OAuth client | Before Phase 2 |
| Download + place `google-services.json` | Before Phase 2 |
| Apply `com.google.gms.google-services` plugin | Before Phase 2 |

