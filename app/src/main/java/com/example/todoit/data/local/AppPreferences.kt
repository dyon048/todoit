package com.example.todoit.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        val SPREADSHEET_ID   = stringPreferencesKey("spreadsheet_id")
        val LAST_SYNC_AT     = longPreferencesKey("last_sync_at")
    }

    suspend fun getSpreadsheetId(): String? =
        dataStore.data.map { it[SPREADSHEET_ID] }.first()

    suspend fun setSpreadsheetId(id: String) {
        dataStore.edit { it[SPREADSHEET_ID] = id }
    }

    suspend fun getLastSyncAt(): Long =
        dataStore.data.map { it[LAST_SYNC_AT] ?: 0L }.first()

    suspend fun setLastSyncAt(ts: Long) {
        dataStore.edit { it[LAST_SYNC_AT] = ts }
    }
}

