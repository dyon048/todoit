package com.example.todoit.data.repository

import com.example.todoit.data.local.dao.RecurrenceDao
import com.example.todoit.data.local.entity.toDomain
import com.example.todoit.data.local.entity.toEntity
import com.example.todoit.domain.model.Recurrence
import com.example.todoit.domain.repository.RecurrenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecurrenceRepositoryImpl @Inject constructor(
    private val dao: RecurrenceDao,
) : RecurrenceRepository {

    override fun observeRecurrences(): Flow<List<Recurrence>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getRecurrenceById(id: String): Recurrence? =
        dao.getById(id)?.toDomain()

    override suspend fun upsertRecurrence(recurrence: Recurrence) {
        dao.upsert(recurrence.toEntity(syncedAt = null))
    }

    override suspend fun deleteRecurrence(id: String) {
        dao.deleteById(id)
    }
}

