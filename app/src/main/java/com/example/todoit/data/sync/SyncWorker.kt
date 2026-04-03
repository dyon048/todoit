package com.example.todoit.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            syncManager.fullSync()
            Result.success()
        }.getOrElse { e ->
            val msg = e.message ?: ""
            when {
                msg.contains("Not signed in") -> Result.failure() // no point retrying
                msg.contains("401")           -> Result.failure()
                runAttemptCount < 4           -> Result.retry()
                else                          -> Result.failure()
            }
        }
    }
}
