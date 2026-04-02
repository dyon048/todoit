package com.example.todoit.domain.repository

import com.example.todoit.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow

interface TodoRepository {
    fun observeTodosByGroup(groupId: String): Flow<List<TodoItem>>
    suspend fun getTodosByGroup(groupId: String): List<TodoItem>
    suspend fun getTodoById(id: String): TodoItem?
    suspend fun upsertTodo(todo: TodoItem)
    suspend fun softDeleteTodo(id: String)
    suspend fun softDeleteTodosByGroup(groupId: String)
}

