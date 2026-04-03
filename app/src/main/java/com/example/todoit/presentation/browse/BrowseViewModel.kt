package com.example.todoit.presentation.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoit.domain.model.GroupNode
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import com.example.todoit.domain.model.TodoItem
import com.example.todoit.domain.usecase.group.GetGroupTreeUseCase
import com.example.todoit.domain.usecase.task.GetTasksForTodoUseCase
import com.example.todoit.domain.usecase.task.UpsertTaskUseCase
import com.example.todoit.domain.usecase.todo.GetTodosForGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val getGroupTree: GetGroupTreeUseCase,
    private val getTodosForGroup: GetTodosForGroupUseCase,
    private val getTasksForTodo: GetTasksForTodoUseCase,
    private val upsertTask: UpsertTaskUseCase,
) : ViewModel() {

    /** IDs of groups whose todo list is currently visible. */
    private val _expandedGroupIds = MutableStateFlow<Set<String>>(emptySet())

    /** IDs of todos whose task list is currently visible. */
    private val _expandedTodoIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<BrowseUiState> =
        combine(
            getGroupTree.observe(),
            _expandedGroupIds,
            _expandedTodoIds,
        ) { tree, groupIds, todoIds -> Triple(tree, groupIds, todoIds) }
            .transformLatest { (tree, groupIds, todoIds) ->
                val allGroupIds = flattenGroupIds(tree)

                if (allGroupIds.isEmpty()) {
                    emit(BrowseUiState.Success(emptyList()))
                    return@transformLatest
                }

                // Always load todos for ALL groups so we can show count badges
                // even on collapsed groups.
                val todoMapFlow: Flow<Map<String, List<TodoItem>>> =
                    combine(
                        allGroupIds.map { gId ->
                            getTodosForGroup.observe(gId).map { todos -> gId to todos }
                        }
                    ) { pairs -> pairs.associate { it } }

                // Reactively derive task flows from the current todo map.
                // The inner transformLatest also emits BrowseUiState so that
                // the outer collector's type is consistent (avoids Any inference).
                todoMapFlow.transformLatest { todoMap ->
                    val expandedTodos: List<TodoItem> =
                        groupIds.flatMap { gId -> todoMap[gId] ?: emptyList() }

                    if (expandedTodos.isEmpty()) {
                        emit(
                            BrowseUiState.Success(
                                buildFlatList(tree, groupIds, todoIds, todoMap, emptyMap())
                            )
                        )
                        return@transformLatest
                    }

                    val taskMapFlow: Flow<Map<String, List<Task>>> =
                        combine(
                            expandedTodos.map { todo ->
                                getTasksForTodo.observe(todo.id).map { tasks -> todo.id to tasks }
                            }
                        ) { pairs -> pairs.associate { it } }

                    taskMapFlow.collect { taskMap ->
                        emit(
                            BrowseUiState.Success(
                                buildFlatList(tree, groupIds, todoIds, todoMap, taskMap)
                            )
                        )
                    }
                }.collect { state: BrowseUiState -> emit(state) }
            }
            .catch { emit(BrowseUiState.Error(it.message ?: "Unknown error")) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                BrowseUiState.Loading,
            )

    // -------------------------------------------------------------------------
    // User actions
    // -------------------------------------------------------------------------

    fun toggleGroup(groupId: String) {
        _expandedGroupIds.update { current ->
            if (groupId in current) current - groupId else current + groupId
        }
    }

    fun toggleTodo(todoId: String) {
        _expandedTodoIds.update { current ->
            if (todoId in current) current - todoId else current + todoId
        }
    }

    /**
     * Immediately flips a task between [TaskStatus.DONE] and [TaskStatus.PENDING].
     * Uses [UpsertTaskUseCase] so reminders and recurrence expansions are handled
     * the same way as in the TaskEditScreen.
     */
    fun toggleTaskDone(task: Task) = viewModelScope.launch {
        runCatching {
            val newStatus = if (task.status == TaskStatus.DONE) TaskStatus.PENDING else TaskStatus.DONE
            upsertTask(task.copy(status = newStatus))
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun flattenGroupIds(nodes: List<GroupNode>): List<String> =
        nodes.flatMap { listOf(it.group.id) + flattenGroupIds(it.children) }

    /**
     * Builds the ordered flat list that drives the LazyColumn.
     *
     * Order: root groups depth-first → todos when group expanded → tasks when todo expanded.
     * Task sort: PENDING/IN_PROGRESS first, DONE last, each bucket sorted by dueDate ASC.
     */
    private fun buildFlatList(
        tree: List<GroupNode>,
        expandedGroupIds: Set<String>,
        expandedTodoIds: Set<String>,
        todosByGroup: Map<String, List<TodoItem>>,
        tasksByTodo: Map<String, List<Task>>,
    ): List<BrowseListItem> {
        val items = mutableListOf<BrowseListItem>()

        fun processNode(node: GroupNode, depth: Int) {
            val groupId = node.group.id
            val isExpanded = groupId in expandedGroupIds
            val todos = todosByGroup[groupId] ?: emptyList()

            items += BrowseListItem.GroupHeader(
                node = node,
                isExpanded = isExpanded,
                todoCount = todos.size,
                depth = depth,
            )

            if (isExpanded) {
                for (todo in todos) {
                    val tasks = tasksByTodo[todo.id] ?: emptyList()
                    val doneCount = tasks.count { it.status == TaskStatus.DONE }
                    val isTodoExpanded = todo.id in expandedTodoIds

                    items += BrowseListItem.TodoSubRow(
                        todo = todo,
                        isExpanded = isTodoExpanded,
                        doneCount = doneCount,
                        totalCount = tasks.size,
                        depth = depth,
                    )

                    if (isTodoExpanded && tasks.isNotEmpty()) {
                        val sorted = tasks.sortedWith(
                            compareBy<Task> { if (it.status == TaskStatus.DONE) 1 else 0 }
                                .thenBy { it.dueDate }
                        )
                        for (task in sorted) {
                            items += BrowseListItem.TaskSubRow(task = task, depth = depth)
                        }
                    }
                }
            }

            for (child in node.children) {
                processNode(child, depth + 1)
            }
        }

        for (node in tree) {
            processNode(node, 0)
        }

        return items
    }
}
