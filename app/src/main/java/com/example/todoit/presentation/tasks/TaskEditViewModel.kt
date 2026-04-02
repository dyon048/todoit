package com.example.todoit.presentation.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import com.example.todoit.domain.repository.TaskRepository
import com.example.todoit.domain.usecase.task.UpsertTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TaskEditFormState(
    val id: String = "",
    val todoId: String = "",
    val title: String = "",
    val status: TaskStatus = TaskStatus.PENDING,
    val priority: Int = 3,
    val dueDate: Long = System.currentTimeMillis(),
    val startTime: Long? = null,
    val reminderAt: Long? = null,
    val inheritSchedule: Boolean = true,
    /** 0L = new task (UpsertTaskUseCase will assign createdAt); non-zero = original task timestamp. */
    val originalCreatedAt: Long = 0L,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val upsertTask: UpsertTaskUseCase,
) : ViewModel() {

    private val taskId: String? = savedStateHandle["taskId"]
    private val todoId: String = checkNotNull(savedStateHandle["todoId"])

    private val _form = MutableStateFlow(TaskEditFormState(todoId = todoId))
    val form: StateFlow<TaskEditFormState> = _form.asStateFlow()

    init {
        if (!taskId.isNullOrBlank()) {
            viewModelScope.launch {
                val task = taskRepository.getTaskById(taskId)
                if (task != null) {
                    _form.update {
                        it.copy(
                            id = task.id,
                            todoId = task.todoId,
                            title = task.title,
                            status = task.status,
                            priority = task.priority,
                            dueDate = task.dueDate,
                            startTime = task.startTime,
                            reminderAt = task.reminderAt,
                            inheritSchedule = task.inheritSchedule,
                            originalCreatedAt = task.createdAt,
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(v: String) = _form.update { it.copy(title = v, error = null) }
    fun onStatusChange(v: TaskStatus) = _form.update { it.copy(status = v) }
    fun onPriorityChange(v: Int) = _form.update { it.copy(priority = v) }
    fun onDueDateChange(v: Long) = _form.update { it.copy(dueDate = v) }
    fun onStartTimeChange(v: Long?) = _form.update { it.copy(startTime = v) }
    fun onReminderChange(v: Long?) = _form.update { it.copy(reminderAt = v) }

    fun save() {
        val f = _form.value
        if (f.title.isBlank()) {
            _form.update { it.copy(error = "Title is required") }
            return
        }
        _form.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                upsertTask(
                    Task(
                        id = f.id.ifBlank { UUID.randomUUID().toString() },
                        todoId = f.todoId,
                        title = f.title.trim(),
                        status = f.status,
                        priority = f.priority,
                        dueDate = f.dueDate,
                        startTime = f.startTime,
                        reminderAt = f.reminderAt,
                        locationId = null,
                        scheduleId = null,
                        inheritSchedule = f.inheritSchedule,
                        recurrenceId = null,
                        recurrenceInstanceDate = null,
                        parentTaskId = null,
                        scoreCache = 0f,
                        createdAt = f.originalCreatedAt, // 0L for new tasks → UpsertTaskUseCase sets now
                        updatedAt = System.currentTimeMillis(),
                        deletedAt = null,
                    )
                )
            }.onSuccess {
                _form.update { it.copy(isSaving = false, isSaved = true) }
            }.onFailure { e ->
                _form.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}

