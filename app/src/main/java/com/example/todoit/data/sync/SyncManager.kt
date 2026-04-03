package com.example.todoit.data.sync

import com.example.todoit.data.local.AppPreferences
import com.example.todoit.data.local.dao.GroupDao
import com.example.todoit.data.local.dao.LocationDao
import com.example.todoit.data.local.dao.RecurrenceDao
import com.example.todoit.data.local.dao.ScheduleDao
import com.example.todoit.data.local.dao.TaskDao
import com.example.todoit.data.local.dao.TodoDao
import com.example.todoit.data.remote.sheets.RetryPolicy
import com.example.todoit.data.remote.sheets.SheetsApiService
import com.example.todoit.data.remote.sheets.SpreadsheetBootstrapper
import com.example.todoit.data.remote.sheets.toGroupEntity
import com.example.todoit.data.remote.sheets.toLocationEntity
import com.example.todoit.data.remote.sheets.toRecurrenceEntity
import com.example.todoit.data.remote.sheets.toScheduleEntity
import com.example.todoit.data.remote.sheets.toSheetRow
import com.example.todoit.data.remote.sheets.toTaskEntity
import com.example.todoit.data.remote.sheets.toTodoEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val sheetsApi: SheetsApiService,
    private val bootstrapper: SpreadsheetBootstrapper,
    private val retryPolicy: RetryPolicy,
    private val prefs: AppPreferences,
    private val groupDao: GroupDao,
    private val todoDao: TodoDao,
    private val taskDao: TaskDao,
    private val scheduleDao: ScheduleDao,
    private val locationDao: LocationDao,
    private val recurrenceDao: RecurrenceDao,
) {
    /** Full bidirectional sync — pull remote wins on conflict, then push dirty local rows. */
    suspend fun fullSync() {
        val spreadsheetId = bootstrapper.ensureSpreadsheet()
        pull(spreadsheetId)
        push(spreadsheetId)
        prefs.setLastSyncAt(System.currentTimeMillis())
    }

    // ─── PULL ──────────────────────────────────────────────────────────────
    suspend fun pull(spreadsheetId: String) = retryPolicy.executeWithRetry {
        val allSheets = sheetsApi.batchReadAllSheets(spreadsheetId)

        allSheets["groups"]?.drop(1)?.forEach { row ->   // drop header row
            val remote = row.toGroupEntity() ?: return@forEach
            val local  = groupDao.getById(remote.id)
            if (local == null || remote.updatedAt > local.updatedAt) {
                groupDao.upsert(remote)
            }
        }

        allSheets["todo_items"]?.drop(1)?.forEach { row ->
            val remote = row.toTodoEntity() ?: return@forEach
            val local  = todoDao.getById(remote.id)
            if (local == null || remote.updatedAt > local.updatedAt) {
                todoDao.upsert(remote)
            }
        }

        allSheets["tasks"]?.drop(1)?.forEach { row ->
            val remote = row.toTaskEntity() ?: return@forEach
            val local  = taskDao.getById(remote.id)
            if (local == null || remote.updatedAt > local.updatedAt) {
                taskDao.upsert(remote)
            }
        }

        allSheets["schedules"]?.drop(1)?.forEach { row ->
            val remote = row.toScheduleEntity() ?: return@forEach
            val local  = scheduleDao.getById(remote.id)
            if (local == null || remote.updatedAt > local.updatedAt) {
                scheduleDao.upsert(remote)
            }
        }

        allSheets["locations"]?.drop(1)?.forEach { row ->
            val remote = row.toLocationEntity() ?: return@forEach
            val local  = locationDao.getById(remote.id)
            if (local == null || remote.updatedAt > local.updatedAt) {
                locationDao.upsert(remote)
            }
        }

        allSheets["recurrences"]?.drop(1)?.forEach { row ->
            val remote = row.toRecurrenceEntity() ?: return@forEach
            val local  = recurrenceDao.getById(remote.id)
            if (local == null || remote.updatedAt > local.updatedAt) {
                recurrenceDao.upsert(remote)
            }
        }
    }

    // ─── PUSH ──────────────────────────────────────────────────────────────
    /**
     * Appends every locally-dirty row to its corresponding sheet tab.
     * Using append (not batchUpdate at A1) keeps the header row at row 1 intact and adds
     * data rows after it.  Duplicate rows for the same entity ID are fine — the pull phase
     * resolves them by always keeping the entry with the newest updatedAt.
     */
    suspend fun push(spreadsheetId: String) = retryPolicy.executeWithRetry {
        val now = System.currentTimeMillis()

        val dirtyGroups = groupDao.getDirtyRows()
        if (dirtyGroups.isNotEmpty()) {
            sheetsApi.appendRows(spreadsheetId, "groups", dirtyGroups.map { it.toSheetRow() })
            groupDao.markSynced(dirtyGroups.map { it.id }, now)
        }

        val dirtyTodos = todoDao.getDirtyRows()
        if (dirtyTodos.isNotEmpty()) {
            sheetsApi.appendRows(spreadsheetId, "todo_items", dirtyTodos.map { it.toSheetRow() })
            todoDao.markSynced(dirtyTodos.map { it.id }, now)
        }

        val dirtyTasks = taskDao.getDirtyRows()
        if (dirtyTasks.isNotEmpty()) {
            sheetsApi.appendRows(spreadsheetId, "tasks", dirtyTasks.map { it.toSheetRow() })
            taskDao.markSynced(dirtyTasks.map { it.id }, now)
        }

        val dirtySchedules = scheduleDao.getDirtyRows()
        if (dirtySchedules.isNotEmpty()) {
            sheetsApi.appendRows(spreadsheetId, "schedules", dirtySchedules.map { it.toSheetRow() })
            scheduleDao.markSynced(dirtySchedules.map { it.id }, now)
        }

        val dirtyLocations = locationDao.getDirtyRows()
        if (dirtyLocations.isNotEmpty()) {
            sheetsApi.appendRows(spreadsheetId, "locations", dirtyLocations.map { it.toSheetRow() })
            locationDao.markSynced(dirtyLocations.map { it.id }, now)
        }

        val dirtyRecurrences = recurrenceDao.getDirtyRows()
        if (dirtyRecurrences.isNotEmpty()) {
            sheetsApi.appendRows(spreadsheetId, "recurrences", dirtyRecurrences.map { it.toSheetRow() })
            recurrenceDao.markSynced(dirtyRecurrences.map { it.id }, now)
        }
    }
}


