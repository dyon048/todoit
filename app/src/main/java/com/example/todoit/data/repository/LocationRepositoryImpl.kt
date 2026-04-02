package com.example.todoit.data.repository

import com.example.todoit.data.local.dao.LocationDao
import com.example.todoit.data.local.entity.toDomain
import com.example.todoit.data.local.entity.toEntity
import com.example.todoit.domain.model.Location
import com.example.todoit.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocationRepositoryImpl @Inject constructor(
    private val dao: LocationDao,
) : LocationRepository {

    override fun observeLocations(): Flow<List<Location>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getLocationById(id: String): Location? =
        dao.getById(id)?.toDomain()

    override suspend fun upsertLocation(location: Location) {
        val now = System.currentTimeMillis()
        dao.upsert(location.copy(updatedAt = now).toEntity(syncedAt = null))
    }

    override suspend fun softDeleteLocation(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
    }
}

