package com.example.todoit.domain.model

data class GroupNode(
    val group: Group,
    val children: List<GroupNode> = emptyList(),
)

