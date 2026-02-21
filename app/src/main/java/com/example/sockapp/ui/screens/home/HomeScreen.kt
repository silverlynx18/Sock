package com.example.sockapp.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.sockapp.navigation.MainScreen
import com.example.sockapp.viewmodels.ProfileViewModel
import com.example.sockapp.ui.screens.invitation.PendingInvitationsSummaryCard // Assuming this path
import com.example.sockapp.viewmodels.InvitationViewModel // For the summary card
import androidx.lifecycle.viewmodel.compose.viewModel // For default viewModel creation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profileViewModel: ProfileViewModel,
    navController: NavController,
    invitationViewModel: InvitationViewModel = viewModel() // For summary card
) {
    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val user = profileState.user
    var hasAttemptedNavigation by remember { mutableStateOf(false) }

    val invitationState by invitationViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        // Fetch initial data needed for dashboard, like pending invitations summary
        invitationViewModel.fetchMyPendingInvitations()
        // User profile (which includes defaultGroupIdOnOpen) is already being listened to by ProfileViewModel
    }

    LaunchedEffect(user, hasAttemptedNavigation) {
        if (user != null && !user.defaultGroupIdOnOpen.isNullOrBlank() && !hasAttemptedNavigation) {
            navController.navigate(MainScreen.GroupPage.createRoute(user.defaultGroupIdOnOpen!!)) {
                // Replace HomeScreen in backstack so pressing back from group page doesn't go to empty HomeScreen
                popUpTo(MainScreen.Home.route) { inclusive = true }
                launchSingleTop = true // Avoid multiple copies if already there
            }
            hasAttemptedNavigation = true // Ensure navigation attempt only happens once per user session/change
        } else if (user != null && user.defaultGroupIdOnOpen.isNullOrBlank()) {
            // If default group is explicitly null or blank, mark as attempted so it doesn't re-evaluate unless user changes.
            hasAttemptedNavigation = true
        }
    }

    Scaffold(
        // TopAppBar is typically part of MainAppScreen, so HomeScreen might not need its own,
        // unless MainAppScreen's TopAppBar is dynamic and HomeScreen provides its title.
        // For this example, let's assume MainAppScreen handles the TopAppBar title based on current route.
    ) { paddingValues ->
        // Show loading indicator while user data is loading and navigation decision hasn't been made
        if (user == null || (!hasAttemptedNavigation && !user.defaultGroupIdOnOpen.isNullOrBlank())) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading dashboard...")
            }
        } else {
            // Actual HomeScreen content (Dashboard)
            // This is shown if no default group navigation occurs or after returning from it.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Welcome to SockApp Dashboard, ${user?.username ?: "User"}!",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("This is your main dashboard content area.")

                // Example: Pending Invitations Summary Card
                if (invitationState.pendingInvitations.isNotEmpty()) {
                    PendingInvitationsSummaryCard(
                        pendingInvitations = invitationState.pendingInvitations,
                        onViewAllInvitationsClicked = {
                            navController.navigate(MainScreen.AllInvitations.route)
                        }
                    )
                }


                // Placeholder navigation buttons (examples)
                Button(onClick = { navController.navigate(MainScreen.ManageGroups.route) }) {
                    Text("View My Groups")
                }
                Button(onClick = { navController.navigate(MainScreen.MyProfile.route) }) {
                    Text("View My Profile")
                }
                 Button(onClick = { navController.navigate(MainScreen.Settings.route) }) {
                    Text("Go to Settings")
                }

                // Add other dashboard elements here
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        // HomeScreen(profileViewModel = viewModel(), navController = rememberNavController())
        Text("HomeScreen Preview (Requires ViewModel and NavController)")
    }
}
