package com.example.todoit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index("todo_id"),
        Index("status"),
        Index("due_date"),
        Index("updated_at"),
        Index("deleted_at"),
        Index("score_cache"),
        Index("parent_task_id"),
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "todo_id") val todoId: String,
    val title: String,
    val status: String,
    val priority: Int,
    @ColumnInfo(name = "due_date") val dueDate: Long,
    @ColumnInfo(name = "start_time") val startTime: Long?,
    @ColumnInfo(name = "reminder_at") val reminderAt: Long?,
    @ColumnInfo(name = "location_id") val locationId: String?,
    @ColumnInfo(name = "schedule_id") val scheduleId: String?,
    @ColumnInfo(name = "inherit_schedule") val inheritSchedule: Boolean,
    @ColumnInfo(name = "recurrence_id") val recurrenceId: String?,
    @ColumnInfo(name = "recurrence_instance_date") val recurrenceInstanceDate: Long?,
    @ColumnInfo(name = "parent_task_id") val parentTaskId: String?,
    @ColumnInfo(name = "score_cache") val scoreCache: Float,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long?,
)

