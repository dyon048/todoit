package com.example.todoit.domain.model

data class Location(
    val id: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    /** Geofence radius in metres; minimum 100m recommended */
    val radiusMeters: Float,
    val updatedAt: Long,
    val deletedAt: Long?,
)

