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
import com.google.api.services.sheets.v4.model.ValueRange
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
    suspend fun push(spreadsheetId: String) = retryPolicy.executeWithRetry {
        val updates = mutableListOf<ValueRange>()
        val now = System.currentTimeMillis()

        val dirtyGroups = groupDao.getDirtyRows()
        if (dirtyGroups.isNotEmpty()) {
            val rows = dirtyGroups.map { it.toSheetRow() }
            updates += ValueRange().setRange("groups!A1").setValues(rows)
        }

        val dirtyTodos = todoDao.getDirtyRows()
        if (dirtyTodos.isNotEmpty()) {
            updates += ValueRange().setRange("todo_items!A1").setValues(dirtyTodos.map { it.toSheetRow() })
        }

        val dirtyTasks = taskDao.getDirtyRows()
        if (dirtyTasks.isNotEmpty()) {
            updates += ValueRange().setRange("tasks!A1").setValues(dirtyTasks.map { it.toSheetRow() })
        }

        val dirtySchedules = scheduleDao.getDirtyRows()
        if (dirtySchedules.isNotEmpty()) {
            updates += ValueRange().setRange("schedules!A1").setValues(dirtySchedules.map { it.toSheetRow() })
        }

        val dirtyLocations = locationDao.getDirtyRows()
        if (dirtyLocations.isNotEmpty()) {
            updates += ValueRange().setRange("locations!A1").setValues(dirtyLocations.map { it.toSheetRow() })
        }

        sheetsApi.batchWriteRows(spreadsheetId, updates)

        // Mark pushed rows as synced
        groupDao.markSynced(dirtyGroups.map { it.id }, now)
        todoDao.markSynced(dirtyTodos.map { it.id }, now)
        taskDao.markSynced(dirtyTasks.map { it.id }, now)
        scheduleDao.markSynced(dirtySchedules.map { it.id }, now)
        locationDao.markSynced(dirtyLocations.map { it.id }, now)
    }
}


