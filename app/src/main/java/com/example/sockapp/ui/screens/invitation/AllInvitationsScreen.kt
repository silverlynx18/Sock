package com.example.sockapp.ui.screens.invitation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sockapp.data.models.invitation.Invitation
import com.example.sockapp.data.models.invitation.InvitationStatus
import com.example.sockapp.viewmodels.InvitationViewModel
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllInvitationsScreen(
    invitationViewModel: InvitationViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onViewInvitation: (invitationId: String) -> Unit // To navigate to InvitationAcceptanceScreen
) {
    val uiState by invitationViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        invitationViewModel.fetchMyPendingInvitations()
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            invitationViewModel.clearActionMessage()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar("Error: $it")
            invitationViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Invitations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            if (uiState.isLoading && uiState.pendingInvitations.isEmpty()) {
                CircularProgressIndicator()
            } else if (uiState.pendingInvitations.isEmpty()) {
                Text("You have no pending invitations.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.pendingInvitations, key = { it.invitationId }) { invitation ->
                        InvitationListItem(
                            invitation = invitation,
                            isLoading = uiState.actionInProgressMap[invitation.invitationId] ?: false,
                            onAccept = {
                                invitationViewModel.acceptInvitation(invitation.invitationId, invitation.groupName)
                            },
                            onDecline = {
                                invitationViewModel.declineInvitation(invitation.invitationId, invitation.groupName)
                            },
                            onClick = {
                                onViewInvitation(invitation.invitationId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AllInvitationsScreenPreview_WithInvites() {
    MaterialTheme {
        val mockViewModel = InvitationViewModel()
        // Simulate some state
        // (mockViewModel._uiState as MutableStateFlow).value = InvitationUiState(
        //     pendingInvitations = listOf(
        //         Invitation(invitationId = "1", groupName = "Group Alpha", inviterName = "Alex", createdAt = Timestamp.now(), status = InvitationStatus.PENDING),
        //         Invitation(invitationId = "2", groupName = "Beta Testers", inviterName = "Beth", createdAt = Timestamp.now(), status = InvitationStatus.PENDING)
        //     )
        // )
        AllInvitationsScreen(
            invitationViewModel = mockViewModel,
            onNavigateBack = {},
            onViewInvitation = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AllInvitationsScreenPreview_NoInvites() {
    MaterialTheme {
        val mockViewModel = InvitationViewModel()
        // (mockViewModel._uiState as MutableStateFlow).value = InvitationUiState(pendingInvitations = emptyList())
         AllInvitationsScreen(
            invitationViewModel = mockViewModel,
            onNavigateBack = {},
            onViewInvitation = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AllInvitationsScreenPreview_Loading() {
    MaterialTheme {
        val mockViewModel = InvitationViewModel()
        // (mockViewModel._uiState as MutableStateFlow).value = InvitationUiState(isLoading = true)
        AllInvitationsScreen(
            invitationViewModel = mockViewModel,
            onNavigateBack = {},
            onViewInvitation = {}
        )
    }
}
