package com.example.todoit.domain.engine

import com.example.todoit.domain.model.Schedule
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.Group
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleEvaluator @Inject constructor() {

    /**
     * Returns true if [schedule] is currently active.
     * A null schedule means "always active".
     */
    fun isScheduleActive(schedule: Schedule?, now: LocalDateTime): Boolean {
        schedule ?: return true  // no schedule = always active

        // Check day of week (Calendar: Mon=1..Sun=7 → Java DayOfWeek value)
        val todayValue = now.dayOfWeek.value   // Mon=1 … Sun=7
        if (schedule.activeDays.isNotEmpty() && todayValue !in schedule.activeDays) return false

        // Check time window
        val startMin = schedule.startTimeOfDay
        val endMin   = schedule.endTimeOfDay
        if (startMin != null && endMin != null) {
            val nowMin = now.hour * 60 + now.minute
            if (nowMin < startMin || nowMin > endMin) return false
        }

        return true
    }

    /**
     * Resolves the effective schedule for a task, respecting inheritance from group.
     */
    fun resolveSchedule(task: Task, groupSchedule: Schedule?): Schedule? =
        if (task.inheritSchedule) groupSchedule else null  // task-level override not stored yet
}

