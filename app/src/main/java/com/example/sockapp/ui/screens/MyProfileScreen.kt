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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    onNavigateToLogin: () -> Unit // Or to a welcome screen after deletion
) {
    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val user = profileState.user

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Use this to trigger listening when the screen is composed or user changes
    LaunchedEffect(Unit) { // Or key on profileViewModel.profileState.user?.userId if needed
        profileViewModel.listenToMyProfile()
    }

    LaunchedEffect(profileState.accountDeletionSuccess) {
        if (profileState.accountDeletionSuccess) {
            onNavigateToLogin() // Navigate away after successful deletion
            profileViewModel.clearAccountDeletionStatus()
        }
    }
     LaunchedEffect(profileState.updateSuccess) {
        if (profileState.updateSuccess) {
            // Potentially show a snackbar or confirmation
            // And then clear the status in VM
            profileViewModel.clearProfileUpdateStatus()
        }
    }


    if (profileState.isLoading && user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (user == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Could not load profile.")
                profileState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { profileViewModel.listenToMyProfile() }) { // Retry
                    Text("Retry")
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("My Profile", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Basic display of user info
            Text("Username: ${user.username}", style = MaterialTheme.typography.titleMedium)
            Text("Email: ${user.email}", style = MaterialTheme.typography.bodyMedium)
            Text("Bio: ${user.bio ?: "Not set"}", style = MaterialTheme.typography.bodyMedium)
            Text("Status: ${user.customStatus ?: "Not set"}", style = MaterialTheme.typography.bodyMedium)
            // Add Profile Image later with Coil or Glide

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { showEditDialog = true }) {
                Text("Edit Profile")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showDeleteConfirmDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Account")
            }

            if (profileState.isUpdating || profileState.accountDeletionLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            profileState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
             profileState.accountDeletionError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            user = user,
            onDismiss = { showEditDialog = false },
            onSave = { updatedBio, updatedStatus ->
                profileViewModel.updateUserProfile(updatedBio, updatedStatus, null) // Null for profileImageUrl for now
                showEditDialog = false
            },
            profileState = profileState
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete your account? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        profileViewModel.deleteAccount()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (bio: String, status: String) -> Unit,
    profileState: com.example.sockapp.ui.profile.ProfileState // Explicitly use the state type
) {
    var bio by remember(user?.bio) { mutableStateOf(user?.bio ?: "") }
    var customStatus by remember(user?.customStatus) { mutableStateOf(user?.customStatus ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customStatus,
                    onValueChange = { customStatus = it },
                    label = { Text("Custom Status") }
                )
                if (profileState.isUpdating) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator()
                }
                profileState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(bio, customStatus) }, enabled = !profileState.isUpdating) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss, enabled = !profileState.isUpdating) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MyProfileScreenPreview() {
    // SockAppTheme {
        val previewUser = User(userId = "123", username = "PreviewUser", email = "preview@example.com", bio = "Living the preview life.", customStatus = "Online and coding")
        val profileViewModel: ProfileViewModel = viewModel() // This will be a dummy for preview
        // In a real preview, you might mock the ViewModel or its state.
        // For simplicity, we rely on the default state or mock data directly if needed.
        // This preview will likely show the loading or error state unless the VM is properly mocked.
        MyProfileScreen(profileViewModel = profileViewModel, onNavigateToLogin = {})
    // }
}

@Preview(showBackground = true)
@Composable
fun EditProfileDialogPreview() {
    // SockAppTheme {
        EditProfileDialog(
            user = User(bio = "Test bio", customStatus = "Test status"),
            onDismiss = {},
            onSave = { _, _ -> },
            profileState = com.example.sockapp.ui.profile.ProfileState() // Pass default state
        )
    // }
}
