package com.example.todoit.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.todoit.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE todo_id = :todoId AND deleted_at IS NULL ORDER BY due_date ASC")
    fun observeByTodo(todoId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE todo_id = :todoId AND deleted_at IS NULL ORDER BY due_date ASC")
    suspend fun getByTodo(todoId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE deleted_at IS NULL AND status != 'DONE' ORDER BY score_cache DESC")
    fun observeAllActiveSortedByScore(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deleted_at IS NULL AND status != 'DONE' ORDER BY score_cache DESC")
    fun getTopScoredPaged(): PagingSource<Int, TaskEntity>

    @Query("SELECT * FROM tasks WHERE deleted_at IS NULL AND status != 'DONE' ORDER BY score_cache DESC")
    suspend fun getAllActive(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE reminder_at > :afterMs AND deleted_at IS NULL AND status != 'DONE'")
    suspend fun getWithReminders(afterMs: Long): List<TaskEntity>

    @Upsert
    suspend fun upsert(entity: TaskEntity)

    @Query("UPDATE tasks SET deleted_at = :ts, updated_at = :ts, synced_at = NULL WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long)

    @Query("UPDATE tasks SET deleted_at = :ts, updated_at = :ts, synced_at = NULL WHERE todo_id = :todoId")
    suspend fun softDeleteByTodo(todoId: String, ts: Long)

    @Query("UPDATE tasks SET deleted_at = :ts, updated_at = :ts, synced_at = NULL WHERE parent_task_id = :parentId")
    suspend fun softDeleteInstancesByParent(parentId: String, ts: Long)

    @Query("UPDATE tasks SET score_cache = :score WHERE id = :id")
    suspend fun updateScoreCache(id: String, score: Float)

    @Query("""
        SELECT * FROM tasks 
        WHERE synced_at IS NULL OR synced_at < updated_at
        LIMIT 500
    """)
    suspend fun getDirtyRows(): List<TaskEntity>

    @Query("UPDATE tasks SET synced_at = :ts WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, ts: Long)
}

