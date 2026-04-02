package com.example.todoit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "radius_meters") val radiusMeters: Float,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long?,
)

