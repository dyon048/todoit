package com.example.todoit.presentation.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoit.domain.model.TodoItem
import com.example.todoit.domain.model.TodoStatus
import com.example.todoit.domain.repository.GroupRepository
import com.example.todoit.domain.usecase.todo.DeleteTodoUseCase
import com.example.todoit.domain.usecase.todo.GetTodosForGroupUseCase
import com.example.todoit.domain.usecase.todo.UpdateTodoStatusUseCase
import com.example.todoit.domain.usecase.todo.UpsertTodoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GroupDetailUiState {
    data object Loading : GroupDetailUiState
    data class Success(val todos: List<TodoItem>) : GroupDetailUiState
    data class Error(val message: String) : GroupDetailUiState
}

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTodos: GetTodosForGroupUseCase,
    private val upsertTodo: UpsertTodoUseCase,
    private val deleteTodo: DeleteTodoUseCase,
    private val groupRepository: GroupRepository,
    private val updateTodoStatus: UpdateTodoStatusUseCase,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _groupName = MutableStateFlow("")
    /** The name of the current group — used as the screen title prefix. */
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    val uiState: StateFlow<GroupDetailUiState> =
        getTodos.observe(groupId)
            .map<List<TodoItem>, GroupDetailUiState> { GroupDetailUiState.Success(it) }
            .catch { emit(GroupDetailUiState.Error(it.message ?: "Unknown error")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupDetailUiState.Loading)

    init {
        viewModelScope.launch {
            val group = groupRepository.getGroupById(groupId)
            if (group != null) _groupName.value = group.name
        }
    }

    fun save(todo: TodoItem) = viewModelScope.launch { runCatching { upsertTodo(todo) } }
    fun delete(todoId: String) = viewModelScope.launch { runCatching { deleteTodo(todoId) } }
    fun toggleTodoStatus(todo: TodoItem) = viewModelScope.launch {
        runCatching {
            val newStatus = if (todo.status == TodoStatus.DONE) TodoStatus.PENDING else TodoStatus.DONE
            updateTodoStatus(todo.id, newStatus)
        }
    }
}

