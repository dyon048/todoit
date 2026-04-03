package com.example.todoit.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoit.domain.model.TaskStatus
import com.example.todoit.domain.repository.TaskRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles geofence ENTER transitions and posts proximity notifications.
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var taskRepository: TaskRepository

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                event.triggeringGeofences?.forEach { geofence ->
                    val task = taskRepository.getTaskById(geofence.requestId) ?: return@forEach
                    if (task.status != TaskStatus.DONE) {
                        notificationHelper.showProximityNotification(task.id, task.title)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

