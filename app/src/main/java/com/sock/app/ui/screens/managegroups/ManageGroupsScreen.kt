package com.sock.app.ui.screens.managegroups

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
import com.sock.app.ui.components.GroupCard
import com.sock.app.ui.theme.SockTheme

@Composable
fun ManageGroupsScreen(
    managedGroups: List<GroupSummary>,
    memberGroups: List<GroupSummary>,
    pendingInvites: Int,
    onCreateGroup: () -> Unit,
    onGroupSelected: (GroupSummary) -> Unit,
    onViewInvitations: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Pending invitations",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (pendingInvites > 0) {
                            "You have $pendingInvites awaiting your response."
                        } else {
                            "No pending invitations right now."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onViewInvitations, enabled = pendingInvites > 0) {
                        Text(text = "Review invitations")
                    }
                }
            }
        }

        item {
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Create a new group",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose group colors, name, and invite your trusted circle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onCreateGroup) {
                        Text(text = "Start group setup")
                    }
                }
            }
        }

        if (managedGroups.isNotEmpty()) {
            item {
                Text(
                    text = "Managed by you",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(managedGroups, key = { it.id }) { group ->
                GroupCard(
                    summary = group,
                    onClick = onGroupSelected
                )
            }
        }

        if (memberGroups.isNotEmpty()) {
            item {
                Text(
                    text = "Member of",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(memberGroups, key = { it.id }) { group ->
                GroupCard(
                    summary = group,
                    onClick = onGroupSelected
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ManageGroupsScreenPreview() {
    SockTheme {
        ManageGroupsScreen(
            managedGroups = listOf(
                GroupSummary(
                    id = "1",
                    name = "Household",
                    memberCount = 5,
                    primaryColor = 0xFF625B71,
                    secondaryColor = 0xFF312E38,
                    status = AvailabilityStatus.BUSY
                )
            ),
            memberGroups = listOf(
                GroupSummary(
                    id = "2",
                    name = "Climbing Crew",
                    memberCount = 7,
                    primaryColor = 0xFF386A20,
                    secondaryColor = 0xFF1B370C,
                    status = AvailabilityStatus.OPEN_TO_HANGOUT
                )
            ),
            pendingInvites = 2,
            onCreateGroup = {},
            onGroupSelected = {},
            onViewInvitations = {}
        )
    }
}
