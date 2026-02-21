package com.example.sockapp.ui.screens.profile // Keep original package

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sockapp.data.models.User
import com.example.sockapp.data.models.profile.SocialPlatform
import com.example.sockapp.ui.components.status.StatusView
import com.example.sockapp.viewmodels.ProfileViewModel // Corrected import path
import com.example.sockapp.ui.theme.SockAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    profileViewModel: ProfileViewModel = viewModel(), // Use Hilt or pass instance
    onNavigateBack: () -> Unit // Added for navigation
) {
    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    // The user object in profileState will be updated based on the userId passed to loadUserProfile
    val user = profileState.user

    LaunchedEffect(userId) {
        if (userId.isNotEmpty() && userId != profileState.user?.userId) { // Load if different user or not loaded
            profileViewModel.listenToUserProfile(userId) // Changed to listenToUserProfile
        }
    }
    // Clear profile when screen is left if it's not the current user's profile
    // DisposableEffect(Unit) {
    //     onDispose {
    //         if (userId != profileViewModel.auth.currentUser?.uid) {
    //             // profileViewModel.clearViewedUserProfile() // Add this method to VM to clear 'user' field or specific state for viewed profile
    //         }
    //     }
    // }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.username ?: "User Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (profileState.isLoading && user?.userId != userId) { // Show loading if fetching a new profile
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (user == null || user.userId != userId) { // If no user or wrong user loaded
                 Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("User profile not found or error loading.")
                    profileState.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { profileViewModel.listenToUserProfile(userId) }) {
                        Text("Retry")
                    }
                }
            } else { // User data available
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user.profileImageUrl)
                            .error(Icons.Filled.AccountCircle)
                            .placeholder(Icons.Filled.AccountCircle)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${user.username}'s profile image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(120.dp).clip(MaterialTheme.shapes.medium)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(user.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(8.dp))
                    // Display Global Status of the viewed user
                    StatusView(
                        activeStatusId = user.activeStatusId,
                        statusText = user.globalCustomStatusText,
                        iconKey = user.globalCustomStatusIconKey,
                        expiresAt = user.globalStatusExpiresAt
                        // TODO: Potentially show group-specific status if context is available and not overridden
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()

                    // Bio Section
                    Text("Bio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top=16.dp).align(Alignment.Start))
                    Text(
                        user.bio ?: "This user hasn't set a bio yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Start).padding(top=4.dp, bottom = 16.dp)
                    )

                    // Social Media Links Section
                    if (!user.socialMediaLinks.isNullOrEmpty()) {
                        Text("Social Links", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(modifier = Modifier.align(Alignment.Start), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            user.socialMediaLinks.forEach { (key, value) ->
                                val platform = SocialPlatform.fromKey(key)
                                if (platform != null && value.isNotBlank()) {
                                    SocialLinkItemView(platform = platform, value = value) // Re-use from MyProfileScreen
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                         Text("No social links provided.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(bottom=16.dp))
                    }
                    // Add other public info as needed
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserProfileScreenPreview() {
    SockAppTheme {
        // UserProfileScreen(userId = "sampleUserId", onNavigateBack = {})
        Text("UserProfileScreen Preview (Needs ViewModel with User Data for a specific userId)")
    }
}
