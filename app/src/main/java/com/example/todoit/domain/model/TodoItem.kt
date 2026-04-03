package com.example.todoit.domain.model

data class TodoItem(
    val id: String,
    val groupId: String,
    val title: String,
    val description: String?,
    val status: TodoStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
