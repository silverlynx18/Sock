package com.sock.app.ui.screens.managegroups

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sock.app.ui.viewmodel.ManageGroupsViewModel

@Composable
fun ManageGroupsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGroup: (String) -> Unit = {},
    onNavigateToGroupDetails: (String) -> Unit = {},
    onCreateGroup: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ManageGroupsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ManageGroupsViewModel(context.applicationContext as Application) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    var showInvitationsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.clearError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Groups") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateGroup) {
                Icon(Icons.Default.Add, contentDescription = "Create Group")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pending Invitations Section
                if (uiState.pendingInvitations.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pending Invitations",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (uiState.pendingInvitations.size > 2) {
                                TextButton(onClick = { showInvitationsDialog = true }) {
                                    Text("View All")
                                }
                            }
                        }
                    }

                    items(uiState.pendingInvitations.take(2)) { invitation ->
                        InvitationCard(
                            invitation = invitation,
                            onAccept = { viewModel.acceptInvitation(invitation.invitationId) },
                            onDecline = { viewModel.declineInvitation(invitation.invitationId) }
                        )
                    }
                }

                // Create Group Card
                item {
                    Card(
                        onClick = onCreateGroup,
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Create a New Group",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Managed Groups Section
                if (uiState.managedGroups.isNotEmpty()) {
                    item {
                        Text(
                            text = "Managed by You",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(uiState.managedGroups) { group ->
                        GroupListItem(
                            group = group,
                            onClick = { onNavigateToGroup(group.groupId) },
                            onDetailsClick = { onNavigateToGroupDetails(group.groupId) }
                        )
                    }
                }

                // Member Groups Section
                if (uiState.memberGroups.isNotEmpty()) {
                    item {
                        Text(
                            text = "Member Of",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(uiState.memberGroups) { group ->
                        GroupListItem(
                            group = group,
                            onClick = { onNavigateToGroup(group.groupId) },
                            onDetailsClick = { onNavigateToGroupDetails(group.groupId) }
                        )
                    }
                }

                // Empty state
                if (uiState.groups.isEmpty() && uiState.pendingInvitations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No groups yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onCreateGroup) {
                                    Text("Create Your First Group")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Error message
        uiState.errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    // All Invitations Dialog
    if (showInvitationsDialog) {
        AlertDialog(
            onDismissRequest = { showInvitationsDialog = false },
            title = { Text("All Invitations") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(uiState.pendingInvitations) { invitation ->
                        InvitationCard(
                            invitation = invitation,
                            onAccept = {
                                viewModel.acceptInvitation(invitation.invitationId)
                                if (uiState.pendingInvitations.size == 1) {
                                    showInvitationsDialog = false
                                }
                            },
                            onDecline = {
                                viewModel.declineInvitation(invitation.invitationId)
                                if (uiState.pendingInvitations.size == 1) {
                                    showInvitationsDialog = false
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInvitationsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun InvitationCard(
    invitation: com.sock.app.data.model.Invitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Invited to join ${invitation.groupName}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Accept")
                }
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Decline")
                }
            }
        }
    }
}

@Composable
private fun GroupListItem(
    group: com.sock.app.data.model.Group,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${group.members.size} member${if (group.members.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDetailsClick) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Group Details"
                )
            }
        }
    }
}
