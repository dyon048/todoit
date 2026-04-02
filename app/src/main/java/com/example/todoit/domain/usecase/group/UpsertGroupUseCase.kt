package com.example.todoit.domain.usecase.group

import com.example.todoit.domain.model.Group
import com.example.todoit.domain.repository.GroupRepository
import java.util.UUID
import javax.inject.Inject

class UpsertGroupUseCase @Inject constructor(
    private val repository: GroupRepository,
) {
    /** Creates a new group or updates an existing one.
     *  Validates no circular parentId reference before saving. */
    suspend operator fun invoke(group: Group) {
        val id = group.id.ifBlank { UUID.randomUUID().toString() }
        val now = System.currentTimeMillis()
        val toSave = group.copy(
            id = id,
            createdAt = if (group.createdAt == 0L) now else group.createdAt,
            updatedAt = now,
        )
        // Guard: reject if parentId would create a cycle
        if (toSave.parentId != null) {
            requireNoCycle(toSave.id, toSave.parentId, repository)
        }
        repository.upsertGroup(toSave)
    }

    private suspend fun requireNoCycle(
        targetId: String,
        parentId: String,
        repository: GroupRepository,
    ) {
        var current: String? = parentId
        val visited = mutableSetOf<String>()
        while (current != null) {
            check(current !in visited) { "Circular group reference detected" }
            if (current == targetId) error("Group cannot be its own ancestor")
            visited += current
            current = repository.getGroupById(current)?.parentId
        }
    }
}

