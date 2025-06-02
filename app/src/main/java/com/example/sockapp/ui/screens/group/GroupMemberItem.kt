package com.example.sockapp.ui.screens.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sockapp.data.models.group.GroupMember
import com.example.sockapp.data.models.group.GroupRole
import com.google.firebase.Timestamp

@Composable
fun GroupMemberItem(
    member: GroupMember,
    currentUserRole: GroupRole?, // Role of the user viewing this item
    currentUserId: String?, // UID of the user viewing this item
    onRemoveMember: (memberId: String) -> Unit,
    onChangeRole: (memberId: String, newRole: GroupRole) -> Unit,
    onViewProfile: (memberId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isSelf = member.userId == currentUserId

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(member.photoUrl)
                        .placeholder(Icons.Filled.AdminPanelSettings) // Generic user icon
                        .error(Icons.Filled.AdminPanelSettings)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${member.displayName ?: "User"}'s profile image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        member.displayName ?: "Unknown User",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RoleIcon(role = member.role, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            member.role.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (currentUserRole != null && !isSelf && currentUserRole.canRemoveMember(member.role, currentUserRole == GroupRole.OWNER)) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Member options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (currentUserRole.canRemoveMember(member.role, currentUserRole == GroupRole.OWNER)) {
                             DropdownMenuItem(
                                text = { Text("Remove Member", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    onRemoveMember(member.userId)
                                    showMenu = false
                                }
                            )
                        }
                        // Promotion options
                        GroupRole.entries.forEach { potentialNewRole ->
                            if (member.role != potentialNewRole && currentUserRole.canPromoteTo(potentialNewRole) && potentialNewRole != GroupRole.OWNER) { // Cannot promote to owner directly
                                DropdownMenuItem(
                                    text = { Text("Promote to ${potentialNewRole.displayName}") },
                                    onClick = {
                                        onChangeRole(member.userId, potentialNewRole)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                        // Demotion options (simplified, assumes demotion is to one step down or to member)
                        if (member.role != GroupRole.MEMBER && currentUserRole.canDemoteFrom(member.role)) {
                             val targetDemotionRole = GroupRole.entries.find { it.level == member.role.level -1 } ?: GroupRole.MEMBER
                             if(targetDemotionRole.level < member.role.level) { // ensure it's a demotion
                                 DropdownMenuItem(
                                    text = { Text("Demote to ${targetDemotionRole.displayName}") },
                                    onClick = {
                                        onChangeRole(member.userId, targetDemotionRole)
                                        showMenu = false
                                    }
                                )
                             }
                        }
                         DropdownMenuItem(
                            text = { Text("View Profile") }, // Basic action
                            onClick = {
                                onViewProfile(member.userId)
                                showMenu = false
                            }
                        )
                    }
                }
            } else if (isSelf) {
                // Potentially show a "You" chip or similar
                Text("You", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun RoleIcon(role: GroupRole, modifier: Modifier = Modifier) {
    val icon: ImageVector = when (role) {
        GroupRole.OWNER -> Icons.Filled.Star
        GroupRole.ADMIN -> Icons.Filled.Shield // Or AdminPanelSettings
        GroupRole.MODERATOR -> Icons.Filled.AdminPanelSettings // Or a gavel icon
        GroupRole.MEMBER -> Icons.Filled.AdminPanelSettings // Or a generic person icon
    }
    val tint = when (role) {
        GroupRole.OWNER -> Color(0xFFFFD700) // Gold
        GroupRole.ADMIN -> MaterialTheme.colorScheme.secondary
        GroupRole.MODERATOR -> MaterialTheme.colorScheme.tertiary
        GroupRole.MEMBER -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Icon(imageVector = icon, contentDescription = role.displayName, modifier = modifier, tint = tint)
}

@Preview(showBackground = true)
@Composable
fun GroupMemberItemPreview_AdminViewMember() {
    MaterialTheme {
        GroupMemberItem(
            member = GroupMember(userId = "user123", displayName = "Alice Smith", role = GroupRole.MEMBER, joinedAt = Timestamp.now()),
            currentUserRole = GroupRole.ADMIN,
            currentUserId = "adminUserId",
            onRemoveMember = {},
            onChangeRole = { _, _ -> },
            onViewProfile = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GroupMemberItemPreview_OwnerViewAdmin() {
    MaterialTheme {
        GroupMemberItem(
            member = GroupMember(userId = "admin1", displayName = "Bob Johnson", role = GroupRole.ADMIN, joinedAt = Timestamp.now()),
            currentUserRole = GroupRole.OWNER,
            currentUserId = "ownerUserId",
            onRemoveMember = {},
            onChangeRole = { _, _ -> },
            onViewProfile = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GroupMemberItemPreview_MemberViewSelf() {
    MaterialTheme {
        GroupMemberItem(
            member = GroupMember(userId = "currentUser", displayName = "You", role = GroupRole.MEMBER, joinedAt = Timestamp.now()),
            currentUserRole = GroupRole.MEMBER,
            currentUserId = "currentUser",
            onRemoveMember = {},
            onChangeRole = { _, _ -> },
            onViewProfile = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GroupMemberItemPreview_MemberViewOtherMember() {
    MaterialTheme {
        GroupMemberItem(
            member = GroupMember(userId = "otherUser", displayName = "Charlie Brown", role = GroupRole.MEMBER, joinedAt = Timestamp.now()),
            currentUserRole = GroupRole.MEMBER,
            currentUserId = "currentUser",
            onRemoveMember = {},
            onChangeRole = { _, _ -> },
            onViewProfile = {} // No menu should appear
        )
    }
}
