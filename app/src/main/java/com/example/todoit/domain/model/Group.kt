package com.example.todoit.domain.model

data class Group(
    val id: String,
    val parentId: String?,
    val name: String,
    val color: String?,
    val scheduleId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

