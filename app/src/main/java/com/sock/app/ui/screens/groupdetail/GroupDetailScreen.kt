package com.sock.app.ui.screens.groupdetail

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sock.app.model.AvailabilityStatus
import com.sock.app.model.GroupMemberSummary
import com.sock.app.ui.components.statusColor
import com.sock.app.ui.theme.SockTheme

@Composable
fun GroupDetailScreen(
    groupName: String,
    groupStatus: AvailabilityStatus,
    members: List<GroupMemberSummary>,
    onUpdateStatus: () -> Unit,
    onRevertToGlobal: () -> Unit,
    onMemberSelected: (GroupMemberSummary) -> Unit,
    onManageGroup: () -> Unit,
    onLeaveGroup: () -> Unit,
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
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your status in this group",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        tonalElevation = 1.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = groupStatus.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = groupStatus.statusColor()
                            )
                            Text(
                                text = groupStatus.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onUpdateStatus) {
                                Text(text = "Update status")
                            }
                            OutlinedButton(onClick = onRevertToGlobal) {
                                Text(text = "Revert to global status")
                            }
                        }
                    }

                    Button(onClick = onManageGroup) {
                        Text(text = "Open group settings")
                    }

                    OutlinedButton(onClick = onLeaveGroup) {
                        Text(text = "Leave group")
                    }
                }
            }
        }

        item {
            Text(
                text = "Members",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(members, key = { it.id }) { member ->
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = member.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "@${member.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = member.status.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = member.status.statusColor()
                    )
                    if (member.isAdmin) {
                        Text(
                            text = "Admin",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(onClick = { onMemberSelected(member) }) {
                        Text(text = "View profile")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GroupDetailScreenPreview() {
    SockTheme {
        GroupDetailScreen(
            groupName = "Roommates",
            groupStatus = AvailabilityStatus.BUSY_ANYONE_CAN_JOIN,
            members = listOf(
                GroupMemberSummary(
                    id = "1",
                    displayName = "Alex Chen",
                    username = "alex",
                    status = AvailabilityStatus.OPEN_TO_HANGOUT,
                    isAdmin = true
                ),
                GroupMemberSummary(
                    id = "2",
                    displayName = "Sam Patel",
                    username = "sam",
                    status = AvailabilityStatus.WORKING
                )
            ),
            onUpdateStatus = {},
            onRevertToGlobal = {},
            onMemberSelected = {},
            onManageGroup = {},
            onLeaveGroup = {}
        )
    }
}
