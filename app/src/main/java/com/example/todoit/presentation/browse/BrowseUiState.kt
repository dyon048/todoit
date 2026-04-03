package com.example.todoit.presentation.browse

import com.example.todoit.domain.model.GroupNode
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TodoItem

/**
 * A flat list item used in the single [LazyColumn] of BrowseScreen.
 *
 * Using a flat sealed class avoids nested scrollable containers (which crash at runtime)
 * and gives full recycling across all three hierarchy levels.
 */
sealed class BrowseListItem {
    /** Unique key used for [LazyColumn] stable item identity and [animateItem]. */
    abstract val stableKey: String

    /**
     * A group node row. [depth] drives start-padding for nested sub-groups.
     */
    data class GroupHeader(
        val node: GroupNode,
        val isExpanded: Boolean,
        /** Total non-deleted todos in this group (shown as a badge). */
        val todoCount: Int,
        /** Nesting depth — 0 for root groups, +1 for each sub-group level. */
        val depth: Int,
    ) : BrowseListItem() {
        override val stableKey = "group_${node.group.id}"
    }

    /**
     * A todo sub-row shown when its parent group is expanded.
     * [doneCount] and [totalCount] drive the progress indicator.
     */
    data class TodoSubRow(
        val todo: TodoItem,
        val isExpanded: Boolean,
        val doneCount: Int,
        val totalCount: Int,
        /** Copied from the parent group's depth to compute indentation. */
        val depth: Int,
    ) : BrowseListItem() {
        override val stableKey = "todo_${todo.id}"
    }

    /**
     * A task sub-row shown when its parent todo is expanded.
     * Contains the task itself; the checkbox drives an immediate DONE toggle.
     */
    data class TaskSubRow(
        val task: Task,
        /** Copied from the grandparent group's depth. */
        val depth: Int,
    ) : BrowseListItem() {
        override val stableKey = "task_${task.id}"
    }
}

sealed interface BrowseUiState {
    data object Loading : BrowseUiState
    data class Success(val items: List<BrowseListItem>) : BrowseUiState
    data class Error(val message: String) : BrowseUiState
}

