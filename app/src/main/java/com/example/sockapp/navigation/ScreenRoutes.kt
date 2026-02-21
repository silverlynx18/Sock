package com.example.sockapp.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// --- Navigation Graph Routes ---
sealed class NavGraphRoute(val route: String) {
    object Root : NavGraphRoute("root_graph")
    object Auth : NavGraphRoute("auth_graph")
    object Main : NavGraphRoute("main_graph")
}

// --- Screens within AuthNavGraph ---
sealed class AuthScreen(val route: String) {
    object Login : AuthScreen("login_screen")
    object SignUp : AuthScreen("signup_screen")
    // object ForgotPassword : AuthScreen("forgot_password_screen")
}

// --- Screens within MainNavGraph (and NavRail items) ---
sealed class MainScreen(val route: String, val title: String, val icon: ImageVector? = null) {
    // Bottom Nav or Nav Rail items
    object Home : MainScreen("home_screen", "Dashboard", Icons.Filled.Home)
    object MyProfile : MainScreen("my_profile_screen", "My Profile", Icons.Filled.AccountCircle)
    object ManageGroups : MainScreen("manage_groups_screen", "Groups", Icons.Filled.Group)
    object AllInvitations : MainScreen("all_invitations_screen", "Invitations", Icons.Filled.Email)
    object Settings : MainScreen("settings_screen", "Settings", Icons.Filled.Settings)

    // Screens without direct nav bar items (navigated to from other screens)
    object UserProfile : MainScreen("user_profile_screen/{userId}", "User Profile") {
        fun createRoute(userId: String) = "user_profile_screen/$userId"
        const val ARG_USER_ID = "userId"
    }
    object CreateGroup : MainScreen("create_group_screen", "Create Group")
    object GroupPage : MainScreen("group_page_screen/{groupId}", "Group Page") {
        fun createRoute(groupId: String) = "group_page_screen/$groupId"
        const val ARG_GROUP_ID = "groupId"
    }
    object GroupDetails : MainScreen("group_details_screen/{groupId}", "Group Details") { // Often part of GroupPage or settings
        fun createRoute(groupId: String) = "group_details_screen/$groupId"
        const val ARG_GROUP_ID = "groupId"
    }
    object InvitationAcceptance : MainScreen("invitation_acceptance_screen/{invitationId}", "Invitation") {
        fun createRoute(invitationId: String) = "invitation_acceptance_screen/$invitationId"
        const val ARG_INVITATION_ID = "invitationId"
    }
    // Add other screens from Module 3 and 4 as needed
    // e.g. GroupMembersList, EditGroupDetails, etc.
}

// List of items for the Navigation Rail
val navRailItems = listOf(
    MainScreen.Home,
    MainScreen.ManageGroups,
    MainScreen.AllInvitations,
    MainScreen.MyProfile,
    MainScreen.Settings
)
