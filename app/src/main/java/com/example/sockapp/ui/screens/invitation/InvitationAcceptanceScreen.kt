package com.example.sockapp.ui.screens.invitation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HighlightOff
 // import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.outlined.HourglassEmpty // Using outlined version for pending
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sockapp.data.models.invitation.Invitation
import com.example.sockapp.data.models.invitation.InvitationStatus
import com.example.sockapp.viewmodels.InvitationViewModel
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitationAcceptanceScreen(
    invitationId: String?, // Nullable if ID might not be present initially
    invitationViewModel: InvitationViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onInvitationProcessed: () -> Unit // To navigate away or refresh after action
) {
    val uiState by invitationViewModel.uiState.collectAsState()
    val invitation = uiState.viewedInvitationDetails
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoadingAction = uiState.actionInProgressMap[invitation?.invitationId ?: ""] ?: false


    LaunchedEffect(invitationId) {
        if (invitationId != null) {
            invitationViewModel.loadInvitationDetails(invitationId)
        } else {
            // Handle case where invitationId is null, maybe show error or navigate back
        }
    }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            invitationViewModel.clearActionMessage()
            if (invitation?.status == InvitationStatus.ACCEPTED || invitation?.status == InvitationStatus.DECLINED) {
                onInvitationProcessed()
            }
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
                title = { Text("Group Invitation") },
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
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading && invitation == null) {
                CircularProgressIndicator()
            } else if (invitation == null) {
                Text("Invitation details could not be loaded or are invalid.", textAlign = TextAlign.Center)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(invitation.groupImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${invitation.groupName ?: "Group"} image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )

                    Text(
                        text = "You've been invited to join",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = invitation.groupName ?: "a group",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    invitation.inviterName?.let {
                        Text(
                            text = "by ${it}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "Role: ${invitation.roleToAssign.displayName}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    when (invitation.status) {
                        InvitationStatus.PENDING -> {
                            if (isLoadingAction) {
                                CircularProgressIndicator()
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                                ) {
                                    Button(
                                        onClick = { invitationViewModel.acceptInvitation(invitation.invitationId, invitation.groupName) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = "Accept")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Accept Invitation")
                                    }
                                    OutlinedButton(
                                        onClick = { invitationViewModel.declineInvitation(invitation.invitationId, invitation.groupName) }
                                    ) {
                                         Icon(Icons.Filled.HighlightOff, contentDescription = "Decline")
                                         Spacer(modifier = Modifier.width(8.dp))
                                        Text("Decline")
                                    }
                                }
                            }
                        }
                        InvitationStatus.ACCEPTED -> StatusDisplayChip("Accepted", Icons.Filled.CheckCircle, MaterialTheme.colorScheme.secondary)
                        InvitationStatus.DECLINED -> StatusDisplayChip("Declined", Icons.Filled.HighlightOff, MaterialTheme.colorScheme.error)
                        InvitationStatus.EXPIRED -> StatusDisplayChip("Expired", Icons.Outlined.HourglassEmpty, MaterialTheme.colorScheme.onSurfaceVariant)
                        InvitationStatus.REVOKED -> StatusDisplayChip("Revoked", Icons.Outlined.HourglassEmpty, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDisplayChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = text, tint = color, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Preview(showBackground = true)
@Composable
fun InvitationAcceptanceScreenPreview_Pending() {
    MaterialTheme {
        // Mock ViewModel that provides a sample PENDING invitation
        val mockViewModel = InvitationViewModel() // In real preview, inject state
        // This is a simplified way to set preview state if _uiState was public, not recommended for actual code.
        // For a proper preview, you'd use a Hilt/Koin preview setup or pass state directly to a stateless version of the screen.
        // (mockViewModel._uiState as MutableStateFlow).value = InvitationUiState(
        //     viewedInvitationDetails = Invitation(
        //         invitationId = "invite123",
        //         groupId = "groupABC",
        //         groupName = "The Cool Coders Club",
        //         groupImageUrl = null, // Add a sample URL for image preview
        //         inviterId = "userSender",
        //         inviterName = "Alex The Inviter",
        //         status = InvitationStatus.PENDING,
        //         roleToAssign = com.example.sockapp.data.models.group.GroupRole.MEMBER,
        //         createdAt = Timestamp.now()
        //     )
        // )
        InvitationAcceptanceScreen(
            invitationId = "invite123",
            invitationViewModel = mockViewModel,
            onNavigateBack = {},
            onInvitationProcessed = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InvitationAcceptanceScreenPreview_Accepted() {
     MaterialTheme {
        val mockViewModel = InvitationViewModel()
        // (mockViewModel._uiState as MutableStateFlow).value = InvitationUiState(
        //     viewedInvitationDetails = Invitation(invitationId = "invite123", groupName = "Tech Innovators", status = InvitationStatus.ACCEPTED)
        // )
        InvitationAcceptanceScreen(
            invitationId = "invite123",
            invitationViewModel = mockViewModel,
            onNavigateBack = {},
            onInvitationProcessed = {}
        )
    }
}
