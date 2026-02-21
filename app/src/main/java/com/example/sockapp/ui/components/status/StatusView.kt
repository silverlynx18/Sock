package com.example.sockapp.ui.components.status

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline // Generic custom icon placeholder
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.sockapp.data.models.userstatus.AppPresetStatus // Updated import
import com.google.firebase.Timestamp

// Helper to map icon keys to ImageVectors.
// In a real app, this would be more robust, perhaps loading icons dynamically or having a larger predefined map.
fun getIconForKey(iconKey: String?): ImageVector {
    // This is a placeholder. You'd have a map or resource lookup.
    // For now, just returning a generic icon if a key is provided.
    return when (iconKey?.lowercase()) {
        "coffee" -> Icons.Filled.Coffee // Example, assuming Coffee icon exists
        "work" -> Icons.Filled.Work // Example
        // Add more mappings
        else -> Icons.Filled.HelpOutline // Fallback for unknown keys
    }
}


@Composable
fun StatusView(
    // Input parameters based on resolved status data
    activeStatusId: String?, // Can be an AppPresetStatus.id or a key for a UserGeneratedStatusPreset
    statusText: String?,    // The actual text to display, could be from AppPresetStatus, UserGeneratedPreset, or ad-hoc custom
    iconKey: String?,       // Key for a custom icon, or null if using AppPresetStatus icon
    expiresAt: Timestamp?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    defaultStatusWhenExpired: AppPresetStatus = AppPresetStatus.Offline // What to show if status is expired
) {
    val currentTime = Timestamp.now()
    val isExpired = expiresAt != null && expiresAt.toDate().before(currentTime.toDate())

    val displayStatus: AppPresetStatus
    val finalText: String?
    val finalIcon: ImageVector
    val finalColor: Color

    if (isExpired) {
        displayStatus = defaultStatusWhenExpired
        finalText = displayStatus.displayName
        finalIcon = displayStatus.icon
        finalColor = displayStatus.color
    } else {
        // Try to resolve activeStatusId to an AppPresetStatus
        val appPreset = AppPresetStatus.fromId(activeStatusId)

        if (appPreset != AppPresetStatus.Unknown && appPreset != AppPresetStatus.ShowingCustomMessage) {
            // It's a direct AppPresetStatus (Online, Busy, Away, Offline)
            displayStatus = appPreset
            finalText = if (!statusText.isNullOrBlank() && appPreset == AppPresetStatus.Online) {
                // If Online and custom text is provided, it acts like a subtle note alongside "Online"
                // This behavior might need refinement based on exact UX desired for "Online" + text.
                // For other presets like Busy/Away, their displayName usually takes precedence.
                 statusText
            } else {
                displayStatus.displayName
            }
            finalIcon = iconKey?.let { getIconForKey(it) } ?: displayStatus.icon // Custom icon overrides preset icon
            finalColor = displayStatus.color
        } else {
            // It's a custom status (either ShowingCustomMessage type, a UserGeneratedPreset, or ad-hoc)
            // or an unknown preset ID (should fallback to custom display)
            finalText = statusText?.takeIf { it.isNotBlank() } ?: "Status" // Default if text is blank
            finalIcon = iconKey?.let { getIconForKey(it) } ?: AppPresetStatus.ShowingCustomMessage.icon // Default custom icon
            finalColor = AppPresetStatus.ShowingCustomMessage.color // Default color for custom
        }
    }

    if (finalText.isNullOrBlank() && finalIcon == AppPresetStatus.Offline.icon && isExpired) {
        // If it's expired and defaults to offline with no text, perhaps show nothing or "Offline"
        // For now, it will show "Offline" due to defaultStatusWhenExpired.
        // If you want to show nothing, this condition can be used.
    }


    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = finalIcon,
            contentDescription = "Status: ${finalText ?: "icon"}",
            tint = finalColor,
            modifier = Modifier.size(iconSize)
        )
        if (!finalText.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = finalText,
                style = textStyle,
                color = LocalContentColor.current.copy(alpha = 0.8f)
            )
        }
    }
}

// --- Preview Composable with assumed Icons.Filled.Coffee and Icons.Filled.Work ---
@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
fun StatusViewGlobalOnlinePreview() {
    MaterialTheme {
        StatusView(activeStatusId = AppPresetStatus.Online.id, statusText = null, iconKey = null, expiresAt = null)
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
fun StatusViewGlobalOnlineWithTextPreview() {
    MaterialTheme {
        // Assuming "Online" preset allows displaying an additional text if provided
        StatusView(activeStatusId = AppPresetStatus.Online.id, statusText = "In a meeting", iconKey = null, expiresAt = null)
    }
}


@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
fun StatusViewGlobalBusyPreview() {
    MaterialTheme {
        StatusView(activeStatusId = AppPresetStatus.Busy.id, statusText = null, iconKey = null, expiresAt = null)
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
fun StatusViewGlobalCustomTextOnlyPreview() {
    MaterialTheme {
        StatusView(activeStatusId = AppPresetStatus.ShowingCustomMessage.id, statusText = "Thinking...", iconKey = null, expiresAt = null)
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
fun StatusViewGlobalCustomTextAndIconPreview() {
    MaterialTheme {
        StatusView(activeStatusId = AppPresetStatus.ShowingCustomMessage.id, statusText = "Coding session", iconKey = "work", expiresAt = null)
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
fun StatusViewGlobalCustomWithPresetIDSameAsAppPreset() {
    MaterialTheme {
        // This simulates if activeStatusId was, say, "my_preset_1", but that preset happened to have text "Online"
        // The logic should prioritize the custom text and icon if activeStatusId isn't a direct AppPresetStatus.
        // Current logic: if AppPresetStatus.fromId(activeStatusId) is not Unknown or ShowingCustomMessage, it uses preset.
        // This preview might not fully reflect a UserGeneratedPreset without more context.
        StatusView(activeStatusId = "online_user_preset_id_example", statusText = "Actually custom online", iconKey = "coffee", expiresAt = null)
    }
}


@Composable
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
fun StatusViewExpiredPreview() {
    MaterialTheme {
        StatusView(
            activeStatusId = AppPresetStatus.Busy.id,
            statusText = "Should not show",
            iconKey = null,
            expiresAt = Timestamp(Timestamp.now().seconds - 3600, 0) // 1 hour ago
        )
    }
}

// Add dummy icons if not available in Icons.Filled for preview
object Icons {
    object Filled {
        val Coffee: ImageVector get() = Icons.Filled.LocalCafe // Example mapping
        val Work: ImageVector get() = Icons.Filled.Work // Example mapping
    }
}
