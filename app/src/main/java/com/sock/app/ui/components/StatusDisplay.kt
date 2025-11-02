package com.sock.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sock.app.data.model.StatusType

@Composable
fun StatusDisplay(
    status: StatusType?,
    modifier: Modifier = Modifier
) {
    if (status == null) {
        Text(
            text = "No status set",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = getStatusIcon(status),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = status.displayText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

fun getStatusIcon(status: StatusType): ImageVector {
    return when (status) {
        StatusType.OPEN_TO_HANGOUT -> Icons.Default.Group
        StatusType.BUSY -> Icons.Default.EventBusy
        StatusType.GOING_THROUGH_IT -> Icons.Default.MoodBad
        StatusType.BUSY_ANYONE_CAN_JOIN -> Icons.Default.Groups
        StatusType.WORKING -> Icons.Default.Work
        StatusType.DO_NOT_DISTURB -> Icons.Default.NotificationsOff
        StatusType.DO_NOT_APPROACH -> Icons.Default.Block
    }
}
