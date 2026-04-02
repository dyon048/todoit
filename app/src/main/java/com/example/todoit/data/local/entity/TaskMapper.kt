package com.example.todoit.data.local.entity

import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus

fun TaskEntity.toDomain() = Task(
    id = id,
    todoId = todoId,
    title = title,
    status = TaskStatus.valueOf(status),
    priority = priority,
    dueDate = dueDate,
    startTime = startTime,
    reminderAt = reminderAt,
    locationId = locationId,
    scheduleId = scheduleId,
    inheritSchedule = inheritSchedule,
    recurrenceId = recurrenceId,
    recurrenceInstanceDate = recurrenceInstanceDate,
    parentTaskId = parentTaskId,
    scoreCache = scoreCache,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun Task.toEntity(syncedAt: Long? = null) = TaskEntity(
    id = id,
    todoId = todoId,
    title = title,
    status = status.name,
    priority = priority,
    dueDate = dueDate,
    startTime = startTime,
    reminderAt = reminderAt,
    locationId = locationId,
    scheduleId = scheduleId,
    inheritSchedule = inheritSchedule,
    recurrenceId = recurrenceId,
    recurrenceInstanceDate = recurrenceInstanceDate,
    parentTaskId = parentTaskId,
    scoreCache = scoreCache,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncedAt = syncedAt,
)

