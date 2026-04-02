package com.example.todoit.presentation.common

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.todoit.domain.model.TaskStatus

@Composable
fun StatusChip(status: TaskStatus) {
    val (label, color) = when (status) {
        TaskStatus.PENDING     -> "Pending"     to Color(0xFF546E7A)
        TaskStatus.IN_PROGRESS -> "In Progress" to Color(0xFF1565C0)
        TaskStatus.DONE        -> "Done"        to Color(0xFF2E7D32)
    }
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = color,
        ),
    )
}

