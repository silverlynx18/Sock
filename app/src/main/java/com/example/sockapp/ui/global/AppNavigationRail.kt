package com.example.sockapp.ui.global

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu // Standard menu icon for toggle
import androidx.compose.material.icons.filled.MenuOpen // Alternative for expanded state
import androidx.compose.material.icons.filled.SportsSoccer // Placeholder "Couch" or "App" Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.example.sockapp.navigation.MainScreen
import com.example.sockapp.navigation.navRailItems
import com.example.sockapp.ui.theme.SockAppTheme

@Composable
fun AppNavigationRail(
    navController: NavController,
    currentDestination: NavDestination?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        header = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.MenuOpen else Icons.Filled.Menu,
                        contentDescription = if (isExpanded) "Collapse Menu" else "Expand Menu"
                    )
                }
                // "Global Couch Icon" - can navigate home or be a brand icon
                // For this implementation, it will navigate home if clicked when expanded
                // and just be an icon when collapsed.
                IconButton(
                    onClick = {
                        if (isExpanded) { // Only navigate if expanded, or always if preferred
                            navController.navigate(MainScreen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            onToggleExpanded() // If collapsed, clicking it expands the rail
                        }
                    }
                ) {
                    Icon(
                        Icons.Filled.SportsSoccer, // Replace with actual "Couch" icon
                        contentDescription = "App Home / Brand Icon"
                    )
                }
                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))
            }
        },
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant // Or specific color
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center // Center items vertically
        ) {
            navRailItems.forEach { screen ->
                NavigationRailItem(
                    icon = { screen.icon?.let { Icon(it, contentDescription = screen.title) } },
                    label = { if (isExpanded) Text(screen.title) else null }, // Show label only when expanded
                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    alwaysShowLabel = false // Important for label behavior with isExpanded
                )
                Spacer(Modifier.height(8.dp)) // Adjust spacing between items
            }
        }
    }
}

@Preview(showBackground = true, name = "Navigation Rail Collapsed")
@Composable
fun AppNavigationRailCollapsedPreview() {
    SockAppTheme {
        val navController = rememberNavController()
        AppNavigationRail(
            navController = navController,
            currentDestination = null, // In a real scenario, get this from NavController
            isExpanded = false,
            onToggleExpanded = {}
        )
    }
}

@Preview(showBackground = true, name = "Navigation Rail Expanded")
@Composable
fun AppNavigationRailExpandedPreview() {
    SockAppTheme {
        val navController = rememberNavController()
        // Simulate a current destination for selection highlighting
        // This requires manually creating a NavDestination or running in an environment with a NavHost.
        // For simplicity, passing null.
        AppNavigationRail(
            navController = navController,
            currentDestination = null,
            isExpanded = true,
            onToggleExpanded = {}
        )
    }
}
