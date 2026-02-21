package com.example.sockapp.navigation

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.sockapp.ui.screens.group.CreateGroupScreen
import com.example.sockapp.ui.screens.group.GroupDetailsPageScreen
import com.example.sockapp.ui.screens.group.GroupPageScreen
import com.example.sockapp.ui.screens.group.ManageGroupsScreen
import com.example.sockapp.ui.screens.home.HomeScreen // Assuming a HomeScreen placeholder
import com.example.sockapp.ui.screens.invitation.AllInvitationsScreen
import com.example.sockapp.ui.screens.invitation.InvitationAcceptanceScreen
import com.example.sockapp.ui.screens.profile.MyProfileScreen
import com.example.sockapp.ui.screens.profile.UserProfileScreen
import com.example.sockapp.ui.screens.settings.SettingsScreen
import com.example.sockapp.viewmodels.AuthViewModel
import com.example.sockapp.viewmodels.GroupViewModel
import com.example.sockapp.viewmodels.InvitationViewModel
import com.example.sockapp.viewmodels.ProfileViewModel

@Composable
fun MainNavGraph(
    navController: NavHostController,
    rootNavController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val groupViewModel: GroupViewModel = viewModel()
    val invitationViewModel: InvitationViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = MainScreen.Home.route,
        modifier = modifier,
        route = NavGraphRoute.Main.route
    ) {
        composable(MainScreen.Home.route) {
            HomeScreen(
                profileViewModel = profileViewModel, // Pass for default group navigation
                navController = navController
            )
        }
        composable(MainScreen.MyProfile.route) {
            MyProfileScreen(
                profileViewModel = profileViewModel,
                onNavigateToLogin = { // This is effectively a logout then go to auth
                    authViewModel.logout()
                    rootNavController.navigate(NavGraphRoute.Auth.route) {
                        popUpTo(NavGraphRoute.Main.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = MainScreen.UserProfile.route,
            arguments = listOf(navArgument(MainScreen.UserProfile.ARG_USER_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(MainScreen.UserProfile.ARG_USER_ID)
            if (userId != null) {
                UserProfileScreen(
                    userId = userId,
                    profileViewModel = profileViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(MainScreen.ManageGroups.route) {
            ManageGroupsScreen(
                groupViewModel = groupViewModel,
                onNavigateToCreateGroup = { navController.navigate(MainScreen.CreateGroup.route) },
                onNavigateToGroupPage = { groupId -> navController.navigate(MainScreen.GroupPage.createRoute(groupId)) }
            )
        }
        composable(MainScreen.CreateGroup.route) {
            CreateGroupScreen(
                groupViewModel = groupViewModel,
                onNavigateBack = { navController.popBackStack() },
                onGroupCreatedSuccessfully = {
                    navController.popBackStack(MainScreen.ManageGroups.route, inclusive = false)
                }
            )
        }
        composable(
            route = MainScreen.GroupPage.route,
            arguments = listOf(navArgument(MainScreen.GroupPage.ARG_GROUP_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString(MainScreen.GroupPage.ARG_GROUP_ID)
            if (groupId != null) {
                GroupPageScreen(
                    groupId = groupId,
                    groupViewModel = groupViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToGroupSettings = { navController.navigate(MainScreen.GroupDetails.createRoute(it)) },
                    onNavigateToGroupMembersList = { /* TODO: Define route for full members list screen */ }
                )
            } else {
                navController.popBackStack()
            }
        }
        composable(
            route = MainScreen.GroupDetails.route,
            arguments = listOf(navArgument(MainScreen.GroupDetails.ARG_GROUP_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString(MainScreen.GroupDetails.ARG_GROUP_ID)
            if (groupId != null) {
                GroupDetailsPageScreen(
                    groupId = groupId,
                    groupViewModel = groupViewModel,
                    profileViewModel = profileViewModel, // Pass ProfileViewModel
                    onNavigateBack = { navController.popBackStack() },
                    onViewMembersList = { /* TODO: Define route for full members list screen */ }
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(MainScreen.AllInvitations.route) {
            AllInvitationsScreen(
                invitationViewModel = invitationViewModel,
                onNavigateBack = { navController.popBackStack() },
                onViewInvitation = { invitationId -> navController.navigate(MainScreen.InvitationAcceptance.createRoute(invitationId)) }
            )
        }
        composable(
            route = MainScreen.InvitationAcceptance.route,
            arguments = listOf(navArgument(MainScreen.InvitationAcceptance.ARG_INVITATION_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val invitationId = backStackEntry.arguments?.getString(MainScreen.InvitationAcceptance.ARG_INVITATION_ID)
            InvitationAcceptanceScreen(
                invitationId = invitationId,
                invitationViewModel = invitationViewModel,
                onNavigateBack = { navController.popBackStack() },
                onInvitationProcessed = { navController.popBackStack(MainScreen.AllInvitations.route, inclusive = false)}
            )
        }

        composable(MainScreen.Settings.route) {
            SettingsScreen(
                 profileViewModel = profileViewModel,
                 authViewModel = authViewModel,
                 onNavigateBack = { navController.popBackStack() },
                 onLoggedOutCompletely = {
                    authViewModel.logout()
                    rootNavController.navigate(NavGraphRoute.Auth.route) {
                        popUpTo(NavGraphRoute.Main.route) { inclusive = true }
                        launchSingleTop = true;
                    }
                 },
                 onNavigateToEditProfile = { navController.navigate(MainScreen.MyProfile.route) }, // Or a dedicated edit screen
                 onNavigateToGroupSelection = { navController.navigate(MainScreen.ManageGroups.route) } // For selecting default group
            )
        }
    }
}

// Placeholder for HomeScreen - replace with your actual HomeScreen composable
// @Composable
// fun HomeScreen() {
//     Text("Home Screen Content")
// }

// Placeholder for SettingsScreen was removed as it's now fully defined in its own file.
