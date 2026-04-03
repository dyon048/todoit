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
    /**
     * Full bidirectional sync — single batchRead shared between pull and push.
     * Push uses in-place row updates for existing entities (no duplicate rows).
     */
    suspend fun fullSync() {
        val spreadsheetId = bootstrapper.ensureSpreadsheet()
        val allSheets = retryPolicy.executeWithRetry { sheetsApi.batchReadAllSheets(spreadsheetId) }
        pullFromData(allSheets)
        push(spreadsheetId, allSheets)
        prefs.setLastSyncAt(System.currentTimeMillis())
    }

    // ─── PULL ──────────────────────────────────────────────────────────────

    /** Public overload — reads sheets itself (used when called standalone). */
    suspend fun pull(spreadsheetId: String) = retryPolicy.executeWithRetry {
        val allSheets = sheetsApi.batchReadAllSheets(spreadsheetId)
        pullFromData(allSheets)
    }

    private suspend fun pullFromData(allSheets: Map<String, List<List<Any>>>) {
        allSheets["groups"]?.drop(1)?.forEach { row ->
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
     * Pushes every locally-dirty row to the corresponding sheet tab.
     *
     * Strategy (in-place upsert — no duplicate rows):
     * - If the entity's ID already exists in the sheet → overwrite that specific row
     *   using [SheetsApiService.batchWriteRows] with the exact cell address.
     * - If the ID is not yet in the sheet → append as a new row.
     *
     * [existingSheetData] is the data already read by [fullSync] so we don't need
     * an extra round-trip.  When called standalone, pass an empty map and the method
     * will read the sheet itself.
     */
    suspend fun push(
        spreadsheetId: String,
        existingSheetData: Map<String, List<List<Any>>> = emptyMap(),
    ) = retryPolicy.executeWithRetry {
        val sheetData = existingSheetData.ifEmpty {
            sheetsApi.batchReadAllSheets(spreadsheetId)
        }

        val now = System.currentTimeMillis()

        val dirtyGroups = groupDao.getDirtyRows()
        if (dirtyGroups.isNotEmpty()) {
            val rowMap = buildRowNumberMap(sheetData["groups"])
            upsertRows(spreadsheetId, "groups", dirtyGroups.map { it.id to it.toSheetRow() }, rowMap)
            groupDao.markSynced(dirtyGroups.map { it.id }, now)
        }

        val dirtyTodos = todoDao.getDirtyRows()
        if (dirtyTodos.isNotEmpty()) {
            val rowMap = buildRowNumberMap(sheetData["todo_items"])
            upsertRows(spreadsheetId, "todo_items", dirtyTodos.map { it.id to it.toSheetRow() }, rowMap)
            todoDao.markSynced(dirtyTodos.map { it.id }, now)
        }

        val dirtyTasks = taskDao.getDirtyRows()
        if (dirtyTasks.isNotEmpty()) {
            val rowMap = buildRowNumberMap(sheetData["tasks"])
            upsertRows(spreadsheetId, "tasks", dirtyTasks.map { it.id to it.toSheetRow() }, rowMap)
            taskDao.markSynced(dirtyTasks.map { it.id }, now)
        }

        val dirtySchedules = scheduleDao.getDirtyRows()
        if (dirtySchedules.isNotEmpty()) {
            val rowMap = buildRowNumberMap(sheetData["schedules"])
            upsertRows(spreadsheetId, "schedules", dirtySchedules.map { it.id to it.toSheetRow() }, rowMap)
            scheduleDao.markSynced(dirtySchedules.map { it.id }, now)
        }

        val dirtyLocations = locationDao.getDirtyRows()
        if (dirtyLocations.isNotEmpty()) {
            val rowMap = buildRowNumberMap(sheetData["locations"])
            upsertRows(spreadsheetId, "locations", dirtyLocations.map { it.id to it.toSheetRow() }, rowMap)
            locationDao.markSynced(dirtyLocations.map { it.id }, now)
        }

        val dirtyRecurrences = recurrenceDao.getDirtyRows()
        if (dirtyRecurrences.isNotEmpty()) {
            val rowMap = buildRowNumberMap(sheetData["recurrences"])
            upsertRows(spreadsheetId, "recurrences", dirtyRecurrences.map { it.id to it.toSheetRow() }, rowMap)
            recurrenceDao.markSynced(dirtyRecurrences.map { it.id }, now)
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    /**
     * Builds a map of entity ID → 1-based sheet row number from the raw sheet data.
     * Row 1 is the header; data rows start at row 2 (index 1 in the values list).
     */
    private fun buildRowNumberMap(sheetData: List<List<Any>>?): Map<String, Int> {
        if (sheetData.isNullOrEmpty()) return emptyMap()
        return sheetData.drop(1).mapIndexedNotNull { idx, row ->
            val id = row.getOrNull(0)?.toString()?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
            id to (idx + 2)   // +1 for header row, +1 for 1-based row index
        }.toMap()
    }

    /**
     * Writes [rows] to [sheetName]:
     * - Rows whose ID already appears in [existingRowMap] → overwrite the specific row in-place.
     * - Rows with an unknown ID → append as new rows.
     */
    private suspend fun upsertRows(
        spreadsheetId: String,
        sheetName: String,
        rows: List<Pair<String, List<Any>>>,   // id to sheetRow
        existingRowMap: Map<String, Int>,
    ) {
        val toUpdate = rows.filter { (id, _) -> id in existingRowMap }
        val toInsert = rows.filter { (id, _) -> id !in existingRowMap }

        if (toUpdate.isNotEmpty()) {
            val updates = toUpdate.map { (id, sheetRow) ->
                val rowNum = existingRowMap[id]!!
                ValueRange()
                    .setRange("$sheetName!A$rowNum")
                    .setValues(listOf(sheetRow))
            }
            sheetsApi.batchWriteRows(spreadsheetId, updates)
        }

        if (toInsert.isNotEmpty()) {
            sheetsApi.appendRows(spreadsheetId, sheetName, toInsert.map { it.second })
        }
    }
}
