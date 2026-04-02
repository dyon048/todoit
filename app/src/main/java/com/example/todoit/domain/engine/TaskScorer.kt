package com.example.todoit.domain.engine

import com.example.todoit.domain.model.Location
import com.example.todoit.domain.model.Schedule
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Scoring formula (max 110 pts):
 *  Priority:  P1=40, P2=30, P3=20, P4=10
 *  Due time:  overdue +30, due≤2h +20, due≤24h +10, due≤72h +5
 *  Location:  within radius +20, within 2× radius +10
 *  Schedule:  inactive → excluded (score = -1)
 *  DONE       → excluded (score = -1)
 */
@Singleton
class TaskScorer @Inject constructor(
    private val scheduleEvaluator: ScheduleEvaluator,
) {
    companion object {
        private val PRIORITY_SCORES = mapOf(1 to 40f, 2 to 30f, 3 to 20f, 4 to 10f)
    }

    fun score(
        task: Task,
        location: Location?,
        userLat: Double?,
        userLng: Double?,
        schedule: Schedule?,
        now: Long = System.currentTimeMillis(),
    ): Float {
        if (task.status == TaskStatus.DONE) return -1f

        val nowDt = LocalDateTime.now()
        if (!scheduleEvaluator.isScheduleActive(schedule, nowDt)) return -1f

        var pts = 0f

        // ── Priority ──────────────────────────────────────────────────────
        pts += PRIORITY_SCORES[task.priority] ?: 10f

        // ── Due time proximity ────────────────────────────────────────────
        val diffMs = task.dueDate - now
        pts += when {
            diffMs < 0                       -> 30f  // overdue
            diffMs <= 2 * 3_600_000L         -> 20f  // ≤ 2 h
            diffMs <= 24 * 3_600_000L        -> 10f  // ≤ 24 h
            diffMs <= 72 * 3_600_000L        ->  5f  // ≤ 72 h
            else                             ->  0f
        }

        // ── Location proximity ────────────────────────────────────────────
        if (location != null && userLat != null && userLng != null) {
            val dist = haversineDistance(userLat, userLng, location.latitude, location.longitude)
            val radius = location.radiusMeters.toDouble()
            pts += when {
                dist <= radius   -> 20f
                dist <= radius * 2 -> 10f
                else             ->  0f
            }
        }

        return pts
    }

    /** Haversine distance in meters. */
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0  // Earth radius in metres
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

