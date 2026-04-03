package com.example.todoit.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.todoit.domain.model.Schedule
import com.example.todoit.presentation.common.EmptyStateView
import com.example.todoit.presentation.common.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    navController: NavHostController,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedules") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.SCHEDULE_EDIT) }) {
                Icon(Icons.Default.Add, contentDescription = "New schedule")
            }
        },
    ) { padding ->
        if (schedules.isEmpty()) {
            EmptyStateView(
                title = "No schedules yet",
                subtitle = "Tap + to create a schedule. Assign it to a group or task to filter what shows in 'What To Do Now'.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(schedules, key = { it.id }) { schedule ->
                    ScheduleRow(
                        schedule = schedule,
                        onEdit = { navController.navigate(Screen.scheduleEdit(schedule.id)) },
                        onDelete = { viewModel.deleteSchedule(schedule.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    schedule: Schedule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(schedule.name, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                formatScheduleSummary(schedule),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun formatScheduleSummary(schedule: Schedule): String {
    val days = schedule.activeDays.sorted().joinToString(", ") { d ->
        DAY_LABELS.getOrNull(d - 1) ?: d.toString()
    }
    val time = when {
        schedule.startTimeOfDay != null && schedule.endTimeOfDay != null ->
            " · ${schedule.startTimeOfDay.toHhMm()}–${schedule.endTimeOfDay.toHhMm()}"
        schedule.startTimeOfDay != null -> " · from ${schedule.startTimeOfDay.toHhMm()}"
        else -> ""
    }
    return "$days$time"
}

private fun Int.toHhMm(): String = "%02d:%02d".format(this / 60, this % 60)

