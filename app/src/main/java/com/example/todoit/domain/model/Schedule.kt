package com.example.todoit.domain.model

data class Schedule(
    val id: String,
    val name: String,
    /** Day-of-week integers: 1=Mon … 7=Sun */
    val activeDays: List<Int>,
    val startTimeOfDay: Int?,
    val endTimeOfDay: Int?,
    val updatedAt: Long,
    val deletedAt: Long?,
)

