package com.example.todoit.domain.repository

import com.example.todoit.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

interface ScheduleRepository {
    fun observeSchedules(): Flow<List<Schedule>>
    suspend fun getAllSchedules(): List<Schedule>
    suspend fun getScheduleById(id: String): Schedule?
    suspend fun upsertSchedule(schedule: Schedule)
    suspend fun softDeleteSchedule(id: String)
}

