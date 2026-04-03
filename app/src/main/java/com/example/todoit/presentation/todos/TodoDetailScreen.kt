package com.example.todoit.presentation.todos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.todoit.domain.model.Task
import com.example.todoit.presentation.common.ConfirmDeleteDialog
import com.example.todoit.presentation.common.EmptyStateView
import com.example.todoit.presentation.common.PriorityChip
import com.example.todoit.presentation.common.Screen
import com.example.todoit.presentation.common.StatusChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailScreen(
    todoId: String,
    navController: NavHostController,
    viewModel: TodoDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todoTitle by viewModel.todoTitle.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (todoTitle.isBlank()) "Tasks" else "$todoTitle Tasks") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.taskEdit(todoId)) }) {
                Icon(Icons.Default.Add, contentDescription = "New task")
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is TodoDetailUiState.Loading -> {}
            is TodoDetailUiState.Error -> EmptyStateView(title = "Error", subtitle = state.message)
            is TodoDetailUiState.Success ->
                if (state.tasks.isEmpty()) {
                    EmptyStateView(
                        title = "No tasks yet",
                        subtitle = "Tap + to add one.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    LazyColumn(contentPadding = padding) {
                        items(state.tasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onEdit = { navController.navigate(Screen.taskEdit(todoId, task.id)) },
                                onDelete = { deleteTarget = task },
                            )
                        }
                    }
                }
        }
    }

    deleteTarget?.let { task ->
        ConfirmDeleteDialog(
            title = "Delete \"${task.title}\"?",
            onConfirm = { viewModel.delete(task.id); deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun TaskCard(task: Task, onEdit: () -> Unit, onDelete: () -> Unit) {
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    Card(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(task.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete task")
                }
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                PriorityChip(task.priority)
                Spacer(Modifier.width(8.dp))
                StatusChip(task.status)
                Spacer(Modifier.weight(1f))
                Text(
                    fmt.format(Date(task.dueDate)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

