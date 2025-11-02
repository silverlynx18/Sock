package com.sock.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sock.app.model.AvailabilityStatus
import com.sock.app.model.GroupSummary
import com.sock.app.ui.theme.SockTheme

@Composable
fun GroupCard(
    summary: GroupSummary,
    onClick: (GroupSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(summary) },
        colors = CardDefaults.cardColors(
            containerColor = Color(summary.primaryColor).copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = summary.status.statusIcon(),
                contentDescription = "${summary.name} status",
                tint = summary.status.statusColor(),
                modifier = Modifier.size(28.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${summary.memberCount} members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = summary.status.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = summary.status.statusColor()
                )
                Text(
                    text = "Update status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview
@Composable
private fun GroupCardPreview() {
    SockTheme {
        GroupCard(
            summary = GroupSummary(
                id = "demo",
                name = "Roommates",
                memberCount = 4,
                primaryColor = 0xFF6750A4,
                secondaryColor = 0xFF24005A,
                status = AvailabilityStatus.OPEN_TO_HANGOUT
            ),
            onClick = {}
        )
    }
}
