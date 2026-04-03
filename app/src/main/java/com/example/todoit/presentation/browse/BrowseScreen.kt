package com.example.todoit.presentation.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.todoit.presentation.common.EmptyStateView
import com.example.todoit.presentation.common.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Top-level "Browse" screen — a 3-level expandable accordion:
 *
 * ```
 * Group (fold/unfold)
 *   └─ Todo item (fold/unfold + progress bar)
 *        └─ Task (checkbox to toggle DONE, tap to edit)
 * ```
 *
 * Uses a single flat [LazyColumn] whose items are driven by [BrowseViewModel.uiState].
 * The ViewModel flattens the entire Group → Todo → Task hierarchy into
 * [BrowseListItem] sealed class instances so there are no nested scrollable
 * containers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    navController: NavHostController,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Browse") }) },
    ) { padding ->
        when (val state = uiState) {
            is BrowseUiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            is BrowseUiState.Error ->
                EmptyStateView(
                    title = "Something went wrong",
                    subtitle = state.message,
                    modifier = Modifier.padding(padding),
                )

            is BrowseUiState.Success ->
                if (state.items.isEmpty()) {
                    EmptyStateView(
                        title = "No groups yet",
                        subtitle = "Create a group first, then add todos and tasks.",
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            scope.launch {
                                isRefreshing = true
                                delay(400)
                                isRefreshing = false
                            }
                        },
                        modifier = Modifier.padding(padding),
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(
                                items = state.items,
                                key = { it.stableKey },
                            ) { listItem ->
                                when (listItem) {
                                    is BrowseListItem.GroupHeader ->
                                        GroupRow(
                                            item = listItem,
                                            onClick = { viewModel.toggleGroup(listItem.node.group.id) },
                                        )

                                    is BrowseListItem.TodoSubRow ->
                                        TodoRow(
                                            item = listItem,
                                            onClick = { viewModel.toggleTodo(listItem.todo.id) },
                                        )

                                    is BrowseListItem.TaskSubRow ->
                                        TaskCheckRow(
                                            item = listItem,
                                            onToggle = { viewModel.toggleTaskDone(it) },
                                            onEdit = {
                                                navController.navigate(
                                                    Screen.taskEdit(it.todoId, it.id)
                                                )
                                            },
                                        )
                                }
                            }
                        }
                    }
                }
        }
    }
}

