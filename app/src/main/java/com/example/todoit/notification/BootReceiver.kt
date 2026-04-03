package com.example.todoit.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoit.data.location.GeofenceManager
import com.example.todoit.domain.model.TaskStatus
import com.example.todoit.domain.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-registers exact alarms and geofences after device reboot.
 * Triggered by ACTION_BOOT_COMPLETED intent.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var taskRepository: TaskRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val now   = System.currentTimeMillis()
                val tasks = taskRepository.getAllActiveTasks()
                    .filter { it.status != TaskStatus.DONE }

                // Re-register exact alarms for tasks with future reminders
                reminderScheduler.rescheduleAll(
                    tasks.filter { it.reminderAt != null && it.reminderAt > now }
                )

                // Re-register geofences
                geofenceManager.registerGeofencesForActiveTasks(tasks)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

