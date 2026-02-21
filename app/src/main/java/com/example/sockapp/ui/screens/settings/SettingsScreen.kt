package com.example.sockapp.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ReceiptLong // For Terms
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HomeWork // For Default Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset // For Change Password
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy // For Privacy Policy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sockapp.BuildConfig
import com.example.sockapp.data.models.profile.NotificationType
import com.example.sockapp.ui.components.settings.AccountDeletionConfirmationDialogStep1
import com.example.sockapp.ui.components.settings.AccountDeletionConfirmationDialogStep2
import com.example.sockapp.ui.components.settings.SettingsItem
import com.example.sockapp.ui.components.settings.SettingsSectionTitle
import com.example.sockapp.ui.theme.SockAppTheme
import com.example.sockapp.viewmodels.AuthViewModel
import com.example.sockapp.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch // For snackbar coroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profileViewModel: ProfileViewModel,
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onLoggedOutCompletely: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToGroupSelection: () -> Unit // For choosing default group
) {
    var showDeleteConfirmStep1 by remember { mutableStateOf(false) }
    var showDeleteConfirmStep2 by remember { mutableStateOf(false) }

    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val user = profileState.user

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val localCoroutineScope = rememberCoroutineScope()

    LaunchedEffect(profileState.accountDeletionSuccess) {
        if (profileState.accountDeletionSuccess) {
            showDeleteConfirmStep1 = false
            showDeleteConfirmStep2 = false
            profileViewModel.clearAccountDeletionStatus()
            localCoroutineScope.launch { snackbarHostState.showSnackbar("Account successfully deleted. Logging out...") }
            onLoggedOutCompletely()
        }
    }

    LaunchedEffect(authViewModel.passwordResetEmailSent) {
        if (authViewModel.passwordResetEmailSent) {
            localCoroutineScope.launch { snackbarHostState.showSnackbar("Password reset email sent to ${user?.email ?: "your email address"}.") }
            authViewModel.clearPasswordResetStatus()
        }
    }
    LaunchedEffect(authViewModel.error) {
        authViewModel.error?.let {
            if (it.contains("Password reset failed", ignoreCase = true)) {
                localCoroutineScope.launch { snackbarHostState.showSnackbar(it) }
                authViewModel.clearErrors()
            }
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


    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // --- Account Section ---
            item { SettingsSectionTitle("Account") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Edit,
                    title = "Edit Profile",
                    subtitle = "Update your personal information",
                    onClick = onNavigateToEditProfile
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.LockReset,
                    title = "Change Password",
                    subtitle = "Send a password reset link to your email",
                    onClick = {
                        val email = user?.email
                        if (email != null) {
                            authViewModel.sendPasswordResetEmail(email)
                        } else {
                            localCoroutineScope.launch { snackbarHostState.showSnackbar("Your email address is not available.")}
                        }
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.DeleteForever,
                    title = "Delete Account",
                    subtitle = "Permanently remove your account and data",
                    titleColor = MaterialTheme.colorScheme.error,
                    iconColor = MaterialTheme.colorScheme.error,
                    onClick = { showDeleteConfirmStep1 = true }
                )
            }

            // --- App Behavior Section ---
            item { SettingsSectionTitle("App Behavior") }
            item {
                val currentDefaultGroupInfo = if (user?.defaultGroupIdOnOpen != null) {
                    // In a real app, you'd fetch the group name here using the ID
                    // For now, just showing the ID.
                    "Current: Group ID ${user.defaultGroupIdOnOpen}"
                } else {
                    "Current: None (opens Dashboard)"
                }
                SettingsItem(
                    icon = Icons.Filled.HomeWork,
                    title = "Default View on App Open",
                    subtitle = currentDefaultGroupInfo,
                    onClick = onNavigateToGroupSelection // This would navigate to a screen where user can pick a group
                )
            }
            if (user?.defaultGroupIdOnOpen != null) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { profileViewModel.setDefaultGroupOnOpen(null) }) {
                            Text("Clear Default Group")
                        }
                    }
                }
            }


            // --- Notifications Section ---
            item { SettingsSectionTitle("Notifications") }
            // Master toggle (optional)
            val allNotificationsEnabled = user?.notificationPreferences?.values?.all { it } ?: true
            item {
                SettingsItem(
                    icon = Icons.Filled.Notifications,
                    title = "All Push Notifications",
                    subtitle = if(allNotificationsEnabled) "Enabled" else "Some disabled",
                    trailingContent = {
                        Switch(
                            checked = allNotificationsEnabled,
                            onCheckedChange = { enabled ->
                                val newPrefs = NotificationType.entries.associate { it.key to enabled }
                                profileViewModel.updateNotificationPreferences(newPrefs)
                            }
                        )
                    }
                )
            }
            // Individual Toggles
            NotificationType.entries.forEach { notificationType ->
                item {
                    val isEnabled = user?.notificationPreferences?.get(notificationType.key) ?: notificationType.defaultEnabled
                    SettingsItem(
                        icon = null, // No icon for sub-items, or provide specific ones
                        title = notificationType.displayName,
                        subtitle = notificationType.description,
                        modifier = Modifier.padding(start = 16.dp), // Indent sub-items
                        trailingContent = {
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { enabled ->
                                    profileViewModel.updateSingleNotificationPreference(notificationType, enabled)
                                }
                            )
                        }
                    )
                }
            }


            // --- About Section ---
            item { SettingsSectionTitle("About") }
            item {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "App Version",
                    subtitle = BuildConfig.VERSION_NAME
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.Policy,
                    title = "Privacy Policy",
                    onClick = { openUrl(context, "https://example.com/privacy") }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "Terms of Service",
                    onClick = { openUrl(context, "https://example.com/terms") }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    title = "Help & Support",
                    onClick = { openUrl(context, "https://example.com/support") }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
            item {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Logout",
                    onClick = {
                        authViewModel.logout()
                        onLoggedOutCompletely()
                    },
                    showDivider = false
                )
            }
        }
    }

    if (showDeleteConfirmStep1) {
        AccountDeletionConfirmationDialogStep1(
            onConfirm = { showDeleteConfirmStep1 = false; showDeleteConfirmStep2 = true },
            onDismiss = { showDeleteConfirmStep1 = false }
        )
    }
    if (showDeleteConfirmStep2) {
        AccountDeletionConfirmationDialogStep2(
            isLoading = profileState.accountDeletionLoading,
            errorMessage = profileState.accountDeletionError,
            onConfirmDeletion = { profileViewModel.deleteAccount() },
            onDismiss = { showDeleteConfirmStep2 = false; profileViewModel.clearAccountDeletionStatus() }
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SockAppTheme {
        SettingsScreen(
            profileViewModel = ProfileViewModel(),
            authViewModel = AuthViewModel(),
            onNavigateBack = {},
            onLoggedOutCompletely = {},
            onNavigateToEditProfile = {},
            onNavigateToGroupSelection = {}
        )
    }
}
