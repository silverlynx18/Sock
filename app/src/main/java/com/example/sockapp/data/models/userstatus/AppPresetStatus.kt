package com.example.sockapp.data.models.userstatus

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.WavingHand // Example for "Away"
import androidx.compose.material.icons.filled.PersonOutline // Example for "Offline"
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// This represents the app-defined, non-user-generated statuses from Module 2.
// Renamed from UserStatusType (Module 2) to AppPresetStatus for clarity.
sealed class AppPresetStatus(
    val id: String, // Key to be stored in User.activeStatusId or GroupStatusDetail.activeStatusId
    open val displayName: String,
    open val icon: ImageVector, // This is the default icon from AppIcons or M2 icons
    open val color: Color // Default color
) {
    object Online : AppPresetStatus("online", "Online", Icons.Filled.Circle, Color(0xFF4CAF50))
    object Busy : AppPresetStatus("busy", "Busy", Icons.Filled.Block, Color(0xFFF44336))
    object Away : AppPresetStatus("away", "Away", Icons.Filled.WavingHand, Color(0xFFFF9800)) // Example
    // This represents a state where a custom text is set, but it's still an app-preset "type"
    // It signals that User.globalCustomStatusText or GroupStatusDetail.customStatusText should be displayed.
    // The icon here could be a generic "custom" icon.
    object ShowingCustomMessage : AppPresetStatus("showing_custom", "Custom", Icons.Filled.ChatBubbleOutline, Color(0xFF2196F3))
    object Offline : AppPresetStatus("offline", "Offline", Icons.Filled.PersonOutline, Color.Gray)
    object Unknown : AppPresetStatus("unknown", "Unknown", Icons.Outlined.HelpOutline, Color.LightGray)

    companion object {
        val selectableAppPresetStatuses: List<AppPresetStatus> = listOf(Online, Busy, Away, ShowingCustomMessage, Offline)

        fun fromId(id: String?): AppPresetStatus {
            return when (id?.lowercase()) {
                Online.id -> Online
                Busy.id -> Busy
                Away.id -> Away
                ShowingCustomMessage.id -> ShowingCustomMessage
                Offline.id -> Offline
                else -> Unknown
            }
        }
    }
}
