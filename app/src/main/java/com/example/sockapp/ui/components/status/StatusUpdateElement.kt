package com.example.sockapp.ui.components.status

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sockapp.data.models.UserStatusType
import com.example.sockapp.ui.theme.AppIcons // Assuming AppIcons is in this package

@Composable
fun StatusUpdateElement(
    currentStatusType: UserStatusType,
    currentCustomText: String?, // The actual custom message text
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium) // Clip the clickable area
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium, // Apply shape for visual consistency if using elevation/border
        tonalElevation = 1.dp, // Subtle elevation to indicate it's a distinct element
        // border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) // Alternative to elevation
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // Pushes StatusView and Icon to opposite ends
        ) {
            StatusView(
                statusType = currentStatusType,
                customText = currentCustomText,
                // textStyle = MaterialTheme.typography.bodyMedium // Optionally override text style
            )
            Icon(
                imageVector = AppIcons.EditStatus, // Using the general edit icon
                contentDescription = "Edit status",
                tint = MaterialTheme.colorScheme.primary // Make the icon noticeable
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun StatusUpdateElementOnlinePreview() {
    MaterialTheme {
        StatusUpdateElement(
            currentStatusType = UserStatusType.Online,
            currentCustomText = null,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun StatusUpdateElementCustomPreview() {
    MaterialTheme {
        StatusUpdateElement(
            currentStatusType = UserStatusType.Custom,
            currentCustomText = "Planning the next big thing!",
            onClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun StatusUpdateElementBusyPreview() {
    MaterialTheme {
        StatusUpdateElement(
            currentStatusType = UserStatusType.Busy,
            currentCustomText = null,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun StatusUpdateElementOfflinePreview() {
    MaterialTheme {
        StatusUpdateElement(
            currentStatusType = UserStatusType.Offline,
            currentCustomText = null,
            onClick = {}
        )
    }
}
