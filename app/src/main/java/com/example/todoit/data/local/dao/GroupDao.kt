package com.example.todoit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.todoit.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM groups WHERE deleted_at IS NULL ORDER BY name ASC")
    fun observeAll(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE deleted_at IS NULL ORDER BY name ASC")
    suspend fun getAll(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GroupEntity?

    @Upsert
    suspend fun upsert(entity: GroupEntity)

    @Query("UPDATE groups SET deleted_at = :ts, updated_at = :ts, synced_at = NULL WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long)

    @Query("""
        SELECT * FROM groups 
        WHERE synced_at IS NULL OR synced_at < updated_at
        LIMIT 500
    """)
    suspend fun getDirtyRows(): List<GroupEntity>

    @Query("UPDATE groups SET synced_at = :ts WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, ts: Long)
}

