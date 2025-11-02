package com.sock.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sock.app.data.repository.AuthRepository
import com.sock.app.ui.screens.auth.LoginScreen
import com.sock.app.ui.screens.auth.SignUpScreen
import com.sock.app.ui.screens.dashboard.DashboardScreen

@Composable
fun SockNavigation() {
    val navController = rememberNavController()
    val authRepository = AuthRepository()
    
    val startDestination = if (authRepository.isUserLoggedIn) {
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
                }}
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
    }
    object GroupDetails : Screen("group_details/{groupId}") {
        fun createRoute(groupId: String) = "group_details/$groupId"
    }
    object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
}
