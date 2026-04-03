package com.example.todoit.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.todoit.domain.model.Task
import com.example.todoit.presentation.common.AppPermissionsHandler
import com.example.todoit.presentation.common.EmptyStateView
import com.example.todoit.presentation.common.PriorityChip
import com.example.todoit.presentation.common.Screen
import com.example.todoit.presentation.common.StatusChip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Request location + notification permissions; start passive updates on grant
    AppPermissionsHandler(onLocationGranted = { viewModel.startLocationUpdates() })

    Scaffold(
        topBar = { TopAppBar(title = { Text("What To Do Now") }) }
    ) { padding ->
        when (val state = uiState) {
            is HomeUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            is HomeUiState.Error ->
                EmptyStateView(title = "Error", subtitle = state.message)

            is HomeUiState.Success ->
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        scope.launch {
                            isRefreshing = true
                            viewModel.refreshScores()
                            delay(600)
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier.padding(padding),
                ) {
                    if (state.tasks.isEmpty()) {
                        EmptyStateView(
                            title = "All clear! 🎉",
                            subtitle = "No active tasks right now.",
                        )
                    } else {
                        LazyColumn {
                            items(state.tasks, key = { it.id }) { task ->
                                TaskSummaryCard(
                                    task = task,
                                    onClick = { navController.navigate(Screen.taskEdit(task.todoId, task.id)) },
                                )
                            }
                        }
                    }
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskSummaryCard(task: Task, onClick: () -> Unit) {
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val isOverdue = task.dueDate < System.currentTimeMillis()
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (task.scoreCache > 0f) {
                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            task.scoreCache.toInt().toString(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
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
                    color = if (isOverdue) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
