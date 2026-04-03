package com.example.todoit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_items",
    indices = [
        Index("group_id"),
        Index("status"),
        Index("updated_at"),
        Index("deleted_at"),
    ]
)
data class TodoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "group_id") val groupId: String,
    val title: String,
    val description: String?,
    @ColumnInfo(name = "status", defaultValue = "PENDING") val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long?,
)
