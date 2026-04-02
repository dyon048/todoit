package com.example.todoit.data.repository

import com.example.todoit.data.local.dao.GroupDao
import com.example.todoit.data.local.entity.toDomain
import com.example.todoit.data.local.entity.toEntity
import com.example.todoit.data.sync.SyncScheduler
import com.example.todoit.domain.model.Group
import com.example.todoit.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val dao: GroupDao,
    private val syncScheduler: SyncScheduler,
) : GroupRepository {

    override fun observeGroups(): Flow<List<Group>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllGroups(): List<Group> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getGroupById(id: String): Group? =
        dao.getById(id)?.toDomain()

    override suspend fun upsertGroup(group: Group) {
        val now = System.currentTimeMillis()
        dao.upsert(group.copy(updatedAt = now).toEntity(syncedAt = null))
        syncScheduler.triggerImmediateSync()
    }

    override suspend fun softDeleteGroup(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
        syncScheduler.triggerImmediateSync()
    }
}
