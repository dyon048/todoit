package com.example.todoit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.todoit.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules WHERE deleted_at IS NULL ORDER BY name ASC")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE deleted_at IS NULL ORDER BY name ASC")
    suspend fun getAll(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ScheduleEntity?

    @Upsert
    suspend fun upsert(entity: ScheduleEntity)

    @Query("UPDATE schedules SET deleted_at = :ts, updated_at = :ts, synced_at = NULL WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long)

    @Query("SELECT * FROM schedules WHERE synced_at IS NULL OR synced_at < updated_at LIMIT 500")
    suspend fun getDirtyRows(): List<ScheduleEntity>

    @Query("UPDATE schedules SET synced_at = :ts WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, ts: Long)
}

