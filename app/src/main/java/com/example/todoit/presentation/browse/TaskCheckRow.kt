package com.example.todoit.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.todoit.domain.model.Task
import com.example.todoit.domain.model.TaskStatus
import com.example.todoit.presentation.common.PriorityChip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A single task row shown when its parent todo is expanded.
 *
 * - [Checkbox] on the left immediately toggles DONE ↔ PENDING via [onToggle]
 * - Title uses strikethrough + dimmed color when DONE (clear visual completion)
 * - [PriorityChip] shows urgency at a glance
 * - Due date is highlighted red when the task is overdue AND not yet DONE
 * - Tapping the row text area (not the checkbox) calls [onEdit] to open full edit
 * - Indented via [BrowseListItem.TaskSubRow.depth] to align with sibling TodoRows
 */
@Composable
fun TaskCheckRow(
    item: BrowseListItem.TaskSubRow,
    onToggle: (Task) -> Unit,
    onEdit: (Task) -> Unit,
) {
    val task = item.task
    val isDone = task.status == TaskStatus.DONE
    val isOverdue = task.dueDate < System.currentTimeMillis() && !isDone
    val timeFmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                start = (48 + item.depth * 16).dp,
                end = 16.dp,
                top = 6.dp,
                bottom = 6.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isDone,
            onCheckedChange = { onToggle(task) },
        )
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
            color = if (isDone)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable { onEdit(task) },
        )
        Spacer(Modifier.width(6.dp))
        PriorityChip(task.priority)
        Spacer(Modifier.width(6.dp))
        Text(
            text = timeFmt.format(Date(task.dueDate)),
            style = MaterialTheme.typography.labelSmall,
            color = if (isOverdue)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
    )
}




