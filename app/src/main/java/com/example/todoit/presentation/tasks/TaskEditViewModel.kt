package com.example.todoit.presentation.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoit.domain.model.Location
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import com.example.todoit.domain.repository.LocationRepository
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
    // ── Location ───────────────────────────────────────────────────────────────
    /** true when user has toggled "Enable location trigger" */
    val locationEnabled: Boolean = false,
    /** existing location id when editing a task that already has one */
    val locationId: String? = null,
    val locationLabel: String = "",
    val locationLat: String = "",
    val locationLng: String = "",
    /** Geofence radius in metres (100–5000) */
    val locationRadius: Float = 300f,
    // ── Save state ─────────────────────────────────────────────────────────────
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class TaskEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val locationRepository: LocationRepository,
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
                    // Load any linked location
                    val loc = task.locationId?.let { locationRepository.getLocationById(it) }
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
                            locationEnabled = loc != null,
                            locationId = loc?.id,
                            locationLabel = loc?.label ?: "",
                            locationLat = loc?.latitude?.toString() ?: "",
                            locationLng = loc?.longitude?.toString() ?: "",
                            locationRadius = loc?.radiusMeters ?: 300f,
                        )
                    }
                }
            }
        }
    }

    // ── Form handlers ─────────────────────────────────────────────────────────
    fun onTitleChange(v: String) = _form.update { it.copy(title = v, error = null) }
    fun onStatusChange(v: TaskStatus) = _form.update { it.copy(status = v) }
    fun onPriorityChange(v: Int) = _form.update { it.copy(priority = v) }
    fun onDueDateChange(v: Long) = _form.update { it.copy(dueDate = v) }
    fun onStartTimeChange(v: Long?) = _form.update { it.copy(startTime = v) }
    fun onReminderChange(v: Long?) = _form.update { it.copy(reminderAt = v) }

    fun onLocationToggle(enabled: Boolean) = _form.update { it.copy(locationEnabled = enabled) }
    fun onLocationLabelChange(v: String) = _form.update { it.copy(locationLabel = v) }
    fun onLocationLatChange(v: String) = _form.update { it.copy(locationLat = v) }
    fun onLocationLngChange(v: String) = _form.update { it.copy(locationLng = v) }
    fun onLocationRadiusChange(v: Float) = _form.update { it.copy(locationRadius = v) }

    // ── Save ─────────────────────────────────────────────────────────────────
    fun save() {
        val f = _form.value
        if (f.title.isBlank()) {
            _form.update { it.copy(error = "Title is required") }
            return
        }
        if (f.locationEnabled) {
            val lat = f.locationLat.toDoubleOrNull()
            val lng = f.locationLng.toDoubleOrNull()
            if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
                _form.update { it.copy(error = "Invalid location coordinates") }
                return
            }
        }
        _form.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val resolvedLocationId: String? = if (f.locationEnabled) {
                    val lat = f.locationLat.toDouble()
                    val lng = f.locationLng.toDouble()
                    val locId = f.locationId ?: UUID.randomUUID().toString()
                    locationRepository.upsertLocation(
                        Location(
                            id = locId,
                            label = f.locationLabel.ifBlank { "Task location" },
                            latitude = lat,
                            longitude = lng,
                            radiusMeters = f.locationRadius,
                            updatedAt = System.currentTimeMillis(),
                            deletedAt = null,
                        )
                    )
                    locId
                } else {
                    // If location was disabled on an existing task, soft-delete the old location
                    if (f.locationId != null) locationRepository.softDeleteLocation(f.locationId)
                    null
                }

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
                        locationId = resolvedLocationId,
                        scheduleId = null,
                        inheritSchedule = f.inheritSchedule,
                        recurrenceId = null,
                        recurrenceInstanceDate = null,
                        parentTaskId = null,
                        scoreCache = 0f,
                        createdAt = f.originalCreatedAt,
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
