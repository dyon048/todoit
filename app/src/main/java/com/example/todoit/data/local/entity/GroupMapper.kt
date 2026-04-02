package com.example.todoit.data.local.entity

import com.example.todoit.domain.model.Group

fun GroupEntity.toDomain() = Group(
    id = id,
    parentId = parentId,
    name = name,
    color = color,
    scheduleId = scheduleId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun Group.toEntity(syncedAt: Long? = null) = GroupEntity(
    id = id,
    parentId = parentId,
    name = name,
    color = color,
    scheduleId = scheduleId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncedAt = syncedAt,
)

