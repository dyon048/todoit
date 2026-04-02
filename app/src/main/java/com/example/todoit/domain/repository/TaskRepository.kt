package com.example.todoit.domain.repository

import com.example.todoit.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasksByTodo(todoId: String): Flow<List<Task>>
    fun observeAllActiveSortedByScore(): Flow<List<Task>>
    suspend fun getTasksByTodo(todoId: String): List<Task>
    suspend fun getTaskById(id: String): Task?
    suspend fun getAllActiveTasks(): List<Task>
    suspend fun upsertTask(task: Task)
    suspend fun softDeleteTask(id: String)
    suspend fun softDeleteTasksByTodo(todoId: String)
    suspend fun softDeleteInstancesByParent(parentTaskId: String)
    suspend fun getDirtyTasks(): List<Task>
    suspend fun updateScoreCache(taskId: String, score: Float)
    suspend fun getTasksWithReminders(afterMs: Long): List<Task>
}

