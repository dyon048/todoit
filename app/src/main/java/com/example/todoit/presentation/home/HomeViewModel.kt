package com.example.todoit.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import com.example.todoit.domain.repository.TaskRepository
import com.example.todoit.domain.usecase.task.RefreshScoreCacheUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val tasks: List<Task>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val refreshScoreCache: RefreshScoreCacheUseCase,
) : ViewModel() {

    /** Observed from Room, sorted by scoreCache DESC. */
    val uiState: StateFlow<HomeUiState> =
        taskRepository.observeAllActiveSortedByScore()
            .map<List<Task>, HomeUiState> { tasks ->
                HomeUiState.Success(tasks.filter { it.status != TaskStatus.DONE && it.scoreCache >= 0f })
            }
            .catch { emit(HomeUiState.Error(it.message ?: "Unknown error")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    // Current user location (updated from passive FusedLocation in Phase 4)
    private var userLat: Double? = null
    private var userLng: Double? = null

    init {
        refreshScores()
    }

    fun refreshScores(lat: Double? = null, lng: Double? = null) {
        if (lat != null) userLat = lat
        if (lng != null) userLng = lng
        viewModelScope.launch {
            runCatching { refreshScoreCache(userLat, userLng) }
        }
    }
}
