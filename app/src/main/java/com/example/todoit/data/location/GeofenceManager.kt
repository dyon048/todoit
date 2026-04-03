package com.example.todoit.data.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import com.example.todoit.domain.repository.LocationRepository
import com.example.todoit.notification.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers and removes Android Geofences for tasks that have a location attached.
 *
 * Max 100 geofences enforced by prioritising highest-scored tasks.
 * Requires ACCESS_FINE_LOCATION + ACCESS_BACKGROUND_LOCATION permissions.
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
) {
    private val client = LocationServices.getGeofencingClient(context)

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    /**
     * Registers geofences for all non-DONE tasks that have a locationId.
     * Silently no-ops if the task list is empty or no tasks have locations.
     */
    @SuppressLint("MissingPermission")
    suspend fun registerGeofencesForActiveTasks(tasks: List<Task>) = withContext(Dispatchers.IO) {
        val eligible = tasks
            .filter { it.locationId != null && it.status != TaskStatus.DONE }
            .sortedByDescending { it.scoreCache }
            .take(100) // Android system limit

        if (eligible.isEmpty()) return@withContext

        val geofences = eligible.mapNotNull { task ->
            val location = locationRepository.getLocationById(task.locationId!!)
                ?: return@mapNotNull null
            Geofence.Builder()
                .setRequestId(task.id)
                .setCircularRegion(
                    location.latitude,
                    location.longitude,
                    location.radiusMeters,
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        if (geofences.isEmpty()) return@withContext

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        runCatching { client.addGeofences(request, buildPendingIntent()).await() }
    }

    /** Removes the geofence for a single task (e.g. after task is deleted or completed). */
    suspend fun removeGeofence(taskId: String) = withContext(Dispatchers.IO) {
        runCatching { client.removeGeofences(listOf(taskId)).await() }
    }

    /** Removes ALL registered geofences (e.g. on sign-out). */
    suspend fun clearAllGeofences() = withContext(Dispatchers.IO) {
        runCatching { client.removeGeofences(buildPendingIntent()).await() }
    }
}

