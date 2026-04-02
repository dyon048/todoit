package com.example.todoit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** JSON-encoded List<Int>, e.g. "[1,2,3,4,5]" */
    @ColumnInfo(name = "active_days") val activeDays: String,
    @ColumnInfo(name = "start_time_of_day") val startTimeOfDay: Int?,
    @ColumnInfo(name = "end_time_of_day") val endTimeOfDay: Int?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long?,
)

