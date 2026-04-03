package com.example.todoit.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoit.domain.model.Schedule
import com.example.todoit.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ScheduleEditState(
    val id: String = "",
    val name: String = "",
    /** Active day-of-week flags: index 0=Mon … 6=Sun */
    val activeDayFlags: BooleanArray = BooleanArray(7) { true },
    val hasStartTime: Boolean = false,
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val hasEndTime: Boolean = false,
    val endHour: Int = 18,
    val endMinute: Int = 0,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
) {
    /** Converts activeDayFlags → domain List<Int> where 1=Mon … 7=Sun */
    fun toActiveDays(): List<Int> =
        activeDayFlags.indices.mapNotNull { idx -> if (activeDayFlags[idx]) idx + 1 else null }

    /** Minutes-since-midnight for startTime, or null. */
    fun startTimeOfDay(): Int? =
        if (hasStartTime) startHour * 60 + startMinute else null

    /** Minutes-since-midnight for endTime, or null. */
    fun endTimeOfDay(): Int? =
        if (hasEndTime) endHour * 60 + endMinute else null
}

/** Convert domain Schedule to edit state. */
fun Schedule.toEditState(): ScheduleEditState {
    val flags = BooleanArray(7)
    activeDays.forEach { day -> if (day in 1..7) flags[day - 1] = true }
    val startH = startTimeOfDay?.div(60) ?: 9
    val startM = startTimeOfDay?.rem(60) ?: 0
    val endH = endTimeOfDay?.div(60) ?: 18
    val endM = endTimeOfDay?.rem(60) ?: 0
    return ScheduleEditState(
        id = id,
        name = name,
        activeDayFlags = flags,
        hasStartTime = startTimeOfDay != null,
        startHour = startH,
        startMinute = startM,
        hasEndTime = endTimeOfDay != null,
        endHour = endH,
        endMinute = endM,
    )
}

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    /** Observed list of all schedules (for the list screen). */
    val schedules: StateFlow<List<Schedule>> =
        scheduleRepository.observeSchedules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Edit form state for create/edit dialog. */
    private val _editState = MutableStateFlow(ScheduleEditState())
    val editState: StateFlow<ScheduleEditState> = _editState.asStateFlow()

    // ── Edit state mutations ──────────────────────────────────────────────────

    fun startNew() {
        _editState.value = ScheduleEditState()
    }

    fun startEdit(schedule: Schedule) {
        _editState.value = schedule.toEditState()
    }

    fun onNameChange(v: String) = _editState.update { it.copy(name = v, error = null) }

    fun onDayToggle(index: Int) = _editState.update { s ->
        val flags = s.activeDayFlags.copyOf()
        flags[index] = !flags[index]
        s.copy(activeDayFlags = flags)
    }

    fun onStartTimeToggle(enabled: Boolean) =
        _editState.update { it.copy(hasStartTime = enabled) }

    fun onStartTimeChange(hour: Int, minute: Int) =
        _editState.update { it.copy(startHour = hour, startMinute = minute) }

    fun onEndTimeToggle(enabled: Boolean) =
        _editState.update { it.copy(hasEndTime = enabled) }

    fun onEndTimeChange(hour: Int, minute: Int) =
        _editState.update { it.copy(endHour = hour, endMinute = minute) }

    // ── Persistence ───────────────────────────────────────────────────────────

    fun save() {
        val s = _editState.value
        if (s.name.isBlank()) {
            _editState.update { it.copy(error = "Name is required") }
            return
        }
        if (s.toActiveDays().isEmpty()) {
            _editState.update { it.copy(error = "Select at least one active day") }
            return
        }
        _editState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                scheduleRepository.upsertSchedule(
                    Schedule(
                        id = s.id.ifBlank { UUID.randomUUID().toString() },
                        name = s.name.trim(),
                        activeDays = s.toActiveDays(),
                        startTimeOfDay = s.startTimeOfDay(),
                        endTimeOfDay = s.endTimeOfDay(),
                        updatedAt = System.currentTimeMillis(),
                        deletedAt = null,
                    )
                )
            }.onSuccess {
                _editState.update { it.copy(isSaving = false, isSaved = true) }
            }.onFailure { e ->
                _editState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun deleteSchedule(id: String) = viewModelScope.launch {
        runCatching { scheduleRepository.softDeleteSchedule(id) }
    }
}

