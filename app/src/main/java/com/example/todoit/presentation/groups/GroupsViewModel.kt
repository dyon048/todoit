package com.example.todoit.presentation.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoit.domain.model.Group
import com.example.todoit.domain.model.GroupNode
import com.example.todoit.domain.usecase.group.DeleteGroupUseCase
import com.example.todoit.domain.usecase.group.GetGroupTreeUseCase
import com.example.todoit.domain.usecase.group.UpsertGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GroupsUiState {
    data object Loading : GroupsUiState
    data class Success(val tree: List<GroupNode>) : GroupsUiState
    data class Error(val message: String) : GroupsUiState
}

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val getGroupTree: GetGroupTreeUseCase,
    private val upsertGroup: UpsertGroupUseCase,
    private val deleteGroup: DeleteGroupUseCase,
) : ViewModel() {

    val uiState: StateFlow<GroupsUiState> =
        getGroupTree.observe()
            .map<List<GroupNode>, GroupsUiState> { GroupsUiState.Success(it) }
            .catch { emit(GroupsUiState.Error(it.message ?: "Unknown error")) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupsUiState.Loading)

    fun save(group: Group) = viewModelScope.launch {
        runCatching { upsertGroup(group) }
    }

    fun delete(groupId: String) = viewModelScope.launch {
        runCatching { deleteGroup(groupId) }
    }
}

