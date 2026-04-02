package com.example.todoit.domain.repository

import com.example.todoit.domain.model.Group
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun observeGroups(): Flow<List<Group>>
    suspend fun getAllGroups(): List<Group>
    suspend fun getGroupById(id: String): Group?
    suspend fun upsertGroup(group: Group)
    suspend fun softDeleteGroup(id: String)
}

