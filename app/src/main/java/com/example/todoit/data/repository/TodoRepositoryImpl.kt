package com.example.todoit.data.repository

import com.example.todoit.data.local.dao.TodoDao
import com.example.todoit.data.local.entity.toDomain
import com.example.todoit.data.local.entity.toEntity
import com.example.todoit.data.sync.SyncScheduler
import com.example.todoit.domain.model.TodoItem
import com.example.todoit.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TodoRepositoryImpl @Inject constructor(
    private val dao: TodoDao,
    private val syncScheduler: SyncScheduler,
) : TodoRepository {

    override fun observeTodosByGroup(groupId: String): Flow<List<TodoItem>> =
        dao.observeByGroup(groupId).map { list -> list.map { it.toDomain() } }

    override suspend fun getTodosByGroup(groupId: String): List<TodoItem> =
        dao.getByGroup(groupId).map { it.toDomain() }

    override suspend fun getTodoById(id: String): TodoItem? =
        dao.getById(id)?.toDomain()

    override suspend fun upsertTodo(todo: TodoItem) {
        val now = System.currentTimeMillis()
        dao.upsert(todo.copy(updatedAt = now).toEntity(syncedAt = null))
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun softDeleteTodo(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun softDeleteTodosByGroup(groupId: String) {
        dao.softDeleteByGroup(groupId, System.currentTimeMillis())
        syncScheduler.triggerImmediateSync()
    }
}
