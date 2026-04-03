package com.example.todoit.domain.engine

import com.example.todoit.domain.model.Location
import com.example.todoit.domain.model.Schedule
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskScorerTest {

    private lateinit var scheduleEvaluator: ScheduleEvaluator
    private lateinit var scorer: TaskScorer

    // A fixed "now" so tests are deterministic: 2025-01-15 10:00 UTC (Wednesday)
    private val NOW = 1_736_935_200_000L // 2025-01-15T10:00:00Z

    @Before
    fun setUp() {
        scheduleEvaluator = ScheduleEvaluator()
        scorer = TaskScorer(scheduleEvaluator)
    }

    // ── Helper ─────────────────────────────────────────────────────────────────
    private fun task(
        status: TaskStatus = TaskStatus.PENDING,
        priority: Int = 3,
        dueDate: Long = NOW + 100 * 3_600_000L,  // 100h > 72h → 0 due-date pts
        locationId: String? = null,
    ) = Task(
        id = "t1", todoId = "todo1", title = "Test",
        status = status, priority = priority, dueDate = dueDate,
        startTime = null, reminderAt = null, locationId = locationId,
        scheduleId = null, inheritSchedule = false, recurrenceId = null,
        recurrenceInstanceDate = null, parentTaskId = null, scoreCache = 0f,
        createdAt = NOW, updatedAt = NOW, deletedAt = null,
    )

    private fun location(lat: Double, lng: Double, radius: Float = 500f) = Location(
        id = "loc1", label = "Office",
        latitude = lat, longitude = lng,
        radiusMeters = radius,
        updatedAt = NOW, deletedAt = null,
    )

    // ── Priority tests ─────────────────────────────────────────────────────────

    @Test
    fun `priority 1 gives 40 pts base`() {
        val score = scorer.score(task(priority = 1), null, null, null, now = NOW)
        assertEquals(40f, score, 0.01f)
    }

    @Test
    fun `priority 2 gives 30 pts base`() {
        val score = scorer.score(task(priority = 2), null, null, null, now = NOW)
        assertEquals(30f, score, 0.01f)
    }

    @Test
    fun `priority 3 gives 20 pts base`() {
        val score = scorer.score(task(priority = 3), null, null, null, now = NOW)
        assertEquals(20f, score, 0.01f)
    }

    @Test
    fun `priority 4 gives 10 pts base`() {
        val score = scorer.score(task(priority = 4), null, null, null, now = NOW)
        assertEquals(10f, score, 0.01f)
    }

    // ── Due date tests ─────────────────────────────────────────────────────────

    @Test
    fun `overdue task adds 30 pts`() {
        val overdue = task(dueDate = NOW - 1_000)
        val score = scorer.score(overdue, null, null, null, now = NOW)
        // priority 3 (20) + overdue (30) = 50
        assertEquals(50f, score, 0.01f)
    }

    @Test
    fun `due within 2h adds 20 pts`() {
        val due2h = task(dueDate = NOW + 1 * 3_600_000L)
        val score = scorer.score(due2h, null, null, null, now = NOW)
        // priority 3 (20) + due≤2h (20) = 40
        assertEquals(40f, score, 0.01f)
    }

    @Test
    fun `due within 24h adds 10 pts`() {
        val due24h = task(dueDate = NOW + 12 * 3_600_000L)
        val score = scorer.score(due24h, null, null, null, now = NOW)
        // priority 3 (20) + due≤24h (10) = 30
        assertEquals(30f, score, 0.01f)
    }

    @Test
    fun `due within 72h adds 5 pts`() {
        val due72h = task(dueDate = NOW + 50 * 3_600_000L)
        val score = scorer.score(due72h, null, null, null, now = NOW)
        // priority 3 (20) + due≤72h (5) = 25
        assertEquals(25f, score, 0.01f)
    }

    @Test
    fun `due beyond 72h adds 0 pts`() {
        val farFuture = task(dueDate = NOW + 100 * 3_600_000L) // default is already 100h
        val score = scorer.score(farFuture, null, null, null, now = NOW)
        assertEquals(20f, score, 0.01f) // only priority 3 pts
    }

    // ── Status exclusion ───────────────────────────────────────────────────────

    @Test
    fun `DONE tasks return -1`() {
        val score = scorer.score(task(status = TaskStatus.DONE), null, null, null, now = NOW)
        assertEquals(-1f, score, 0.01f)
    }

    // ── Schedule exclusion ─────────────────────────────────────────────────────

    @Test
    fun `schedule inactive returns -1`() {
        // NOW is Wednesday (day 3), schedule only allows Mon (1) and Tue (2)
        val schedule = Schedule("s1", "Weekdays", activeDays = listOf(1, 2),
            startTimeOfDay = null, endTimeOfDay = null, updatedAt = NOW, deletedAt = null)
        val score = scorer.score(task(), null, null, null, schedule, NOW)
        assertEquals(-1f, score, 0.01f)
    }

    @Test
    fun `schedule active returns positive score`() {
        // NOW is Wednesday (day 3), schedule allows Wednesday (3)
        val schedule = Schedule("s1", "Wed", activeDays = listOf(3),
            startTimeOfDay = null, endTimeOfDay = null, updatedAt = NOW, deletedAt = null)
        val score = scorer.score(task(), null, null, null, schedule, NOW)
        assertTrue(score > 0f)
    }

    // ── Location proximity ─────────────────────────────────────────────────────

    @Test
    fun `within radius adds 20 pts`() {
        // User at (0,0), task at (0,0.001) — ~111m apart, radius 500m
        val loc = location(lat = 0.0, lng = 0.001, radius = 500f)
        val score = scorer.score(task(locationId = "loc1"), loc, 0.0, 0.0, now = NOW)
        // priority 3 (20) + within radius (20) + 48h-future (0) = 40
        assertEquals(40f, score, 0.01f)
    }

    @Test
    fun `within 2x radius adds 10 pts`() {
        // User at (0,0), task at (0,0.006) — ~668m, radius = 500m → 668 > 500 but < 1000
        val loc = location(lat = 0.0, lng = 0.006, radius = 500f)
        val score = scorer.score(task(locationId = "loc1"), loc, 0.0, 0.0, now = NOW)
        // priority 3 (20) + 2x radius (10) = 30
        assertEquals(30f, score, 0.01f)
    }

    @Test
    fun `outside 2x radius adds 0 pts`() {
        // User at (0,0), task location far away
        val loc = location(lat = 10.0, lng = 10.0, radius = 500f)
        val score = scorer.score(task(locationId = "loc1"), loc, 0.0, 0.0, now = NOW)
        assertEquals(20f, score, 0.01f) // only priority pts
    }

    @Test
    fun `no location data adds 0 pts`() {
        val score = scorer.score(task(), null, null, null, now = NOW)
        assertEquals(20f, score, 0.01f)
    }

    // ── Haversine ──────────────────────────────────────────────────────────────

    @Test
    fun `haversine distance between same points is 0`() {
        val dist = scorer.haversineDistance(48.8566, 2.3522, 48.8566, 2.3522)
        assertEquals(0.0, dist, 0.01)
    }

    @Test
    fun `haversine distance Paris to London is approx 343 km`() {
        // Paris: 48.8566° N, 2.3522° E
        // London: 51.5074° N, -0.1278° W
        val dist = scorer.haversineDistance(48.8566, 2.3522, 51.5074, -0.1278)
        assertTrue("Expected ~340km, got ${dist / 1000}km", dist in 335_000.0..350_000.0)
    }
}



