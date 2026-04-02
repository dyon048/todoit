package com.example.todoit.presentation.groups

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.todoit.domain.model.GroupNode
import com.example.todoit.presentation.common.ConfirmDeleteDialog

@Composable
fun GroupTreeList(
    nodes: List<GroupNode>,
    modifier: Modifier = Modifier,
    onTap: (GroupNode) -> Unit,
    onEdit: (GroupNode) -> Unit,
    onDelete: (GroupNode) -> Unit,
    depth: Int = 0,
) {
    LazyColumn(modifier = modifier) {
        items(nodes, key = { it.group.id }) { node ->
            GroupNodeRow(
                node = node,
                depth = depth,
                onTap = onTap,
                onEdit = onEdit,
                onDelete = onDelete,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupNodeRow(
    node: GroupNode,
    depth: Int,
    onTap: (GroupNode) -> Unit,
    onEdit: (GroupNode) -> Unit,
    onDelete: (GroupNode) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onTap(node) },
                    onLongClick = { showMenu = true },
                )
                .padding(start = (16 + depth * 16).dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(node.group.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            if (node.children.isNotEmpty()) {
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit(node) })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; showDeleteDialog = true })
            }
        }
        node.children.forEach { child ->
            GroupNodeRow(node = child, depth = depth + 1, onTap = onTap, onEdit = onEdit, onDelete = onDelete)
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Delete \"${node.group.name}\"?",
            message = "This will delete the group and all its contents.",
            onConfirm = { showDeleteDialog = false; onDelete(node) },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

