package com.example.sockapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sockapp.data.models.User
import com.example.sockapp.ui.profile.ProfileViewModel
// import com.example.sockapp.ui.theme.SockAppTheme // Assuming you have a theme

@Composable
fun UserProfileScreen(
    userId: String,
    profileViewModel: ProfileViewModel = viewModel()
    // onNavigateBack: () -> Unit // Optional: for a back button
) {
    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val user = profileState.user

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            profileViewModel.loadUserProfile(userId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            profileState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            profileState.error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: ${profileState.error}", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { profileViewModel.loadUserProfile(userId) }) {
                        Text("Retry")
                    }
                }
            }
            user != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("User Profile", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Display public user information
                    // Add Profile Image later
                    Text("Username: ${user.username}", style = MaterialTheme.typography.titleMedium)
                    user.bio?.takeIf { it.isNotBlank() }?.let {
                        Text("Bio: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    user.customStatus?.takeIf { it.isNotBlank() }?.let {
                        Text("Status: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    // Do NOT display email or other private info here unless specifically intended
                    // Text("Member Since: ${user.createdAt.toDate().toString()}", style = MaterialTheme.typography.bodySmall) // Format date appropriately

                    // Add other public info as needed (e.g., follower counts, public posts feed)
                }
            }
            else -> {
                 Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("User not found or profile is private.")
                     Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { profileViewModel.loadUserProfile(userId) }) { // Retry
                        Text("Reload Profile")
                    }
                }
            }
        }
        // Add a back button if needed, e.g., using TopAppBar and navigation icon
    }
}

@Preview(showBackground = true)
@Composable
fun UserProfileScreenPreview() {
    // SockAppTheme {
        // Create a dummy ProfileViewModel that provides a sample user for the preview
        val mockProfileViewModel: ProfileViewModel = viewModel() // This won't show much in preview
        // To make this preview more useful, you'd typically mock the ProfileViewModel
        // or pass a User object directly to a stateless version of this Composable.

        // For example, if UserProfileScreen was stateless:
        // UserProfileScreenContent(user = User(username = "OtherUser", bio = "Bio of other user"))
        UserProfileScreen(userId = "sampleUserId", profileViewModel = mockProfileViewModel)
    // }
}

/*
// Example of a stateless version for better previewing if desired:
@Composable
fun UserProfileScreenContent(user: User?) {
    if (user != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("User Profile", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Username: ${user.username}", style = MaterialTheme.typography.titleMedium)
            // ... other fields
        }
    } else {
        Text("User not found.")
    }
}
*/
