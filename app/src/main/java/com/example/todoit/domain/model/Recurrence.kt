package com.example.todoit.domain.model

data class Recurrence(
    val id: String,
    val type: RecurrenceType,
    /** Repeat every N days (used by DAILY and CUSTOM) */
    val interval: Int,
    /** Days of week for WEEKLY recurrence: 1=Mon … 7=Sun */
    val daysOfWeek: List<Int>?,
    val endDate: Long?,
    val maxOccurrences: Int?,
    val updatedAt: Long,
)

