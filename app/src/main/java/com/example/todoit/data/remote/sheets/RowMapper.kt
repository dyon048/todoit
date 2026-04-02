package com.example.todoit.data.remote.sheets

import com.example.todoit.data.local.entity.GroupEntity
import com.example.todoit.data.local.entity.LocationEntity
import com.example.todoit.data.local.entity.RecurrenceEntity
import com.example.todoit.data.local.entity.ScheduleEntity
import com.example.todoit.data.local.entity.TaskEntity
import com.example.todoit.data.local.entity.TodoEntity

// ─────────────────────────────────────────────────────────────────────────────
// Sheet column order constants (0-based index = column position)
// ─────────────────────────────────────────────────────────────────────────────

// GROUPS:  id | parent_id | name | color | schedule_id | created_at | updated_at | deleted_at | synced_at
fun GroupEntity.toSheetRow(): List<Any> = listOf(
    id, parentId ?: "", name, color ?: "", scheduleId ?: "",
    createdAt, updatedAt, deletedAt ?: "", syncedAt ?: ""
)

fun List<Any>.toGroupEntity(): GroupEntity? = runCatching {
    GroupEntity(
        id          = get(0).toString().takeIf { it.isNotBlank() } ?: return null,
        parentId    = get(1).toString().ifBlank { null },
        name        = get(2).toString(),
        color       = get(3).toString().ifBlank { null },
        scheduleId  = get(4).toString().ifBlank { null },
        createdAt   = get(5).toString().toLong(),
        updatedAt   = get(6).toString().toLong(),
        deletedAt   = get(7).toString().toLongOrNull(),
        syncedAt    = get(8).toString().toLongOrNull(),
    )
}.getOrNull()

// TODO_ITEMS: id | group_id | title | description | created_at | updated_at | deleted_at | synced_at
fun TodoEntity.toSheetRow(): List<Any> = listOf(
    id, groupId, title, description ?: "",
    createdAt, updatedAt, deletedAt ?: "", syncedAt ?: ""
)

fun List<Any>.toTodoEntity(): TodoEntity? = runCatching {
    TodoEntity(
        id          = get(0).toString().takeIf { it.isNotBlank() } ?: return null,
        groupId     = get(1).toString(),
        title       = get(2).toString(),
        description = get(3).toString().ifBlank { null },
        createdAt   = get(4).toString().toLong(),
        updatedAt   = get(5).toString().toLong(),
        deletedAt   = get(6).toString().toLongOrNull(),
        syncedAt    = get(7).toString().toLongOrNull(),
    )
}.getOrNull()

// TASKS: id | todo_id | title | status | priority | due_date | start_time | reminder_at | location_id
//        | schedule_id | inherit_schedule | recurrence_id | recurrence_instance_date | parent_task_id
//        | score_cache | created_at | updated_at | deleted_at | synced_at
fun TaskEntity.toSheetRow(): List<Any> = listOf(
    id, todoId, title, status, priority, dueDate,
    startTime ?: "", reminderAt ?: "", locationId ?: "", scheduleId ?: "",
    inheritSchedule, recurrenceId ?: "", recurrenceInstanceDate ?: "",
    parentTaskId ?: "", scoreCache, createdAt, updatedAt, deletedAt ?: "", syncedAt ?: ""
)

fun List<Any>.toTaskEntity(): TaskEntity? = runCatching {
    TaskEntity(
        id                     = get(0).toString().takeIf { it.isNotBlank() } ?: return null,
        todoId                 = get(1).toString(),
        title                  = get(2).toString(),
        status                 = get(3).toString(),
        priority               = get(4).toString().toInt(),
        dueDate                = get(5).toString().toLong(),
        startTime              = get(6).toString().toLongOrNull(),
        reminderAt             = get(7).toString().toLongOrNull(),
        locationId             = get(8).toString().ifBlank { null },
        scheduleId             = get(9).toString().ifBlank { null },
        inheritSchedule        = get(10).toString().toBoolean(),
        recurrenceId           = get(11).toString().ifBlank { null },
        recurrenceInstanceDate = get(12).toString().toLongOrNull(),
        parentTaskId           = get(13).toString().ifBlank { null },
        scoreCache             = get(14).toString().toFloatOrNull() ?: 0f,
        createdAt              = get(15).toString().toLong(),
        updatedAt              = get(16).toString().toLong(),
        deletedAt              = get(17).toString().toLongOrNull(),
        syncedAt               = get(18).toString().toLongOrNull(),
    )
}.getOrNull()

// SCHEDULES: id | name | active_days | start_time_of_day | end_time_of_day | updated_at | deleted_at | synced_at
fun ScheduleEntity.toSheetRow(): List<Any> = listOf(
    id, name, activeDays, startTimeOfDay ?: "", endTimeOfDay ?: "",
    updatedAt, deletedAt ?: "", syncedAt ?: ""
)

fun List<Any>.toScheduleEntity(): ScheduleEntity? = runCatching {
    ScheduleEntity(
        id             = get(0).toString().takeIf { it.isNotBlank() } ?: return null,
        name           = get(1).toString(),
        activeDays     = get(2).toString(),
        startTimeOfDay = get(3).toString().toIntOrNull(),
        endTimeOfDay   = get(4).toString().toIntOrNull(),
        updatedAt      = get(5).toString().toLong(),
        deletedAt      = get(6).toString().toLongOrNull(),
        syncedAt       = get(7).toString().toLongOrNull(),
    )
}.getOrNull()

// RECURRENCES: id | type | interval | days_of_week | end_date | max_occurrences | updated_at | synced_at
fun RecurrenceEntity.toSheetRow(): List<Any> = listOf(
    id, type, interval, daysOfWeek ?: "", endDate ?: "", maxOccurrences ?: "",
    updatedAt, syncedAt ?: ""
)

fun List<Any>.toRecurrenceEntity(): RecurrenceEntity? = runCatching {
    RecurrenceEntity(
        id             = get(0).toString().takeIf { it.isNotBlank() } ?: return null,
        type           = get(1).toString(),
        interval       = get(2).toString().toInt(),
        daysOfWeek     = get(3).toString().ifBlank { null },
        endDate        = get(4).toString().toLongOrNull(),
        maxOccurrences = get(5).toString().toIntOrNull(),
        updatedAt      = get(6).toString().toLong(),
        syncedAt       = get(7).toString().toLongOrNull(),
    )
}.getOrNull()

// LOCATIONS: id | label | latitude | longitude | radius_meters | updated_at | deleted_at | synced_at
fun LocationEntity.toSheetRow(): List<Any> = listOf(
    id, label ?: "", latitude, longitude, radiusMeters,
    updatedAt, deletedAt ?: "", syncedAt ?: ""
)

fun List<Any>.toLocationEntity(): LocationEntity? = runCatching {
    LocationEntity(
        id           = get(0).toString().takeIf { it.isNotBlank() } ?: return null,
        label        = get(1).toString(),
        latitude     = get(2).toString().toDouble(),
        longitude    = get(3).toString().toDouble(),
        radiusMeters = get(4).toString().toFloat(),
        updatedAt    = get(5).toString().toLong(),
        deletedAt    = get(6).toString().toLongOrNull(),
        syncedAt     = get(7).toString().toLongOrNull(),
    )
}.getOrNull()


