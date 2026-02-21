package com.example.sockapp.ui.screens.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.sockapp.data.models.group.Group

@Composable
fun GroupListItem(
    group: Group,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(group.profileImageUrl ?: Icons.Default.Group) // Fallback to default icon
                    .crossfade(true)
                    .build(),
                contentDescription = "${group.name} profile image",
                placeholder = Icons.Default.Group, // Fallback if image is null or loading
                error = Icons.Default.Group, // Fallback on error
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = group.description ?: "No description available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (group.isPublic) Icons.Filled.Public else Icons.Filled.Lock,
                        contentDescription = if (group.isPublic) "Public Group" else "Private Group",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${group.memberCount} member${if (group.memberCount != 1L) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GroupListItemPreviewPublic() {
    MaterialTheme {
        GroupListItem(
            group = Group(
                groupId = "1",
                name = "Awesome Public Group",
                description = "This is a really cool group that you should definitely join. We talk about all sorts of interesting topics and have fun doing it!",
                profileImageUrl = null, // Replace with a sample URL for better preview
                memberCount = 125,
                isPublic = true
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GroupListItemPreviewPrivate() {
    MaterialTheme {
        GroupListItem(
            group = Group(
                groupId = "2",
                name = "Super Secret Private Club",
                description = "Shhh... it's a secret. Only cool people allowed.",
                profileImageUrl = null,
                memberCount = 12,
                isPublic = false
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GroupListItemPreviewNoDescription() {
    MaterialTheme {
        GroupListItem(
            group = Group(
                groupId = "3",
                name = "Mysterious Group",
                description = null,
                profileImageUrl = null,
                memberCount = 5,
                isPublic = true
            ),
            onClick = {}
        )
    }
}
