package com.sock.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sock.app.model.AvailabilityStatus
import com.sock.app.model.GroupSummary
import com.sock.app.model.InvitationSummary
import com.sock.app.ui.SockAppState
import com.sock.app.ui.SockUiState
import com.sock.app.ui.screens.dashboard.DashboardScreen
import com.sock.app.ui.screens.groupdetail.GroupDetailScreen
import com.sock.app.ui.screens.invitations.InvitationsScreen
import com.sock.app.ui.screens.managegroups.ManageGroupsScreen
import com.sock.app.ui.screens.profile.ProfileScreen
import com.sock.app.ui.screens.settings.SettingsScreen

@Composable
fun SockNavHost(
    appState: SockAppState,
    uiState: SockUiState,
    onUpdateGlobalStatus: () -> Unit,
    onUpdateGroupStatus: (GroupSummary) -> Unit,
    onRevertGroupStatus: (GroupSummary) -> Unit,
    onAcceptInvitation: (InvitationSummary) -> Unit,
    onDeclineInvitation: (InvitationSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = appState.navController,
        startDestination = SockTopLevelDestination.DASHBOARD.route,
        modifier = modifier
    ) {
        composable(SockTopLevelDestination.DASHBOARD.route) {
            DashboardScreen(
                globalStatus = uiState.globalStatus,
                groups = uiState.dashboardGroups,
                onUpdateGlobalStatus = onUpdateGlobalStatus,
                onGroupSelected = { appState.navigateToGroup(it.id) },
                onManageGroups = { appState.navigateToTopLevelDestination(SockTopLevelDestination.MANAGE_GROUPS) },
                onViewInvitations = appState::navigateToInvitations
            )
        }

        composable(SockTopLevelDestination.MANAGE_GROUPS.route) {
            ManageGroupsScreen(
                managedGroups = uiState.groupsManagedByUser,
                memberGroups = uiState.groupsMemberOf,
                pendingInvites = uiState.invitations.size,
                onCreateGroup = { /* TODO: trigger create group workflow */ },
                onGroupSelected = { appState.navigateToGroup(it.id) },
                onViewInvitations = appState::navigateToInvitations
            )
        }

        composable(SockTopLevelDestination.SETTINGS.route) {
            SettingsScreen(
                appVersion = "0.1.0",
                onDeleteAccount = { /* TODO: hook into account deletion */ },
                onViewPrivacyPolicy = { /* TODO: navigate to privacy policy */ },
                onViewTerms = { /* TODO: navigate to terms of service */ }
            )
        }

        composable(SockTopLevelDestination.PROFILE.route) {
            ProfileScreen(
                displayName = "Alex Chen",
                username = "alex",
                phoneNumber = "+1 (555) 010-1234",
                onEditDisplayName = { /* TODO */ },
                onChangePhoto = { /* TODO */ },
                onLogOut = { /* TODO */ }
            )
        }

        composable(
            route = SockDestinations.GROUP_DETAIL_ROUTE,
            arguments = listOf(
                navArgument(SockDestinations.GROUP_DETAIL_ARGUMENT_ID) {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val groupId = entry.arguments?.getString(SockDestinations.GROUP_DETAIL_ARGUMENT_ID)
            val group = uiState.dashboardGroups.firstOrNull { it.id == groupId }

            GroupDetailScreen(
                groupName = group?.name ?: "Group",
                groupStatus = group?.status ?: AvailabilityStatus.OPEN_TO_HANGOUT,
                members = uiState.selectedGroupMembers,
                onUpdateStatus = {
                    group?.let(onUpdateGroupStatus)
                },
                onRevertToGlobal = {
                    group?.let(onRevertGroupStatus)
                },
                onMemberSelected = { /* TODO: open member profile */ },
                onManageGroup = { /* TODO: open group settings*/ },
                onLeaveGroup = { /* TODO: leave group */ }
            )
        }

        composable(SockDestinations.INVITATIONS_ROUTE) {
            InvitationsScreen(
                invitations = uiState.invitations,
                onAccept = onAcceptInvitation,
                onDecline = onDeclineInvitation
            )
        }
    }
}
