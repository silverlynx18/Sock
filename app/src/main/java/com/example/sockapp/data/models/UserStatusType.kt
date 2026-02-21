package com.example.sockapp.data.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline // Custom
import androidx.compose.material.icons.filled.Circle // Online
import androidx.compose.material.icons.filled.AccessTime // Away
import androidx.compose.material.icons.filled.Block // Busy
import androidx.compose.material.icons.filled.PersonOutline // Offline
import androidx.compose.material.icons.outlined.HelpOutline // Unknown
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

sealed class UserStatusType(
    val id: String, // Key to be stored in User model (e.g., in user.statusId)
    open val displayName: String,
    open val icon: ImageVector,
    open val color: Color,
    open val isCustomTextAllowed: Boolean = false // True if this type uses the customStatusText from User model
) {
    object Online : UserStatusType("online", "Online", Icons.Filled.Circle, Color(0xFF4CAF50))
    object Busy : UserStatusType("busy", "Busy", Icons.Filled.Block, Color(0xFFF44336))
    object Away : UserStatusType("away", "Away", Icons.Filled.AccessTime, Color(0xFFFF9800))
    object Custom : UserStatusType("custom", "Custom", Icons.Filled.ChatBubbleOutline, Color(0xFF2196F3), isCustomTextAllowed = true)
    object Offline : UserStatusType("offline", "Offline", Icons.Filled.PersonOutline, Color.Gray)
    object Unknown : UserStatusType("unknown", "Unknown", Icons.Outlined.HelpOutline, Color.LightGray)

    companion object {
        // This list would be used by the StatusSelectionBottomSheet
        val selectableStatuses: List<UserStatusType> = listOf(Online, Busy, Away, Custom, Offline)

        fun fromId(id: String?): UserStatusType {
            return when (id?.lowercase()) {
                Online.id -> Online
                Busy.id -> Busy
                Away.id -> Away
                Custom.id -> Custom
                Offline.id -> Offline
                else -> Unknown // Or Offline as a safer default
            }
        }

        // Example: How a ViewModel might determine the display name
        fun getDisplayStatusText(statusId: String?, customStatusText: String?): String {
            val type = fromId(statusId)
            return if (type is Custom && !customStatusText.isNullOrBlank()) {
                customStatusText
            } else {
                type.displayName
            }
        }
         // Example: How a ViewModel might determine the UserStatusType instance
        fun determineUserStatusType(statusId: String?): UserStatusType {
            return fromId(statusId)
        }
    }
}
