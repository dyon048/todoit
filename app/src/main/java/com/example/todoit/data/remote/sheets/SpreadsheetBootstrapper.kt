package com.example.todoit.data.remote.sheets

import com.example.todoit.data.local.AppPreferences
import com.google.api.services.sheets.v4.model.ValueRange
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates the "TodoIt Data" spreadsheet on first sign-in and ensures all 7 sheet tabs +
 * header rows exist.  Stores the resulting spreadsheetId in AppPreferences.
 */
@Singleton
class SpreadsheetBootstrapper @Inject constructor(
    private val sheetsApi: SheetsApiService,
    private val prefs: AppPreferences,
) {
    companion object {
        private val HEADERS: Map<String, List<String>> = mapOf(
            "groups"      to listOf("id","parent_id","name","color","schedule_id","created_at","updated_at","deleted_at","synced_at"),
            "todo_items"  to listOf("id","group_id","title","description","created_at","updated_at","deleted_at","synced_at"),
            "tasks"       to listOf("id","todo_id","title","status","priority","due_date","start_time","reminder_at","location_id","schedule_id","inherit_schedule","recurrence_id","recurrence_instance_date","parent_task_id","score_cache","created_at","updated_at","deleted_at","synced_at"),
            "schedules"   to listOf("id","name","active_days","start_time_of_day","end_time_of_day","updated_at","deleted_at","synced_at"),
            "recurrences" to listOf("id","type","interval","days_of_week","end_date","max_occurrences","updated_at","synced_at"),
            "locations"   to listOf("id","label","latitude","longitude","radius_meters","updated_at","deleted_at","synced_at"),
            "sync_meta"   to listOf("sheet","last_sync_at"),
        )
    }

    /**
     * Returns the spreadsheetId (cached or freshly created).
     * Fully idempotent — safe to call on every sync.
     *
     * Order of operations:
     *  1. Create the spreadsheet if no ID is cached yet (saves ID immediately to avoid orphans).
     *  2. Ensure all 7 sheet tabs exist (renames default "Sheet1", adds any missing ones).
     *  3. Write header rows to row 1 of each tab (safe to overwrite — headers are idempotent).
     */
    suspend fun ensureSpreadsheet(): String {
        var spreadsheetId = prefs.getSpreadsheetId()

        if (spreadsheetId.isNullOrBlank()) {
            // Create spreadsheet and persist ID *before* any further API calls so that a
            // failure mid-setup doesn't leave us creating a new orphaned spreadsheet next time.
            spreadsheetId = sheetsApi.createSpreadsheet("TodoIt Data")
            prefs.setSpreadsheetId(spreadsheetId)
            prefs.setLastSyncAt(0L)
        }

        // Ensure all required sheet tabs exist (idempotent — skips tabs that already exist).
        // Google Sheets does NOT auto-create tabs when you write to them.
        sheetsApi.ensureSheetTabs(spreadsheetId, HEADERS.keys.toList())

        // Write / refresh header rows (idempotent — overwriting row 1 with the same data is safe).
        val headerUpdates = HEADERS.map { (sheet, cols) ->
            ValueRange()
                .setRange("$sheet!A1")
                .setValues(listOf(cols))
        }
        sheetsApi.batchWriteRows(spreadsheetId, headerUpdates)

        return spreadsheetId
    }
}

