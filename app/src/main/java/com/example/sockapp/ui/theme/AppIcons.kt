package com.example.sockapp.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings

object AppIcons {

    // General UI Icons - Filled
    val HomeFilled = Icons.Filled.Home
    val ProfileFilled = Icons.Filled.AccountCircle
    val NotificationsFilled = Icons.Filled.Notifications
    val SettingsFilled = Icons.Filled.Settings
    val SearchFilled = Icons.Filled.Search
    val Send = Icons.Filled.Send
    val Edit = Icons.Filled.Edit // General edit icon
    val Add = Icons.Filled.Add
    val Check = Icons.Filled.Check
    val Close = Icons.Filled.Close
    val BackArrow = Icons.Filled.ArrowBack
    val Menu = Icons.Filled.Menu
    val MoreVert = Icons.Filled.MoreVert
    val Info = Icons.Filled.Info


    // General UI Icons - Outlined
    val HomeOutlined = Icons.Outlined.Home
    val ProfileOutlined = Icons.Outlined.AccountCircle
    val NotificationsOutlined = Icons.Outlined.Notifications
    val SettingsOutlined = Icons.Outlined.Settings
    val SearchOutlined = Icons.Outlined.Search
    val ChatBubbleOutlined = Icons.Outlined.ChatBubbleOutline // Example for general chat icon


    // Specific Feature Icons (if not tied to a data model directly)
    // e.g. val Camera = Icons.Filled.PhotoCamera

    // Note: Status-specific icons (like online, busy) are primarily defined
    // within the UserStatusType sealed class for direct association.
    // However, a generic 'status edit' icon can live here.
    val EditStatus = Icons.Filled.Edit // Could be used on the StatusUpdateElement
}
