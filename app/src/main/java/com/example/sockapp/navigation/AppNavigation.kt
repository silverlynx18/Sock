package com.example.sockapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.sockapp.ui.screens.MainAppScreen
import com.example.sockapp.ui.screens.auth.LoginScreen // Assuming this path from Module 1
import com.example.sockapp.ui.screens.auth.SignUpScreen // Assuming this path from Module 1
import com.example.sockapp.viewmodels.AuthViewModel // Assuming this path

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel() // Use Hilt or pass instance if not using hiltViewModel
) {
    val authState by authViewModel.authState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (authState.isAuthenticated) NavGraphRoute.Main.route else NavGraphRoute.Auth.route,
        route = NavGraphRoute.Root.route
    ) {
        // Authentication Graph
        navigation(
            route = NavGraphRoute.Auth.route,
            startDestination = AuthScreen.Login.route
        ) {
            composable(AuthScreen.Login.route) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(NavGraphRoute.Main.route) {
                            popUpTo(NavGraphRoute.Auth.route) { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = {
                        navController.navigate(AuthScreen.SignUp.route)
                    }
                )
            }
            composable(AuthScreen.SignUp.route) {
                SignUpScreen(
                    authViewModel = authViewModel,
                    onSignUpSuccess = {
                        // Decide if auto-login or navigate to login
                         navController.navigate(AuthScreen.Login.route) {
                            popUpTo(AuthScreen.Login.route) { inclusive = true } // Go to login after sign up
                        }
                        // Or, if auto-login after sign up and AuthState updates:
                        // navController.navigate(NavGraphRoute.Main.route) {
                        //    popUpTo(NavGraphRoute.Auth.route) { inclusive = true }
                        // }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            // Add ForgotPasswordScreen if it exists
        }

        // Main Application Graph (post-login)
        composable(route = NavGraphRoute.Main.route) {
            // MainAppScreen contains its own NavHost and Scaffold for the main app content
            MainAppScreen(rootNavController = navController, authViewModel = authViewModel)
        }
    }
}
