package com.example.todoit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.todoit.data.local.entity.RecurrenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceDao {

    @Query("SELECT * FROM recurrences ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<RecurrenceEntity>>

    @Query("SELECT * FROM recurrences WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurrenceEntity?

    @Upsert
    suspend fun upsert(entity: RecurrenceEntity)

    @Query("DELETE FROM recurrences WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM recurrences WHERE synced_at IS NULL OR synced_at < updated_at LIMIT 500")
    suspend fun getDirtyRows(): List<RecurrenceEntity>

    @Query("UPDATE recurrences SET synced_at = :ts WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, ts: Long)
}

