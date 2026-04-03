package com.example.todoit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.todoit.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("SELECT * FROM todo_items WHERE group_id = :groupId AND deleted_at IS NULL ORDER BY created_at DESC")
    fun observeByGroup(groupId: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todo_items WHERE group_id = :groupId AND deleted_at IS NULL ORDER BY created_at DESC")
    suspend fun getByGroup(groupId: String): List<TodoEntity>

    @Query("SELECT * FROM todo_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TodoEntity?

    @Upsert
    suspend fun upsert(entity: TodoEntity)

    @Query("UPDATE todo_items SET deleted_at = :ts, updated_at = :ts, synced_at = NULL WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long)

    @Query("UPDATE todo_items SET deleted_at = :ts, updated_at = :ts, synced_at = NULL WHERE group_id = :groupId")
    suspend fun softDeleteByGroup(groupId: String, ts: Long)

    @Query("""
        SELECT * FROM todo_items 
        WHERE synced_at IS NULL OR synced_at < updated_at
        LIMIT 500
    """)
    suspend fun getDirtyRows(): List<TodoEntity>

    @Query("UPDATE todo_items SET synced_at = :ts WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, ts: Long)

    @Query("UPDATE todo_items SET status = :status, updated_at = :ts, synced_at = NULL WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, ts: Long)
}

