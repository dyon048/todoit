package com.example.todoit.presentation.todos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.repository.TodoRepository
import com.example.todoit.domain.usecase.task.DeleteTaskUseCase
import com.example.todoit.domain.usecase.task.GetTasksForTodoUseCase
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

sealed interface TodoDetailUiState {
    data object Loading : TodoDetailUiState
    data class Success(val tasks: List<Task>) : TodoDetailUiState
    data class Error(val message: String) : TodoDetailUiState
}

@HiltViewModel
class TodoDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTasks: GetTasksForTodoUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val todoRepository: TodoRepository,
) : ViewModel() {

    val todoId: String = checkNotNull(savedStateHandle["todoId"])

    private val _todoTitle = MutableStateFlow("")
    /** The title of the current todo — used as the screen title prefix. */
    val todoTitle: StateFlow<String> = _todoTitle.asStateFlow()

    val uiState: StateFlow<TodoDetailUiState> =
        getTasks.observe(todoId)
            .map<List<Task>, TodoDetailUiState> { TodoDetailUiState.Success(it) }
            .catch { emit(TodoDetailUiState.Error(it.message ?: "Unknown error")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoDetailUiState.Loading)

    init {
        viewModelScope.launch {
            val todo = todoRepository.getTodoById(todoId)
            if (todo != null) _todoTitle.value = todo.title
        }
    }

    fun delete(taskId: String) = viewModelScope.launch { runCatching { deleteTask(taskId) } }
}

