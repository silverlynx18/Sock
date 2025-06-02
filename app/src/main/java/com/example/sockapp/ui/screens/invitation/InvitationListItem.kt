package com.example.sockapp.ui.screens.invitation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MailOutline // Generic invite icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sockapp.data.models.invitation.Invitation
import com.example.sockapp.data.models.invitation.InvitationStatus
import com.example.sockapp.data.models.group.GroupRole
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun InvitationListItem(
    invitation: Invitation,
    isLoading: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onClick: () -> Unit, // To view details on InvitationAcceptanceScreen
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(invitation.groupImageUrl ?: invitation.inviterPhotoUrl) // Show group image, fallback to inviter
                        .placeholder(Icons.Default.MailOutline)
                        .error(Icons.Default.MailOutline)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Invitation image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invitation.groupName ?: "A Group Invitation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Invited by: ${invitation.inviterName ?: "Someone"}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Role: ${invitation.roleToAssign.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    invitation.createdAt.let {
                        Text(
                            text = "Received: ${formatTimestamp(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (invitation.status == InvitationStatus.PENDING) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        OutlinedButton(onClick = onDecline,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.Clear, contentDescription = "Decline", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Decline")
                        }
                        Button(onClick = onAccept) {
                            Icon(Icons.Filled.Check, contentDescription = "Accept", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Accept")
                        }
                    }
                }
            } else {
                Text(
                    "Status: ${invitation.status.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = when(invitation.status) {
                        InvitationStatus.ACCEPTED -> MaterialTheme.colorScheme.primary
                        InvitationStatus.DECLINED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp): String {
    return try {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(timestamp.toDate())
    } catch (e: Exception) {
        "Unknown date"
    }
}

@Preview(showBackground = true)
@Composable
fun InvitationListItemPreview_Pending() {
    MaterialTheme {
        InvitationListItem(
            invitation = Invitation(
                invitationId = "1",
                groupName = "The Adventurers Guild",
                groupImageUrl = null, // Test with null
                inviterName = "Guild Master Bob",
                inviterPhotoUrl = null,
                roleToAssign = GroupRole.MEMBER,
                status = InvitationStatus.PENDING,
                createdAt = Timestamp.now()
            ),
            isLoading = false,
            onAccept = {},
            onDecline = {},
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InvitationListItemPreview_Accepted() {
    MaterialTheme {
        InvitationListItem(
            invitation = Invitation(
                invitationId = "2",
                groupName = "Book Club",
                inviterName = "Alice",
                roleToAssign = GroupRole.MODERATOR,
                status = InvitationStatus.ACCEPTED,
                createdAt = Timestamp.now()
            ),
            isLoading = false,
            onAccept = {},
            onDecline = {},
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InvitationListItemPreview_Loading() {
    MaterialTheme {
        InvitationListItem(
            invitation = Invitation(
                invitationId = "3",
                groupName = "Gaming Squad",
                inviterName = "PlayerOne",
                status = InvitationStatus.PENDING,
                createdAt = Timestamp.now()
            ),
            isLoading = true,
            onAccept = {},
            onDecline = {},
            onClick = {}
        )
    }
}
