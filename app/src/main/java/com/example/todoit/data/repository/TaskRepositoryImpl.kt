package com.example.todoit.data.repository

import com.example.todoit.data.local.dao.TaskDao
import com.example.todoit.data.local.entity.toDomain
import com.example.todoit.data.local.entity.toEntity
import com.example.todoit.data.sync.SyncScheduler
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao,
    private val syncScheduler: SyncScheduler,
) : TaskRepository {

    override fun observeTasksByTodo(todoId: String): Flow<List<Task>> =
        dao.observeByTodo(todoId).map { list -> list.map { it.toDomain() } }

    override fun observeAllActiveSortedByScore(): Flow<List<Task>> =
        dao.observeAllActiveSortedByScore().map { list -> list.map { it.toDomain() } }

    override suspend fun getTasksByTodo(todoId: String): List<Task> =
        dao.getByTodo(todoId).map { it.toDomain() }

    override suspend fun getTaskById(id: String): Task? =
        dao.getById(id)?.toDomain()

    override suspend fun getAllActiveTasks(): List<Task> =
        dao.getAllActive().map { it.toDomain() }

    override suspend fun upsertTask(task: Task) {
        val now = System.currentTimeMillis()
        dao.upsert(task.copy(updatedAt = now).toEntity(syncedAt = null))
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun softDeleteTask(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun softDeleteTasksByTodo(todoId: String) {
        dao.softDeleteByTodo(todoId, System.currentTimeMillis())
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun softDeleteInstancesByParent(parentTaskId: String) {
        dao.softDeleteInstancesByParent(parentTaskId, System.currentTimeMillis())
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun getDirtyTasks(): List<Task> =
        dao.getDirtyRows().map { it.toDomain() }

    override suspend fun updateScoreCache(taskId: String, score: Float) {
        dao.updateScoreCache(taskId, score)
    }

    override suspend fun getTasksWithReminders(afterMs: Long): List<Task> =
        dao.getWithReminders(afterMs).map { it.toDomain() }
}

