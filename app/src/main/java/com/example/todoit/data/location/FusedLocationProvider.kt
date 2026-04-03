package com.example.todoit.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides passive location updates using FusedLocationProviderClient.
 *
 * - Uses PRIORITY_PASSIVE (no active GPS; piggybacks on other apps' requests).
 * - Reports at most once per minute; interval is 5 minutes.
 * - Never call this without checking ACCESS_FINE_LOCATION permission first.
 */
@Singleton
class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    private val _locationUpdates = MutableSharedFlow<Pair<Double, Double>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Emits (latitude, longitude) pairs whenever a passive location update arrives. */
    val locationUpdates: SharedFlow<Pair<Double, Double>> = _locationUpdates.asSharedFlow()

    private var callback: LocationCallback? = null

    /**
     * Starts passive location updates. Safe to call multiple times — subsequent calls are no-ops.
     * Requires ACCESS_FINE_LOCATION permission to be granted before calling.
     */
    @SuppressLint("MissingPermission")
    fun startPassiveUpdates() {
        if (callback != null) return // already running

        val request = LocationRequest.Builder(
            Priority.PRIORITY_PASSIVE,
            5 * 60 * 1_000L, // desired interval: 5 minutes
        ).apply {
            setMinUpdateIntervalMillis(60_000L)   // at most once per minute
            setWaitForAccurateLocation(false)
        }.build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                _locationUpdates.tryEmit(loc.latitude to loc.longitude)
            }
        }

        client.requestLocationUpdates(request, callback!!, Looper.getMainLooper())

        // Emit last-known location immediately so the scoring engine has a starting point
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) _locationUpdates.tryEmit(loc.latitude to loc.longitude)
        }
    }

    /** Stops passive updates. Call from a ViewModel's onCleared() if appropriate. */
    fun stopUpdates() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }
}

