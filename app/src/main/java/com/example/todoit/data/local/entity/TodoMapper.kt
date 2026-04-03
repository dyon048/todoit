package com.example.todoit.data.local.entity

import com.example.todoit.domain.model.TodoItem
import com.example.todoit.domain.model.TodoStatus

fun TodoEntity.toDomain() = TodoItem(
    id = id,
    groupId = groupId,
    title = title,
    description = description,
    status = runCatching { TodoStatus.valueOf(status) }.getOrDefault(TodoStatus.PENDING),
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun TodoItem.toEntity(syncedAt: Long? = null) = TodoEntity(
    id = id,
    groupId = groupId,
    title = title,
    description = description,
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncedAt = syncedAt,
)
