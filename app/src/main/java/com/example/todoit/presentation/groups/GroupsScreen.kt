package com.example.todoit.presentation.groups

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.todoit.domain.model.Group
import com.example.todoit.domain.model.GroupNode
import com.example.todoit.presentation.common.EmptyStateView
import com.example.todoit.presentation.common.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    navController: NavHostController,
    viewModel: GroupsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Group?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Groups") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editTarget = null; showEditDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New group")
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is GroupsUiState.Loading -> {}
            is GroupsUiState.Error -> EmptyStateView(title = "Error", subtitle = state.message)
            is GroupsUiState.Success ->
                if (state.tree.isEmpty()) {
                    EmptyStateView(
                        title = "No groups yet",
                        subtitle = "Tap + to create your first group.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    GroupTreeList(
                        nodes = state.tree,
                        modifier = Modifier.padding(padding),
                        onTap = { navController.navigate(Screen.groupDetail(it.group.id)) },
                        onEdit = { editTarget = it.group; showEditDialog = true },
                        onDelete = { viewModel.delete(it.group.id) },
                    )
                }
        }
    }

    if (showEditDialog) {
        GroupEditDialog(
            initial = editTarget,
            onSave = { viewModel.save(it); showEditDialog = false },
            onDismiss = { showEditDialog = false },
        )
    }
}

