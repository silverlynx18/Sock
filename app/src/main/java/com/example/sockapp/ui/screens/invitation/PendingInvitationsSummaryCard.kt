package com.example.sockapp.ui.screens.invitation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ForwardToInbox // Or Mail, Markunread
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sockapp.data.models.invitation.Invitation

@Composable
fun PendingInvitationsSummaryCard(
    pendingInvitations: List<Invitation>, // Pass the list of pending invitations
    onViewAllInvitationsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (pendingInvitations.isEmpty()) {
        // Optionally show nothing or a "No pending invitations" message if desired for a summary card
        // For a card, usually it's just not shown if empty.
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onViewAllInvitationsClicked),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ForwardToInbox,
                    contentDescription = "Pending Invitations",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pending Invitations",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${pendingInvitations.size} invitation${if (pendingInvitations.size != 1) "s" else ""} waiting for you.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "View all invitations",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PendingInvitationsSummaryCardPreview_SomeInvites() {
    MaterialTheme {
        PendingInvitationsSummaryCard(
            pendingInvitations = listOf(
                Invitation(invitationId = "1"),
                Invitation(invitationId = "2")
            ),
            onViewAllInvitationsClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PendingInvitationsSummaryCardPreview_OneInvite() {
    MaterialTheme {
        PendingInvitationsSummaryCard(
            pendingInvitations = listOf(Invitation(invitationId = "1")),
            onViewAllInvitationsClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PendingInvitationsSummaryCardPreview_NoInvites() {
    // The component currently returns early if pendingInvitations is empty,
    // so this preview will show nothing, which is the intended behavior for this component.
    MaterialTheme {
        PendingInvitationsSummaryCard(
            pendingInvitations = emptyList(),
            onViewAllInvitationsClicked = {}
        )
    }
}
