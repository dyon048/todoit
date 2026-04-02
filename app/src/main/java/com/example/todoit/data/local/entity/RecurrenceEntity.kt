package com.example.todoit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurrences")
data class RecurrenceEntity(
    @PrimaryKey val id: String,
    /** "DAILY", "WEEKLY", or "CUSTOM" */
    val type: String,
    val interval: Int,
    /** JSON-encoded List<Int>?, e.g. "[1,3,5]" — only for WEEKLY */
    @ColumnInfo(name = "days_of_week") val daysOfWeek: String?,
    @ColumnInfo(name = "end_date") val endDate: Long?,
    @ColumnInfo(name = "max_occurrences") val maxOccurrences: Int?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "synced_at") val syncedAt: Long?,
)

