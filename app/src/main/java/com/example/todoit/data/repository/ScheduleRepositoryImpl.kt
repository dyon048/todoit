package com.example.todoit.data.repository

import com.example.todoit.data.local.dao.ScheduleDao
import com.example.todoit.data.local.entity.toDomain
import com.example.todoit.data.local.entity.toEntity
import com.example.todoit.domain.model.Schedule
import com.example.todoit.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ScheduleRepositoryImpl @Inject constructor(
    private val dao: ScheduleDao,
) : ScheduleRepository {

    override fun observeSchedules(): Flow<List<Schedule>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllSchedules(): List<Schedule> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getScheduleById(id: String): Schedule? =
        dao.getById(id)?.toDomain()

    override suspend fun upsertSchedule(schedule: Schedule) {
        val now = System.currentTimeMillis()
        dao.upsert(schedule.copy(updatedAt = now).toEntity(syncedAt = null))
    }

    override suspend fun softDeleteSchedule(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
    }
}

