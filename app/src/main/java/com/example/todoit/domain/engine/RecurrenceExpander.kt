package com.example.todoit.domain.engine

import com.example.todoit.domain.model.Recurrence
import com.example.todoit.domain.model.RecurrenceType
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurrenceExpander @Inject constructor() {

    /**
     * Generates future instances of [rootTask] up to [upTo] ms.
     * Skips dates that overlap with existing instances (caller should deduplicate by
     * parentTaskId+recurrenceInstanceDate before persisting).
     */
    fun expand(
        rootTask: Task,
        recurrence: Recurrence,
        from: Long = System.currentTimeMillis(),
        upTo: Long = from + 90L * 24 * 3_600_000,
    ): List<Task> {
        val results = mutableListOf<Task>()
        var currentMs = rootTask.dueDate
        var count = 0

        val endCap = minOf(upTo, recurrence.endDate ?: Long.MAX_VALUE)

        while (currentMs <= endCap) {
            val next = nextOccurrence(currentMs, recurrence)
            if (next > endCap) break

            val maxOcc = recurrence.maxOccurrences
            if (maxOcc != null && count >= maxOcc) break

            if (next > from) {
                results += rootTask.copy(
                    id                     = UUID.randomUUID().toString(),
                    dueDate                = next,
                    startTime              = rootTask.startTime?.let { it + (next - rootTask.dueDate) },
                    reminderAt             = rootTask.reminderAt?.let { it + (next - rootTask.dueDate) },
                    status                 = TaskStatus.PENDING,
                    parentTaskId           = rootTask.id,
                    recurrenceInstanceDate = next,
                    scoreCache             = 0f,
                    createdAt              = System.currentTimeMillis(),
                    updatedAt              = System.currentTimeMillis(),
                    deletedAt              = null,
                )
                count++
            }
            currentMs = next
        }
        return results
    }

    /** Returns the next occurrence timestamp after [currentMs]. */
    fun nextOccurrence(currentMs: Long, recurrence: Recurrence): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentMs }

        return when (recurrence.type) {
            RecurrenceType.DAILY  -> {
                cal.add(Calendar.DAY_OF_YEAR, recurrence.interval)
                cal.timeInMillis
            }
            RecurrenceType.WEEKLY -> {
                val daysOfWeek = recurrence.daysOfWeek
                if (daysOfWeek.isNullOrEmpty()) {
                    cal.add(Calendar.WEEK_OF_YEAR, recurrence.interval)
                    cal.timeInMillis
                } else {
                    // Advance day-by-day until we hit a matching day in the cycle.
                    // App convention: Mon=1 … Sun=7.
                    // Java Calendar: Sun=1, Mon=2 … Sat=7 → convert: appDay = if (Sun) 7 else calDay - 1
                    repeat(7) { cal.add(Calendar.DAY_OF_YEAR, 1) }
                    var attempts = 0
                    fun calToAppDay(calDay: Int) = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
                    while (calToAppDay(cal.get(Calendar.DAY_OF_WEEK)) !in daysOfWeek && attempts++ < 7) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    cal.timeInMillis
                }
            }
            RecurrenceType.CUSTOM -> {
                cal.add(Calendar.DAY_OF_YEAR, recurrence.interval)
                cal.timeInMillis
            }
        }
    }
}


