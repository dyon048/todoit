package com.example.todoit.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoit.data.location.FusedLocationProvider
import com.example.todoit.data.location.GeofenceManager
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
    private val locationProvider: FusedLocationProvider,
    private val geofenceManager: GeofenceManager,
) : ViewModel() {

    /** Observed from Room, sorted by scoreCache DESC. */
    val uiState: StateFlow<HomeUiState> =
        taskRepository.observeAllActiveSortedByScore()
            .map<List<Task>, HomeUiState> { tasks ->
                HomeUiState.Success(tasks.filter { it.status != TaskStatus.DONE && it.scoreCache >= 0f })
            }
            .catch { emit(HomeUiState.Error(it.message ?: "Unknown error")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private var userLat: Double? = null
    private var userLng: Double? = null

    init {
        refreshScores()
        // Collect passive location updates and re-score on each new fix
        viewModelScope.launch {
            locationProvider.locationUpdates.collect { (lat, lng) ->
                refreshScores(lat, lng)
            }
        }
        // Re-register geofences whenever the task list changes
        viewModelScope.launch {
            taskRepository.observeAllActiveSortedByScore().collect { tasks ->
                geofenceManager.registerGeofencesForActiveTasks(tasks)
            }
        }
    }

    /** Starts passive location updates. Call after location permission is granted. */
    fun startLocationUpdates() = locationProvider.startPassiveUpdates()

    fun refreshScores(lat: Double? = null, lng: Double? = null) {
        if (lat != null) userLat = lat
        if (lng != null) userLng = lng
        viewModelScope.launch {
            runCatching { refreshScoreCache(userLat, userLng) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationProvider.stopUpdates()
    }
}

