package com.example.sockapp.ui.screens.profile // Keep original package for screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sockapp.data.models.User
import com.example.sockapp.data.models.profile.SocialPlatform
import com.example.sockapp.data.models.profile.getFullSocialUrl
import com.example.sockapp.ui.components.status.StatusView // Updated StatusView
import com.example.sockapp.viewmodels.ProfileViewModel // Corrected ViewModel import path
import com.example.sockapp.viewmodels.ProfileState // Corrected State import path
import com.example.sockapp.ui.theme.SockAppTheme
import com.example.sockapp.data.models.userstatus.AppPresetStatus // For status display

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(), // Use Hilt or pass instance
    onNavigateToLogin: () -> Unit, // After account deletion
    // onNavigateToStatusEditor: () -> Unit // Optional: if navigating to a dedicated status editor screen
) {
    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val user = profileState.user
    val snackbarHostState = remember { SnackbarHostState() }
    val localCoroutineScope = rememberCoroutineScope() // For snackbar

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialogStep1 by remember { mutableStateOf(false) }
    var showDeleteConfirmDialogStep2 by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Assuming listenToMyProfile is now listenToUserProfile in ViewModel
        // And currentUserId is fetched/set within ViewModel or passed if needed
        val currentUserId = profileViewModel.auth.currentUser?.uid // Direct access for init, though VM should handle its own userID
        currentUserId?.let { profileViewModel.listenToUserProfile(it) }
    }

    LaunchedEffect(profileState.accountDeletionSuccess) {
        if (profileState.accountDeletionSuccess) {
            profileViewModel.clearAccountDeletionStatus()
            onNavigateToLogin() // Navigate away after successful deletion
        }
    }
    LaunchedEffect(profileState.updateSuccessMessage) {
        profileState.updateSuccessMessage?.let {
            localCoroutineScope.launch { snackbarHostState.showSnackbar(it) }
            profileViewModel.clearMessagesAndErrors()
        }
    }
    LaunchedEffect(profileState.error) {
        profileState.error?.let {
            localCoroutineScope.launch { snackbarHostState.showSnackbar("Error: $it") }
            profileViewModel.clearMessagesAndErrors()
        }
    }
     LaunchedEffect(profileState.accountDeletionError) {
        profileState.accountDeletionError?.let {
            localCoroutineScope.launch { snackbarHostState.showSnackbar("Deletion Error: $it") }
            profileViewModel.clearMessagesAndErrors() // Also clear this specific error
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (profileState.isLoading && user == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (user == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Could not load profile.")
                    profileState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { profileViewModel.auth.currentUser?.uid?.let { profileViewModel.listenToUserProfile(it) } }) {
                        Text("Retry")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
                    contentDescription = "My profile image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(120.dp).clip(MaterialTheme.shapes.medium)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(user.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(user.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(8.dp))
                StatusView(
                    activeStatusId = user.activeStatusId,
                    statusText = user.globalCustomStatusText,
                    iconKey = user.globalCustomStatusIconKey,
                    expiresAt = user.globalStatusExpiresAt
                )
                Spacer(modifier = Modifier.height(16.dp))
                Divider()

                Text("Bio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top=16.dp).align(Alignment.Start))
                Text(
                    user.bio ?: "No bio set. Click 'Edit Profile' to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Start).padding(top=4.dp, bottom = 16.dp)
                )

                if (!user.socialMediaLinks.isNullOrEmpty()) {
                     Text("Social Links", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
                     Spacer(modifier = Modifier.height(8.dp))
                     Column(modifier = Modifier.align(Alignment.Start), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        user.socialMediaLinks.forEach { (key, value) ->
                            val platform = SocialPlatform.fromKey(key)
                            if (platform != null && value.isNotBlank()) {
                                SocialLinkItemView(platform = platform, value = value)
                            }
                        }
                     }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Text("No social links added yet.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start).padding(bottom=16.dp))
                }


                Button(onClick = { showEditProfileDialog = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Profile Icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile & Links")
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showDeleteConfirmDialogStep1 = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = "Delete Account Icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account")
                }

                if (profileState.isUpdating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showEditProfileDialog && user != null) {
        EditMyProfileDialog(
            user = user,
            onDismiss = { showEditProfileDialog = false },
            onSave = { updatedBio, updatedSocialLinks, updatedProfileImage ->
                profileViewModel.updateUserProfileData(
                    bio = updatedBio,
                    socialMediaLinks = updatedSocialLinks,
                    profileImageUrl = updatedProfileImage
                )
                showEditProfileDialog = false
            },
            isSaving = profileState.isUpdating
        )
    }

    if (showDeleteConfirmDialogStep1) {
        com.example.sockapp.ui.components.settings.AccountDeletionConfirmationDialogStep1(
            onConfirm = { showDeleteConfirmDialogStep1 = false; showDeleteConfirmDialogStep2 = true },
            onDismiss = { showDeleteConfirmDialogStep1 = false }
        )
    }
    if (showDeleteConfirmDialogStep2) {
        com.example.sockapp.ui.components.settings.AccountDeletionConfirmationDialogStep2(
            isLoading = profileState.accountDeletionLoading,
            errorMessage = profileState.accountDeletionError,
            onConfirmDeletion = { profileViewModel.deleteAccount() },
            onDismiss = { showDeleteConfirmDialogStep2 = false; profileViewModel.clearAccountDeletionStatus() }
        )
    }
}

@Composable
fun SocialLinkItemView(platform: SocialPlatform, value: String) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current // For Toast messages or other context needs
    val fullUrl = getFullSocialUrl(platform, value)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    uriHandler.openUri(fullUrl)
                } catch (e: Exception){
                    // Optionally show a Toast message if opening fails
                    // android.widget.Toast.makeText(context, "Could not open link: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = platform.icon,
            contentDescription = platform.displayName,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMyProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (bio: String?, socialLinks: Map<String, String>, profileImageUrl: String?) -> Unit,
    isSaving: Boolean
) {
    var bio by remember(user.bio) { mutableStateOf(user.bio ?: "") }
    var profileImageUrl by remember(user.profileImageUrl) { mutableStateOf(user.profileImageUrl ?: "") }

    val socialLinksMutableState = remember {
        mutableStateMapOf<String, String>().apply {
            SocialPlatform.platformOrder.forEach { platform ->
                put(platform.key, user.socialMediaLinks?.get(platform.key) ?: "")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 160.dp), // Adjusted height
                    maxLines = 5
                )
                Spacer(Modifier.height(12.dp))
                 OutlinedTextField(
                    value = profileImageUrl,
                    onValueChange = { profileImageUrl = it },
                    label = { Text("Profile Image URL (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Social Links", style = MaterialTheme.typography.titleSmall)
                SocialPlatform.platformOrder.forEach { platform ->
                    OutlinedTextField(
                        value = socialLinksMutableState[platform.key] ?: "",
                        onValueChange = { socialLinksMutableState[platform.key] = it },
                        label = { Text(platform.displayName) },
                        placeholder = { Text(platform.hint) },
                        leadingIcon = { Icon(platform.icon, contentDescription = platform.displayName, modifier = Modifier.size(20.dp))},
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        singleLine = true
                    )
                }
                if (isSaving) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalLinks = socialLinksMutableState.filterValues { it.isNotBlank() }
                onSave(bio.ifBlank { null }, finalLinks, profileImageUrl.ifBlank { null })
            }, enabled = !isSaving) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MyProfileScreenPreview() {
    SockAppTheme {
         Text("MyProfileScreen Preview (Needs ViewModel with User Data)")
    }
}

@Preview
@Composable
fun EditMyProfileDialogPreview() {
    SockAppTheme {
        EditMyProfileDialog(
            user = User(
                username = "PreviewUser",
                bio = "This is my bio.",
                socialMediaLinks = mapOf(SocialPlatform.TWITTER.key to "userX", SocialPlatform.WEBSITE.key to "https://example.com")
            ),
            onDismiss = {},
            onSave = { _, _, _ -> },
            isSaving = false
        )
    }
}
