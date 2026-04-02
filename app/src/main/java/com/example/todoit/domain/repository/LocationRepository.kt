package com.example.todoit.domain.repository

import com.example.todoit.domain.model.Location
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocations(): Flow<List<Location>>
    suspend fun getLocationById(id: String): Location?
    suspend fun upsertLocation(location: Location)
    suspend fun softDeleteLocation(id: String)
}

