package com.sock.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class SockTopLevelDestination(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    DASHBOARD(
        route = "dashboard",
        icon = Icons.Outlined.DashboardCustomize,
        label = "Dashboard"
    ),
    MANAGE_GROUPS(
        route = "manage_groups",
        icon = Icons.Outlined.Groups,
        label = "Manage Groups"
    ),
    SETTINGS(
        route = "settings",
        icon = Icons.Outlined.Settings,
        label = "Settings"
    ),
    PROFILE(
        route = "profile",
        icon = Icons.Outlined.AccountCircle,
        label = "My Profile"
    )
}

object SockDestinations {
    const val GROUP_DETAIL_ROUTE = "group/{groupId}"
    const val GROUP_DETAIL_ARGUMENT_ID = "groupId"
    const val INVITATIONS_ROUTE = "invitations"
}
