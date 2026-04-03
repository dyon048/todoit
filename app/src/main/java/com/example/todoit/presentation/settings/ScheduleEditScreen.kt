package com.example.todoit.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.todoit.domain.repository.ScheduleRepository
import com.example.todoit.presentation.common.TimePickerModal
import javax.inject.Inject

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditScreen(
    scheduleId: String?,
    navController: NavHostController,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()

    // Load existing schedule or start fresh
    LaunchedEffect(scheduleId) {
        if (!scheduleId.isNullOrBlank()) {
            val existing = schedules.find { it.id == scheduleId }
            if (existing != null) viewModel.startEdit(existing)
        } else {
            viewModel.startNew()
        }
    }

    // Navigate back after save
    LaunchedEffect(editState.isSaved) {
        if (editState.isSaved) navController.popBackStack()
    }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker   by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (scheduleId.isNullOrBlank()) "New Schedule" else "Edit Schedule") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.save() }) {
                Icon(Icons.Default.Check, contentDescription = "Save")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Name ───────────────────────────────────────────────────────
            OutlinedTextField(
                value = editState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Schedule name *") },
                isError = editState.error != null,
                supportingText = editState.error?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            HorizontalDivider()

            // ── Active days ────────────────────────────────────────────────
            Text("Active days", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DAY_LABELS.forEachIndexed { index, label ->
                    FilterChip(
                        selected = editState.activeDayFlags[index],
                        onClick = { viewModel.onDayToggle(index) },
                        label = { Text(label) },
                    )
                }
            }

            HorizontalDivider()

            // ── Start time ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Start time",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = editState.hasStartTime,
                    onCheckedChange = viewModel::onStartTimeToggle,
                )
            }
            if (editState.hasStartTime) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "%02d:%02d".format(editState.startHour, editState.startMinute),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showStartTimePicker = true }) { Text("Change") }
                }
            }

            // ── End time ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "End time",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = editState.hasEndTime,
                    onCheckedChange = viewModel::onEndTimeToggle,
                )
            }
            if (editState.hasEndTime) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "%02d:%02d".format(editState.endHour, editState.endMinute),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showEndTimePicker = true }) { Text("Change") }
                }
            }

            Spacer(Modifier.height(72.dp)) // FAB clearance
        }
    }

    if (showStartTimePicker) {
        TimePickerModal(
            initialHour   = editState.startHour,
            initialMinute = editState.startMinute,
            onTimeSelected = { h, m ->
                viewModel.onStartTimeChange(h, m)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false },
        )
    }

    if (showEndTimePicker) {
        TimePickerModal(
            initialHour   = editState.endHour,
            initialMinute = editState.endMinute,
            onTimeSelected = { h, m ->
                viewModel.onEndTimeChange(h, m)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false },
        )
    }
}

