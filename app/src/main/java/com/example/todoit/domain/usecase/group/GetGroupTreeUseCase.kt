package com.example.todoit.domain.usecase.group

import com.example.todoit.domain.model.Group
import com.example.todoit.domain.model.GroupNode
import com.example.todoit.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetGroupTreeUseCase @Inject constructor(
    private val repository: GroupRepository,
) {
    /** Emits a live tree every time groups change in Room. */
    fun observe(): Flow<List<GroupNode>> =
        repository.observeGroups().map { groups -> buildTree(groups, parentId = null) }

    suspend fun getOnce(): List<GroupNode> =
        buildTree(repository.getAllGroups(), parentId = null)

    private fun buildTree(groups: List<Group>, parentId: String?): List<GroupNode> =
        groups
            .filter { it.parentId == parentId && it.deletedAt == null }
            .map { group -> GroupNode(group, buildTree(groups, group.id)) }
}

