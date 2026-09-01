package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.TaskFilterTab

@Composable
fun EmptyStateView(
    filterTab: TaskFilterTab,
    isSearchActive: Boolean,
    onAddNewTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when {
        isSearchActive -> "No matching tasks"
        filterTab == TaskFilterTab.COMPLETED -> "No completed tasks yet"
        filterTab == TaskFilterTab.UPCOMING -> "No upcoming tasks"
        else -> "All caught up for today!"
    }

    val subtitle = when {
        isSearchActive -> "Try searching for a different keyword or clear the search filter."
        filterTab == TaskFilterTab.COMPLETED -> "Tasks you complete will be archived here."
        filterTab == TaskFilterTab.UPCOMING -> "Plan ahead by scheduling tasks for the coming days."
        else -> "You have no pending tasks. Tap the button below to add a new task or enjoy your free time!"
    }

    val icon = when {
        isSearchActive -> Icons.Default.SearchOff
        else -> Icons.Default.AssignmentTurnedIn
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textAlign = TextAlign.Center
        )

        if (!isSearchActive && filterTab != TaskFilterTab.COMPLETED) {
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAddNewTask,
                modifier = Modifier.testTag("empty_state_add_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Add Task")
            }
        }
    }
}
