package com.example.sockapp.ui.screens.group

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sockapp.data.models.group.Group
import com.example.sockapp.data.models.group.GroupRole
import com.example.sockapp.viewmodels.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPageScreen(
    groupId: String,
    groupViewModel: GroupViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToGroupSettings: (groupId: String) -> Unit, // Example for settings/details page
    onNavigateToGroupMembersList: (groupId: String) -> Unit // Example for a dedicated members list page
) {
    val uiState by groupViewModel.uiState.collectAsState()
    val group = uiState.currentGroup
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        groupViewModel.listenToGroupDetails(groupId)
        // groupViewModel.listenToGroupMembers(groupId) // Already called by listenToGroupDetails if group is not null
    }

    // Cleanup listener when Composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            groupViewModel.stopListeningToGroupDetails()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar("Error: $it")
            groupViewModel.clearErrorsAndMessages()
        }
    }
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let {
            snackbarHostState.showSnackbar("Action Error: $it")
            groupViewModel.clearErrorsAndMessages()
        }
    }
     LaunchedEffect(uiState.actionSuccessMessage) {
        uiState.actionSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            groupViewModel.clearErrorsAndMessages()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Group") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (group != null) {
                        // Show settings/manage icon if user has rights (e.g., admin/owner)
                        if (uiState.currentUserRoleInGroup?.canManageSettings() == true) {
                            IconButton(onClick = { onNavigateToGroupSettings(groupId) }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Group Settings")
                            }
                        }
                        // Generic options menu
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View Members") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToGroupMembersList(groupId)
                                }
                            )
                            if (uiState.currentUserRoleInGroup != GroupRole.OWNER && uiState.currentGroupMembers.any {it.userId == groupViewModel.auth.currentUser?.uid}) { // if member and not owner
                                DropdownMenuItem(
                                    text = { Text("Leave Group", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        groupViewModel.leaveGroup(groupId)
                                        // Potentially navigate back after leaving if successful
                                    }
                                )
                            }
                            // Add more items like "Report Group" etc.
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoadingCurrentGroup && group == null) {
                CircularProgressIndicator()
            } else if (group == null) {
                Text("Group not found or error loading.")
            } else {
                // Main content of the group page
                // This could be a Column with various sections, or a TabRow for different views (Chat, Files, About)
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Welcome to ${group.name}!", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(group.description ?: "No description.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Member Count: ${group.memberCount}", style = MaterialTheme.typography.bodySmall)
                    Text("Your Role: ${uiState.currentUserRoleInGroup?.displayName ?: "Not a member / Loading..."}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Placeholder for group content (e.g., chat messages, posts feed)
                    Text("Group content placeholder...", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Display a few members (example using CompactMemberListItem)
                    if (uiState.isLoadingCurrentGroupMembers) {
                        CircularProgressIndicator()
                    } else if (uiState.currentGroupMembers.isNotEmpty()) {
                        Text("Members:", style = MaterialTheme.typography.titleMedium)
                        uiState.currentGroupMembers.take(3).forEach { member ->
                            CompactMemberListItem(member = member, onClick = { /* Navigate to member profile */ })
                        }
                        if (uiState.currentGroupMembers.size > 3) {
                            TextButton(onClick = { onNavigateToGroupMembersList(groupId) }) {
                                Text("View all ${uiState.currentGroupMembers.size} members")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GroupPageScreenPreview() {
    MaterialTheme {
        // For preview, you might need to mock the ViewModel or its state.
        // This basic preview won't show much dynamic data.
        GroupPageScreen(
            groupId = "previewGroupId",
            onNavigateBack = {},
            onNavigateToGroupSettings = {},
            onNavigateToGroupMembersList = {}
        )
    }
}
