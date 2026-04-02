package com.example.todoit.domain.model

data class Task(
    val id: String,
    val todoId: String,
    val title: String,
    val status: TaskStatus,
    /** 1=Critical, 2=High, 3=Medium, 4=Low */
    val priority: Int,
    val dueDate: Long,
    val startTime: Long?,
    val reminderAt: Long?,
    val locationId: String?,
    val scheduleId: String?,
    val inheritSchedule: Boolean,
    val recurrenceId: String?,
    val recurrenceInstanceDate: Long?,
    val parentTaskId: String?,
    val scoreCache: Float,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

