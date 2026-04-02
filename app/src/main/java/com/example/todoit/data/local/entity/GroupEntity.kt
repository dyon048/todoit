package com.example.todoit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "groups",
    indices = [
        Index("parent_id"),
        Index("updated_at"),
        Index("deleted_at"),
    ]
)
data class GroupEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "parent_id") val parentId: String?,
    val name: String,
    val color: String?,
    @ColumnInfo(name = "schedule_id") val scheduleId: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long?,
)

