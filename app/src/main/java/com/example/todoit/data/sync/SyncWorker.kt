package com.example.todoit.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.todoit.data.local.AppPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager,
    private val prefs: AppPreferences,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val spreadsheetId = prefs.getSpreadsheetId()
        if (spreadsheetId.isNullOrBlank()) return Result.success() // not configured yet

        return runCatching {
            syncManager.fullSync()
            Result.success()
        }.getOrElse { e ->
            val msg = e.message ?: ""
            when {
                msg.contains("401") || msg.contains("sign in") -> Result.failure()
                runAttemptCount < 4                             -> Result.retry()
                else                                            -> Result.failure()
            }
        }
    }
}

