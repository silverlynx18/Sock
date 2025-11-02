package com.sock.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sock.app.navigation.SockDestinations
import com.sock.app.navigation.SockTopLevelDestination
import kotlinx.coroutines.CoroutineScope

class SockAppState internal constructor(
    val navController: NavHostController,
    val topLevelDestinations: List<SockTopLevelDestination>,
    val coroutineScope: CoroutineScope
) {

    val currentDestination: NavDestination?
        @Composable get() {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            return navBackStackEntry?.destination
        }

    val currentTopLevelDestination: SockTopLevelDestination?
        @Composable get() = currentDestination?.route?.let { route ->
            topLevelDestinations.firstOrNull { it.route == route }
        }

    fun navigateToTopLevelDestination(destination: SockTopLevelDestination) {
        navController.navigate(destination.route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
        }
    }

    fun navigateToGroup(groupId: String) {
        navController.navigate("group/$groupId")
    }

    fun navigateToInvitations() {
        navController.navigate(SockDestinations.INVITATIONS_ROUTE)
    }

    fun onBackClick() {
        navController.popBackStack()
    }
}

@Composable
fun rememberSockAppState(
    navController: NavHostController = rememberNavController(),
    topLevelDestinations: List<SockTopLevelDestination> = SockTopLevelDestination.entries.toList(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): SockAppState {
    return remember(navController, coroutineScope) {
        SockAppState(
            navController = navController,
            topLevelDestinations = topLevelDestinations,
            coroutineScope = coroutineScope
        )
    }
}
