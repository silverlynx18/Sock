package com.example.sockapp.ui.screens.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sockapp.data.models.group.GroupMember
import com.example.sockapp.data.models.group.GroupRole
import com.google.firebase.Timestamp

@Composable
fun GroupMemberDisplayInfo(
    member: GroupMember,
    modifier: Modifier = Modifier,
    imageSize: Dp = 24.dp,
    onClick: (() -> Unit)? = null // Optional click action
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(member.photoUrl)
                .placeholder(Icons.Filled.AccountCircle) // Placeholder for loading/null URL
                .error(Icons.Filled.AccountCircle)       // Error fallback
                .crossfade(true)
                .build(),
            contentDescription = "${member.displayName ?: "User"}'s profile image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(imageSize)
                .clip(MaterialTheme.shapes.small) // Or CircleShape if preferred
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = member.displayName ?: "Unknown User",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GroupMemberDisplayInfoPreview() {
    MaterialTheme {
        GroupMemberDisplayInfo(
            member = GroupMember(
                userId = "user1",
                displayName = "John Doe Very Long Name To Test Ellipsis",
                role = GroupRole.MEMBER,
                joinedAt = Timestamp.now(),
                photoUrl = null // Test with null photoUrl
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GroupMemberDisplayInfoSmallPreview() {
    MaterialTheme {
        GroupMemberDisplayInfo(
            member = GroupMember(
                userId = "user2",
                displayName = "Jane Smith",
                role = GroupRole.ADMIN,
                joinedAt = Timestamp.now(),
                photoUrl = "https://example.com/image.png" // Dummy URL
            ),
            imageSize = 32.dp // Slightly larger image
        )
    }
}
