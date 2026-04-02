package com.example.todoit.presentation.common

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun PriorityChip(priority: Int) {
    val (label, color) = when (priority) {
        1 -> "Critical" to Color(0xFFB00020)
        2 -> "High"     to Color(0xFFE65100)
        3 -> "Medium"   to Color(0xFFF9A825)
        else -> "Low"   to Color(0xFF388E3C)
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

