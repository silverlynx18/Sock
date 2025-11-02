package com.sock.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sock.app.model.AvailabilityStatus
import com.sock.app.model.GroupSummary
import com.sock.app.ui.components.GlobalStatusCard
import com.sock.app.ui.components.GroupCard
import com.sock.app.ui.theme.SockTheme

@Composable
fun DashboardScreen(
    globalStatus: AvailabilityStatus,
    groups: List<GroupSummary>,
    onUpdateGlobalStatus: () -> Unit,
    onGroupSelected: (GroupSummary) -> Unit,
    onManageGroups: () -> Unit,
    onViewInvitations: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlobalStatusCard(
            status = globalStatus,
            onUpdateStatusClick = onUpdateGlobalStatus,
            modifier = Modifier.fillMaxWidth()
        )

        Surface(
            tonalElevation = 2.dp,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Groups",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(onClick = onManageGroups) {
                    Text(text = "Manage groups")
                }

                Button(onClick = onViewInvitations) {
                    Text(text = "View invitations")
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (groups.isEmpty()) {
                item {
                    EmptyState(onCreateGroup = onManageGroups)
                }
            } else {
                items(groups, key = { it.id }) { group ->
                    GroupCard(
                        summary = group,
                        onClick = onGroupSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onCreateGroup: () -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "No groups yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Create a group to start sharing availability with your circles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onCreateGroup) {
                Text(text = "Create a group")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun DashboardScreenPreview() {
    SockTheme {
        DashboardScreen(
            globalStatus = AvailabilityStatus.OPEN_TO_HANGOUT,
            groups = listOf(
                GroupSummary(
                    id = "1",
                    name = "Roommates",
                    memberCount = 4,
                    primaryColor = 0xFF6750A4,
                    secondaryColor = 0xFF24005A,
                    status = AvailabilityStatus.OPEN_TO_HANGOUT
                ),
                GroupSummary(
                    id = "2",
                    name = "Book Club",
                    memberCount = 8,
                    primaryColor = 0xFF386A20,
                    secondaryColor = 0xFF1B370C,
                    status = AvailabilityStatus.WORKING
                )
            ),
            onUpdateGlobalStatus = {},
            onGroupSelected = {},
            onManageGroups = {},
            onViewInvitations = {}
        )
    }
}
