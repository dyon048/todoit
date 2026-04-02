package com.example.todoit.data.local.entity

import com.example.todoit.domain.model.TodoItem

fun TodoEntity.toDomain() = TodoItem(
    id = id,
    groupId = groupId,
    title = title,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun TodoItem.toEntity(syncedAt: Long? = null) = TodoEntity(
    id = id,
    groupId = groupId,
    title = title,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncedAt = syncedAt,
)

