package com.example.sockapp.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.sockapp.navigation.MainNavGraph
import com.example.sockapp.navigation.NavGraphRoute
import com.example.sockapp.ui.global.AppNavigationRail
import com.example.sockapp.ui.theme.SockAppTheme
import com.example.sockapp.viewmodels.AuthViewModel // Assuming AuthViewModel is available for logout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    rootNavController: NavHostController, // To navigate back to Auth graph on logout
    authViewModel: AuthViewModel // Pass your actual AuthViewModel instance
) {
    val mainNavController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    var isNavRailExpanded by remember { mutableStateOf(true) } // State for NavRail expanded/collapsed

    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRouteTitle = navRailItems.find { it.route == currentDestination?.route }?.title ?:
                            currentDestination?.route?.substringBefore("/") ?: // Fallback title
                            "SockApp"


    Row(Modifier.fillMaxSize()) {
        AppNavigationRail(
            navController = mainNavController,
            currentDestination = currentDestination,
            isExpanded = isNavRailExpanded,
            onToggleExpanded = { isNavRailExpanded = !isNavRailExpanded }
        )

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentRouteTitle) },
                    // Navigation icon for TopAppBar could be a menu for drawer, or nothing if using NavRail primarily
                    // actions = {
                    //     IconButton(onClick = {
                    //         authViewModel.logout() // Call logout on your AuthViewModel
                    //         rootNavController.navigate(NavGraphRoute.Auth.route) {
                    //             popUpTo(NavGraphRoute.Main.route) { inclusive = true }
                    //         }
                    //     }) {
                    //         Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    //     }
                    // }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
            // bottomBar = { } // If you had a bottom nav bar
        ) { paddingValues ->
            MainNavGraph( // Call the NavGraph builder here
                navController = mainNavController,
                rootNavController = rootNavController, // Pass rootNavController for logout or cross-graph nav
                authViewModel = authViewModel, // Pass authViewModel for logout actions
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}


// Dummy navRailItems for preview if not directly accessible
private val navRailItems = com.example.sockapp.navigation.navRailItems

@Preview(showBackground = true)
@Composable
fun MainAppScreenPreview() {
    // This preview is complex due to NavControllers and ViewModels.
    // For a meaningful preview, you'd typically create a fake NavController
    // and potentially a fake AuthViewModel.
    SockAppTheme {
        // MainAppScreen(rootNavController = rememberNavController(), authViewModel = AuthViewModel())
        // Simplified preview content:
        Text("MainAppScreen Preview (requires ViewModel and NavController)")
    }
}
