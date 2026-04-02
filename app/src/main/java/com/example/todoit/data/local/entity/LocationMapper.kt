package com.example.todoit.data.local.entity

import com.example.todoit.domain.model.Location

fun LocationEntity.toDomain() = Location(
    id = id,
    label = label,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun Location.toEntity(syncedAt: Long? = null) = LocationEntity(
    id = id,
    label = label,
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncedAt = syncedAt,
)

