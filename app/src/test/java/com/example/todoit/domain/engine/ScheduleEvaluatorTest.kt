package com.example.todoit.domain.engine

import com.example.todoit.domain.model.Schedule
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class ScheduleEvaluatorTest {

    private lateinit var evaluator: ScheduleEvaluator

    @Before
    fun setUp() {
        evaluator = ScheduleEvaluator()
    }

    // Helper that returns a Calendar for a specific day-of-week and time
    // weekday: Calendar.MONDAY .. Calendar.SUNDAY
    private fun calendarAt(weekday: Int, hourOfDay: Int, minute: Int = 0): java.time.LocalDateTime {
        // Map Calendar.MONDAY=2..SUNDAY=1 → ISO 1=Mon..7=Sun
        val isoDay = when (weekday) {
            Calendar.MONDAY    -> 1
            Calendar.TUESDAY   -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY  -> 4
            Calendar.FRIDAY    -> 5
            Calendar.SATURDAY  -> 6
            Calendar.SUNDAY    -> 7
            else -> throw IllegalArgumentException("Bad weekday")
        }
        // Build a LocalDateTime on the given ISO day/hour/minute
        // Use 2025-01-06 (Monday) as anchor
        val monday = java.time.LocalDateTime.of(2025, 1, 6, 0, 0, 0)
        return monday.plusDays((isoDay - 1).toLong()).withHour(hourOfDay).withMinute(minute)
    }

    private fun schedule(
        activeDays: List<Int> = emptyList(),
        startMin: Int? = null,
        endMin: Int? = null,
    ) = Schedule("s1", "Test", activeDays, startMin, endMin, 0L, null)

    // ── Null schedule = always active ──────────────────────────────────────────

    @Test
    fun `null schedule is always active`() {
        val now = calendarAt(Calendar.SUNDAY, 3)
        assertTrue(evaluator.isScheduleActive(null, now))
    }

    // ── Day-of-week filtering ─────────────────────────────────────────────────

    @Test
    fun `empty activeDays means all days active`() {
        val now = calendarAt(Calendar.SATURDAY, 12)
        assertTrue(evaluator.isScheduleActive(schedule(activeDays = emptyList()), now))
    }

    @Test
    fun `current day in activeDays returns true`() {
        // Wednesday = ISO 3
        val now = calendarAt(Calendar.WEDNESDAY, 12)
        assertTrue(evaluator.isScheduleActive(schedule(activeDays = listOf(1, 3, 5)), now))
    }

    @Test
    fun `current day NOT in activeDays returns false`() {
        // Sunday = ISO 7
        val now = calendarAt(Calendar.SUNDAY, 12)
        assertFalse(evaluator.isScheduleActive(schedule(activeDays = listOf(1, 2, 3, 4, 5)), now))
    }

    // ── Time window filtering ──────────────────────────────────────────────────

    @Test
    fun `time within window returns true`() {
        val now = calendarAt(Calendar.MONDAY, 10, 30) // 10:30 → 630 min
        val sched = schedule(startMin = 9 * 60, endMin = 18 * 60) // 9:00–18:00
        assertTrue(evaluator.isScheduleActive(sched, now))
    }

    @Test
    fun `time before window returns false`() {
        val now = calendarAt(Calendar.MONDAY, 8, 0) // 08:00 → 480 min
        val sched = schedule(startMin = 9 * 60, endMin = 18 * 60)
        assertFalse(evaluator.isScheduleActive(sched, now))
    }

    @Test
    fun `time after window returns false`() {
        val now = calendarAt(Calendar.MONDAY, 19, 0) // 19:00 → 1140 min
        val sched = schedule(startMin = 9 * 60, endMin = 18 * 60)
        assertFalse(evaluator.isScheduleActive(sched, now))
    }

    @Test
    fun `time exactly at start of window returns true`() {
        val now = calendarAt(Calendar.MONDAY, 9, 0)
        val sched = schedule(startMin = 9 * 60, endMin = 18 * 60)
        assertTrue(evaluator.isScheduleActive(sched, now))
    }

    @Test
    fun `time exactly at end of window returns true`() {
        val now = calendarAt(Calendar.MONDAY, 18, 0)
        val sched = schedule(startMin = 9 * 60, endMin = 18 * 60)
        assertTrue(evaluator.isScheduleActive(sched, now))
    }

    @Test
    fun `only startMin set without endMin means no time filter`() {
        // When only one of the two is set, the time filter should be skipped
        val now = calendarAt(Calendar.MONDAY, 7, 0)
        val sched = schedule(startMin = 9 * 60, endMin = null)
        assertTrue(evaluator.isScheduleActive(sched, now))
    }

    // ── Combined day + time ────────────────────────────────────────────────────

    @Test
    fun `active day but outside time window returns false`() {
        // Monday in activeDays, but time before window
        val now = calendarAt(Calendar.MONDAY, 7, 0)
        val sched = schedule(activeDays = listOf(1), startMin = 9 * 60, endMin = 18 * 60)
        assertFalse(evaluator.isScheduleActive(sched, now))
    }

    @Test
    fun `active day and within time window returns true`() {
        val now = calendarAt(Calendar.MONDAY, 10, 0)
        val sched = schedule(activeDays = listOf(1), startMin = 9 * 60, endMin = 18 * 60)
        assertTrue(evaluator.isScheduleActive(sched, now))
    }

    @Test
    fun `inactive day even if within time window returns false`() {
        // Sunday not in Mon-Fri schedule
        val now = calendarAt(Calendar.SUNDAY, 10, 0)
        val sched = schedule(activeDays = listOf(1, 2, 3, 4, 5), startMin = 9 * 60, endMin = 18 * 60)
        assertFalse(evaluator.isScheduleActive(sched, now))
    }

    // ── resolveSchedule ───────────────────────────────────────────────────────

    @Test
    fun `resolveSchedule returns null when inheritSchedule is false and no scheduleId`() {
        val task = fakeTask(inheritSchedule = false, scheduleId = null)
        val result = evaluator.resolveSchedule(task, null)
        assertTrue(result == null)
    }

    @Test
    fun `resolveSchedule returns null when inheritSchedule is true but no group schedule`() {
        val task = fakeTask(inheritSchedule = true, scheduleId = null)
        val result = evaluator.resolveSchedule(task, null)
        assertTrue(result == null)
    }

    private fun fakeTask(inheritSchedule: Boolean, scheduleId: String?) = Task(
        id = "t1", todoId = "todo1", title = "T",
        status = TaskStatus.PENDING, priority = 2,
        dueDate = 0L, startTime = null, reminderAt = null,
        locationId = null, scheduleId = scheduleId,
        inheritSchedule = inheritSchedule,
        recurrenceId = null, recurrenceInstanceDate = null,
        parentTaskId = null, scoreCache = 0f,
        createdAt = 0L, updatedAt = 0L, deletedAt = null,
    )
}

