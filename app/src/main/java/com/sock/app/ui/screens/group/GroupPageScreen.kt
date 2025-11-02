package com.sock.app.ui.screens.group

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sock.app.data.model.StatusType
import com.sock.app.ui.components.StatusDisplay
import com.sock.app.ui.components.StatusSelectionBottomSheet
import com.sock.app.ui.viewmodel.GroupPageViewModel

@Composable
fun GroupPageScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    onNavigateToGroupDetails: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: GroupPageViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return GroupPageViewModel(context.applicationContext as Application, groupId) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    var showStatusSheet by remember { mutableStateOf(false) }
    var showRevertDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.clearError()
    }

    val group = uiState.group
    val groupColor = group?.let { Color(android.graphics.Color.parseColor(it.secondaryColor)) }
        ?: MaterialTheme.colorScheme.secondary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = group?.name ?: "Group",
                        modifier = Modifier.clickable { onNavigateToGroupDetails() }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = groupColor
                )
            )
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
        } else if (group == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Group not found",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Your Status Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = groupColor.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Your Status for This Group",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        StatusDisplay(
                            status = uiState.userStatus,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showStatusSheet = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Update Status")
                            }

                            if (uiState.userStatus != null && 
                                uiState.currentUser?.groupSpecificStatuses?.containsKey(groupId) == true) {
                                OutlinedButton(
                                    onClick = { showRevertDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Revert to Global")
                                }
                            }
                        }
                    }
                }

                // Members List
                Text(
                    text = "Members",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.membersWithStatus) { memberWithStatus ->
                        MemberCard(
                            member = memberWithStatus.member,
                            status = memberWithStatus.status,
                            onClick = { onNavigateToUserProfile(memberWithStatus.userId) }
                        )
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
        }
    }

    // Status Selection Bottom Sheet
    if (showStatusSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStatusSheet = false }
        ) {
            StatusSelectionBottomSheet(
                onStatusSelected = { status ->
                    viewModel.updateGroupStatus(groupId, status)
                },
                onDismiss = { showStatusSheet = false }
            )
        }
    }

    // Revert to Global Dialog
    if (showRevertDialog) {
        AlertDialog(
            onDismissRequest = { showRevertDialog = false },
            title = { Text("Revert to Global Status") },
            text = { Text("Are you sure you want to revert to your global status for this group?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.revertToGlobalStatus(groupId)
                        showRevertDialog = false
                    }
                ) {
                    Text("Revert")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevertDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MemberCard(
    member: com.sock.app.data.model.GroupMember,
    status: StatusType?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.username.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = member.username,
                    style = MaterialTheme.typography.bodyLarge
                )
                StatusDisplay(
                    status = status,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Role badge
            if (member.role == "owner" || member.role == "admin") {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = member.role.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
