package com.example.todoit.domain.repository

import com.example.todoit.domain.model.Recurrence
import kotlinx.coroutines.flow.Flow

interface RecurrenceRepository {
    fun observeRecurrences(): Flow<List<Recurrence>>
    suspend fun getRecurrenceById(id: String): Recurrence?
    suspend fun upsertRecurrence(recurrence: Recurrence)
    suspend fun deleteRecurrence(id: String)
}

