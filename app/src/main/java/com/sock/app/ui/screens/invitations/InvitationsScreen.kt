package com.sock.app.ui.screens.invitations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.sock.app.model.InvitationSummary
import com.sock.app.ui.theme.SockTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InvitationsScreen(
    invitations: List<InvitationSummary>,
    onAccept: (InvitationSummary) -> Unit,
    onDecline: (InvitationSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    if (invitations.isEmpty()) {
        Surface(
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.large,
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "No pending invitations",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Invite links from friends will appear here once they reach you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(invitations, key = { it.id }) { invitation ->
            InvitationCard(
                invitation = invitation,
                onAccept = { onAccept(invitation) },
                onDecline = { onDecline(invitation) }
            )
        }
    }
}

@Composable
private fun InvitationCard(
    invitation: InvitationSummary,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = rememberInvitationFormatter()

    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "You've been invited to ${invitation.groupName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Invited by ${invitation.inviterName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Sent ${formatter.format(invitation.sentAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAccept) {
                Text(text = "Accept invitation")
            }
            OutlinedButton(onClick = onDecline) {
                Text(text = "Decline")
            }
        }
    }
}

@Composable
private fun rememberInvitationFormatter(): DateTimeFormatter {
    return DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
        .withZone(ZoneId.systemDefault())
}

@Preview(showBackground = true)
@Composable
private fun InvitationsScreenPreview() {
    SockTheme {
        InvitationsScreen(
            invitations = listOf(
                InvitationSummary(
                    id = "1",
                    groupName = "Game Night",
                    inviterName = "Jordan",
                    sentAt = Instant.now()
                )
            ),
            onAccept = {},
            onDecline = {}
        )
    }
}
