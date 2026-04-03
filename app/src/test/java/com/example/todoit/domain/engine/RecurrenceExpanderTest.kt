package com.example.todoit.domain.engine

import com.example.todoit.domain.model.Recurrence
import com.example.todoit.domain.model.RecurrenceType
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class RecurrenceExpanderTest {

    private lateinit var expander: RecurrenceExpander

    // Anchor: 2025-01-06 09:00 (Monday)
    private val MONDAY_9AM: Long = run {
        val cal = Calendar.getInstance()
        cal.set(2025, Calendar.JANUARY, 6, 9, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    @Before
    fun setUp() {
        expander = RecurrenceExpander()
    }

    private fun baseTask(dueDate: Long = MONDAY_9AM) = Task(
        id = "root", todoId = "todo1", title = "Root",
        status = TaskStatus.PENDING, priority = 2,
        dueDate = dueDate, startTime = null, reminderAt = null,
        locationId = null, scheduleId = null, inheritSchedule = false,
        recurrenceId = "rec1", recurrenceInstanceDate = null, parentTaskId = null,
        scoreCache = 0f, createdAt = MONDAY_9AM, updatedAt = MONDAY_9AM, deletedAt = null,
    )

    private fun recurrence(
        type: RecurrenceType = RecurrenceType.DAILY,
        interval: Int = 1,
        daysOfWeek: List<Int>? = null,
        endDate: Long? = null,
        maxOccurrences: Int? = null,
    ) = Recurrence("rec1", type, interval, daysOfWeek, endDate, maxOccurrences, MONDAY_9AM)

    // ── DAILY ─────────────────────────────────────────────────────────────────

    @Test
    fun `daily recurrence generates correct number of instances within window`() {
        val from = MONDAY_9AM
        val upTo = MONDAY_9AM + 7L * 24 * 3_600_000 // 7 days ahead
        val instances = expander.expand(baseTask(), recurrence(type = RecurrenceType.DAILY, interval = 1), from, upTo)
        // 7 days ahead: instances at day+1, +2, +3, +4, +5, +6, +7
        assertEquals(7, instances.size)
    }

    @Test
    fun `daily recurrence instances all have parentTaskId set`() {
        val from = MONDAY_9AM
        val upTo = MONDAY_9AM + 3L * 24 * 3_600_000
        val instances = expander.expand(baseTask(), recurrence(type = RecurrenceType.DAILY, interval = 1), from, upTo)
        assertTrue(instances.all { it.parentTaskId == "root" })
    }

    @Test
    fun `daily recurrence instances all have PENDING status`() {
        val from = MONDAY_9AM
        val upTo = MONDAY_9AM + 3L * 24 * 3_600_000
        val instances = expander.expand(baseTask(), recurrence(type = RecurrenceType.DAILY, interval = 1), from, upTo)
        assertTrue(instances.all { it.status == TaskStatus.PENDING })
    }

    @Test
    fun `daily recurrence every 2 days generates correct instances`() {
        val from = MONDAY_9AM
        val upTo = MONDAY_9AM + 14L * 24 * 3_600_000 // 14 days
        val instances = expander.expand(baseTask(), recurrence(type = RecurrenceType.DAILY, interval = 2), from, upTo)
        // days 2, 4, 6, 8, 10, 12, 14 → 7 instances
        assertEquals(7, instances.size)
    }

    @Test
    fun `maxOccurrences caps instance count`() {
        val from = MONDAY_9AM
        val upTo = MONDAY_9AM + 90L * 24 * 3_600_000
        val instances = expander.expand(
            baseTask(),
            recurrence(type = RecurrenceType.DAILY, interval = 1, maxOccurrences = 5),
            from, upTo,
        )
        assertEquals(5, instances.size)
    }

    @Test
    fun `endDate caps instance generation`() {
        val endDate = MONDAY_9AM + 4L * 24 * 3_600_000 // 4 days later
        val from = MONDAY_9AM
        val upTo = MONDAY_9AM + 30L * 24 * 3_600_000
        val instances = expander.expand(
            baseTask(),
            recurrence(type = RecurrenceType.DAILY, interval = 1, endDate = endDate),
            from, upTo,
        )
        // instances at day+1, +2, +3, +4 = 4 instances
        assertEquals(4, instances.size)
    }

    @Test
    fun `instances have unique IDs`() {
        val from = MONDAY_9AM
        val upTo = MONDAY_9AM + 7L * 24 * 3_600_000
        val instances = expander.expand(baseTask(), recurrence(type = RecurrenceType.DAILY, interval = 1), from, upTo)
        val ids = instances.map { it.id }.toSet()
        assertEquals(instances.size, ids.size)
    }

    // ── WEEKLY ─────────────────────────────────────────────────────────────────

    @Test
    fun `weekly recurrence without daysOfWeek generates weekly instances`() {
        val from = MONDAY_9AM
        val upTo = MONDAY_9AM + 28L * 24 * 3_600_000 // 4 weeks
        val instances = expander.expand(baseTask(), recurrence(type = RecurrenceType.WEEKLY, interval = 1), from, upTo)
        // Should generate on weeks 1, 2, 3, 4 after the root
        assertEquals(4, instances.size)
    }

    @Test
    fun `weekly recurrence with specific daysOfWeek produces instances on matching days`() {
        // Root is Monday (ISO 1); daysOfWeek = [1, 3] (Mon and Wed).
        // Algorithm: advance 7 days first → next Monday, which IS in [1,3] → stays on Monday.
        // So two instances: Mon Jan 13 and Mon Jan 20 within the 14-day window.
        val from = MONDAY_9AM
        val upTo = MONDAY_9AM + 14L * 24 * 3_600_000 // 2 weeks
        val instances = expander.expand(
            baseTask(),
            recurrence(type = RecurrenceType.WEEKLY, interval = 1, daysOfWeek = listOf(1, 3)),
            from, upTo,
        )
        assertEquals(2, instances.size)
        // All generated instances must be on a day in [1, 3]
        instances.forEach { instance ->
            val cal = Calendar.getInstance().apply { timeInMillis = instance.dueDate }
            val javaDay = cal.get(Calendar.DAY_OF_WEEK)
            val appDay = if (javaDay == Calendar.SUNDAY) 7 else javaDay - 1
            assertTrue("Day $appDay not in [1,3]", appDay == 1 || appDay == 3)
        }
    }

    // ── nextOccurrence ─────────────────────────────────────────────────────────

    @Test
    fun `nextOccurrence daily adds correct days`() {
        val next = expander.nextOccurrence(MONDAY_9AM, recurrence(type = RecurrenceType.DAILY, interval = 3))
        val expected = MONDAY_9AM + 3L * 24 * 3_600_000
        assertEquals(expected, next)
    }

    @Test
    fun `nextOccurrence CUSTOM same as DAILY`() {
        val daily = expander.nextOccurrence(MONDAY_9AM, recurrence(type = RecurrenceType.DAILY, interval = 5))
        val custom = expander.nextOccurrence(MONDAY_9AM, recurrence(type = RecurrenceType.CUSTOM, interval = 5))
        assertEquals(daily, custom)
    }

    // ── Reminder/startTime offset preservation ────────────────────────────────

    @Test
    fun `reminder offset is preserved in instances`() {
        val reminderOffset = 30L * 60 * 1000 // 30 min before
        val taskWithReminder = baseTask().copy(reminderAt = MONDAY_9AM - reminderOffset)
        val from = MONDAY_9AM
        val upTo = MONDAY_9AM + 3L * 24 * 3_600_000
        val instances = expander.expand(
            taskWithReminder,
            recurrence(type = RecurrenceType.DAILY, interval = 1),
            from, upTo,
        )
        instances.forEach { instance ->
            val expectedReminder = instance.dueDate - reminderOffset
            assertEquals(expectedReminder, instance.reminderAt)
        }
    }
}


