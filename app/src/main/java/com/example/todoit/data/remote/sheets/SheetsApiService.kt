package com.example.todoit.data.remote.sheets

import android.content.Context
import com.example.todoit.data.local.AppPreferences
import com.example.todoit.data.remote.auth.GoogleAuthManager
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SheetsApiService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: GoogleAuthManager,
    private val prefs: AppPreferences,
) {
    companion object {
        private const val APP_NAME = "TodoIt"
        val SHEET_NAMES = listOf("groups", "todo_items", "tasks", "schedules", "recurrences", "locations", "sync_meta")
    }

    private fun buildService(): Sheets {
        val account = authManager.getSignedInAccount()
            ?: error("Not signed in — cannot access Sheets API")
        val credential = authManager.getCredential(account)
        return Sheets.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName(APP_NAME)
            .build()
    }

    /** Read all 7 sheets in a single batchGet call. */
    suspend fun batchReadAllSheets(spreadsheetId: String): Map<String, List<List<Any>>> =
        withContext(Dispatchers.IO) {
            val ranges = SHEET_NAMES.map { "$it!A:ZZ" }
            val response = buildService().spreadsheets().values()
                .batchGet(spreadsheetId)
                .setRanges(ranges)
                .execute()
            response.valueRanges.orEmpty().associate { vr ->
                val sheetName = vr.range?.substringBefore("!")?.trim('\'') ?: ""
                sheetName to (vr.getValues()?.map { row -> row.map { it } } ?: emptyList())
            }
        }

    /** Write multiple ranges in a single batchUpdate call. */
    suspend fun batchWriteRows(spreadsheetId: String, updates: List<ValueRange>): Unit =
        withContext(Dispatchers.IO) {
            if (updates.isEmpty()) return@withContext
            val body = BatchUpdateValuesRequest()
                .setValueInputOption("RAW")
                .setData(updates)
            buildService().spreadsheets().values()
                .batchUpdate(spreadsheetId, body)
                .execute()
        }

    /** Append rows at the end of a specific sheet. */
    suspend fun appendRows(spreadsheetId: String, sheetName: String, rows: List<List<Any>>): Unit =
        withContext(Dispatchers.IO) {
            if (rows.isEmpty()) return@withContext
            val body = ValueRange().setValues(rows)
            buildService().spreadsheets().values()
                .append(spreadsheetId, "$sheetName!A1", body)
                .setValueInputOption("RAW")
                .setInsertDataOption("INSERT_ROWS")
                .execute()
        }

    /** Creates a new spreadsheet and returns its ID. */
    suspend fun createSpreadsheet(title: String): String =
        withContext(Dispatchers.IO) {
            val spreadsheet = Spreadsheet()
                .setProperties(SpreadsheetProperties().setTitle(title))
            buildService().spreadsheets().create(spreadsheet).execute().spreadsheetId
        }

    /** Adds a sheet tab to an existing spreadsheet. */
    suspend fun addSheet(spreadsheetId: String, sheetName: String): Unit =
        withContext(Dispatchers.IO) {
            val request = com.google.api.services.sheets.v4.model.Request()
                .setAddSheet(
                    com.google.api.services.sheets.v4.model.AddSheetRequest()
                        .setProperties(
                            com.google.api.services.sheets.v4.model.SheetProperties()
                                .setTitle(sheetName)
                        )
                )
            val body = com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest()
                .setRequests(listOf(request))
            buildService().spreadsheets().batchUpdate(spreadsheetId, body).execute()
        }
}

