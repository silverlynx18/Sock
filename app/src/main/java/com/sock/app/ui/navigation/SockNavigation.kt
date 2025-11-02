package com.sock.app.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sock.app.data.repository.AuthRepository
import com.sock.app.ui.screens.auth.LoginScreen
import com.sock.app.ui.screens.auth.SignUpScreen
import com.sock.app.ui.screens.dashboard.DashboardScreen
import com.sock.app.ui.screens.group.GroupPageScreen
import com.sock.app.ui.screens.managegroups.ManageGroupsScreen

@Composable
fun SockNavigation(context: Context) {
    val navController = rememberNavController()
    val authRepository = remember { AuthRepository(context) }
    
    // Initialize auth on startup
    LaunchedEffect(Unit) {
        authRepository.initializeAuth()
    }
    
    val isLoggedIn by authRepository.isUserLoggedIn.collectAsState(initial = false)
    
    val startDestination = if (isLoggedIn) {
        Screen.Dashboard.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                onLoginSuccess = { navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }}
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onSignUpSuccess = { navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.SignUp.route) { inclusive = true }
                }}
            )
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Dashboard.route) { inclusive = true }
                }},
                onNavigateToGroup = { groupId ->
                    navController.navigate(Screen.GroupPage.createRoute(groupId))
                },
                onNavigateToManageGroups = {
                    navController.navigate(Screen.ManageGroups.route)
                }
            )
        }

        composable(Screen.GroupPage.route) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString(Screen.GroupPage.GROUP_ID_ARG) ?: ""
            GroupPageScreen(
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGroupDetails = {
                    navController.navigate(Screen.GroupDetails.createRoute(groupId))
                },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                }
            )
        }

        composable(Screen.ManageGroups.route) {
            ManageGroupsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGroup = { groupId ->
                    navController.navigate(Screen.GroupPage.createRoute(groupId))
                },
                onNavigateToGroupDetails = { groupId ->
                    navController.navigate(Screen.GroupDetails.createRoute(groupId))
                },
                onCreateGroup = {
                    // TODO: Navigate to create group screen
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Dashboard : Screen("dashboard")
    object ManageGroups : Screen("manage_groups")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object GroupPage : Screen("group_page/{groupId}") {
        fun createRoute(groupId: String) = "group_page/$groupId"
        const val GROUP_ID_ARG = "groupId"
    }
    object GroupDetails : Screen("group_details/{groupId}") {
        fun createRoute(groupId: String) = "group_details/$groupId"
    }
    object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
}
