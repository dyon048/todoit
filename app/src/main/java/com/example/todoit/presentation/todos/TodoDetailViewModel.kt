package com.example.todoit.presentation.todos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.usecase.task.DeleteTaskUseCase
import com.example.todoit.domain.usecase.task.GetTasksForTodoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
) : ViewModel() {

    val todoId: String = checkNotNull(savedStateHandle["todoId"])

    val uiState: StateFlow<TodoDetailUiState> =
        getTasks.observe(todoId)
            .map<List<Task>, TodoDetailUiState> { TodoDetailUiState.Success(it) }
            .catch { emit(TodoDetailUiState.Error(it.message ?: "Unknown error")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodoDetailUiState.Loading)

    fun delete(taskId: String) = viewModelScope.launch { runCatching { deleteTask(taskId) } }
}

