package com.example.sockapp.ui.screens.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sockapp.data.models.group.GroupMember
import com.example.sockapp.data.models.group.GroupRole
import com.google.firebase.Timestamp

@Composable
fun CompactMemberListItem(
    member: GroupMember,
    onClick: (memberId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(member.userId) }
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(member.photoUrl)
                .placeholder(Icons.Filled.Person) // Placeholder for loading/null URL
                .error(Icons.Filled.Person)       // Error fallback
                .crossfade(true)
                .build(),
            contentDescription = "${member.displayName ?: "User"}'s profile image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(32.dp)
                .clip(MaterialTheme.shapes.small) // Or CircleShape
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = member.displayName ?: "Unknown User",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(4.dp))
        // Optionally show a small role icon without text if needed
        RoleIcon(role = member.role, modifier = Modifier.size(12.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun CompactMemberListItemPreview() {
    MaterialTheme {
        CompactMemberListItem(
            member = GroupMember(
                userId = "user1",
                displayName = "Alice Wonderland",
                role = GroupRole.MEMBER,
                joinedAt = Timestamp.now(),
                photoUrl = null
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CompactMemberListItemAdminPreview() {
    MaterialTheme {
        CompactMemberListItem(
            member = GroupMember(
                userId = "user2",
                displayName = "Bob The Admin",
                role = GroupRole.ADMIN,
                joinedAt = Timestamp.now(),
                photoUrl = "https://example.com/bob.png"
            ),
            onClick = {}
        )
    }
}
