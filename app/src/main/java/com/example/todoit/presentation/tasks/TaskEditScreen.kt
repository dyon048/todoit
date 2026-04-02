package com.example.todoit.presentation.tasks

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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.todoit.domain.model.TaskStatus
import com.example.todoit.presentation.common.DatePickerModal
import com.example.todoit.presentation.common.TimePickerModal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    todoId: String,
    taskId: String?,
    navController: NavHostController,
    viewModel: TaskEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Navigate back when saved
    LaunchedEffect(form.isSaved) {
        if (form.isSaved) navController.popBackStack()
    }

    var showDueDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "New Task" else "Edit Task") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.save() },
                content = { Icon(Icons.Default.Check, contentDescription = "Save") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title
            OutlinedTextField(
                value = form.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Title *") },
                isError = form.error != null,
                supportingText = form.error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Status
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = it },
            ) {
                OutlinedTextField(
                    value = form.status.name.replace('_', ' '),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Status") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                    TaskStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.name.replace('_', ' ')) },
                            onClick = { viewModel.onStatusChange(status); statusExpanded = false },
                        )
                    }
                }
            }

            // Priority
            Text("Priority", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "Critical", 2 to "High", 3 to "Medium", 4 to "Low").forEach { (value, label) ->
                    FilterChip(
                        selected = form.priority == value,
                        onClick = { viewModel.onPriorityChange(value) },
                        label = { Text(label) },
                    )
                }
            }

            // Due date
            Text("Due Date", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateFmt.format(Date(form.dueDate)),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    label = { Text("Date") },
                )
                TextButton(onClick = { showDueDatePicker = true }) { Text("Pick") }
            }

            // Start time (optional)
            Text("Start Time (optional)", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.startTime?.let { timeFmt.format(Date(it)) } ?: "—",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    label = { Text("Time") },
                )
                TextButton(onClick = { showStartTimePicker = true }) { Text("Pick") }
                if (form.startTime != null) {
                    TextButton(onClick = { viewModel.onStartTimeChange(null) }) { Text("Clear") }
                }
            }

            // Reminder (optional)
            Text("Reminder (optional)", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.reminderAt?.let { timeFmt.format(Date(it)) } ?: "—",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    label = { Text("Reminder time") },
                )
                TextButton(onClick = { showReminderPicker = true }) { Text("Pick") }
                if (form.reminderAt != null) {
                    TextButton(onClick = { viewModel.onReminderChange(null) }) { Text("Clear") }
                }
            }

            Spacer(Modifier.height(72.dp)) // FAB clearance
        }
    }

    // Date picker modal
    if (showDueDatePicker) {
        DatePickerModal(
            initialDateMs = form.dueDate,
            onDateSelected = { viewModel.onDueDateChange(it); showDueDatePicker = false },
            onDismiss = { showDueDatePicker = false },
        )
    }

    // Start time picker modal
    if (showStartTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = form.startTime ?: form.dueDate }
        TimePickerModal(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            onTimeSelected = { h, m ->
                val base = Calendar.getInstance().apply { timeInMillis = form.dueDate }
                base.set(Calendar.HOUR_OF_DAY, h); base.set(Calendar.MINUTE, m); base.set(Calendar.SECOND, 0)
                viewModel.onStartTimeChange(base.timeInMillis)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false },
        )
    }

    // Reminder time picker modal
    if (showReminderPicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = form.reminderAt ?: form.dueDate }
        TimePickerModal(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            onTimeSelected = { h, m ->
                val base = Calendar.getInstance().apply { timeInMillis = form.dueDate }
                base.set(Calendar.HOUR_OF_DAY, h); base.set(Calendar.MINUTE, m); base.set(Calendar.SECOND, 0)
                viewModel.onReminderChange(base.timeInMillis)
                showReminderPicker = false
            },
            onDismiss = { showReminderPicker = false },
        )
    }
}


