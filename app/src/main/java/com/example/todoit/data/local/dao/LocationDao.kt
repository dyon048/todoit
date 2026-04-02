package com.example.todoit.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.todoit.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations WHERE deleted_at IS NULL ORDER BY label ASC")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): LocationEntity?

    @Upsert
    suspend fun upsert(entity: LocationEntity)

    @Query("UPDATE locations SET deleted_at = :ts, updated_at = :ts, synced_at = NULL WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long)

    @Query("SELECT * FROM locations WHERE synced_at IS NULL OR synced_at < updated_at LIMIT 500")
    suspend fun getDirtyRows(): List<LocationEntity>

    @Query("UPDATE locations SET synced_at = :ts WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>, ts: Long)
}

