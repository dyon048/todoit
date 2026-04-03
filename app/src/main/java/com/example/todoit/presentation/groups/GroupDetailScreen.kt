package com.example.todoit.presentation.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.todoit.domain.model.TodoItem
import com.example.todoit.presentation.common.ConfirmDeleteDialog
import com.example.todoit.presentation.common.EmptyStateView
import com.example.todoit.presentation.common.Screen
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    navController: NavHostController,
    viewModel: GroupDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groupName by viewModel.groupName.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<TodoItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (groupName.isBlank()) "Todos" else "$groupName Todos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New todo")
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is GroupDetailUiState.Loading -> {}
            is GroupDetailUiState.Error -> EmptyStateView(title = "Error", subtitle = state.message)
            is GroupDetailUiState.Success ->
                if (state.todos.isEmpty()) {
                    EmptyStateView(
                        title = "No todos yet",
                        subtitle = "Tap + to add one.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyColumn(contentPadding = padding) {
                        items(state.todos, key = { it.id }) { todo ->
                            ListItem(
                                headlineContent = { Text(todo.title) },
                                supportingContent = todo.description?.let { { Text(it) } },
                                trailingContent = {
                                    IconButton(onClick = { deleteTarget = todo }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate(Screen.todoDetail(todo.id))
                                    },
                            )
                        }
                    }
                }
        }
    }

    if (showAddDialog) {
        TodoEditDialog(
            groupId = groupId,
            initial = null,
            onSave = { viewModel.save(it); showAddDialog = false },
            onDismiss = { showAddDialog = false },
        )
    }

    deleteTarget?.let { todo ->
        ConfirmDeleteDialog(
            title = "Delete \"${todo.title}\"?",
            message = "All tasks inside will also be deleted.",
            onConfirm = { viewModel.delete(todo.id); deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun TodoEditDialog(
    groupId: String,
    initial: TodoItem?,
    onSave: (TodoItem) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Todo" else "Edit Todo") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val now = System.currentTimeMillis()
                    onSave(
                        TodoItem(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            groupId = groupId,
                            title = title.trim(),
                            description = description.trim().ifBlank { null },
                            createdAt = initial?.createdAt ?: now,
                            updatedAt = now,
                            deletedAt = null,
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
