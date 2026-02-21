package com.example.sockapp.ui.screens.invitation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sockapp.data.models.group.GroupRole
import com.example.sockapp.ui.theme.SockAppTheme
import com.example.sockapp.viewmodels.InvitationViewModel
import com.example.sockapp.viewmodels.UserSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteByUsernameScreen(
    invitationViewModel: InvitationViewModel, // Pass instance
    groupIdToInviteTo: String,
    groupName: String, // For display
    onNavigateBack: () -> Unit
) {
    val uiState by invitationViewModel.uiState.collectAsStateWithLifecycle()
    var usernameQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(uiState.sendInviteSuccess, uiState.sendInviteError, uiState.actionMessage) {
        if (uiState.sendInviteSuccess) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Invitation sent successfully!") }
            invitationViewModel.resetSendInviteStatus()
            usernameQuery = "" // Clear query after successful invite
            invitationViewModel.findUsersByUsername("") // Clear results
        }
        uiState.sendInviteError?.let {
            coroutineScope.launch { snackbarHostState.showSnackbar("Error: $it") }
            invitationViewModel.clearErrors()
        }
         uiState.actionMessage?.let { // For general messages from sendGroupInvitation
            coroutineScope.launch { snackbarHostState.showSnackbar(it) }
            invitationViewModel.clearActionMessage()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite to $groupName by Username") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            OutlinedTextField(
                value = usernameQuery,
                onValueChange = {
                    usernameQuery = it
                    searchJob?.cancel() // Cancel previous search if user is typing fast
                    if (it.length >= 2) {
                        searchJob = coroutineScope.launch {
                            delay(300) // Debounce
                            invitationViewModel.findUsersByUsername(it)
                        }
                    } else {
                        invitationViewModel.findUsersByUsername("") // Clear results if query too short
                    }
                },
                label = { Text("Search by username") },
                leadingIcon = { Icon(Icons.Filled.Search, "Search") },
                trailingIcon = {
                    if (usernameQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            usernameQuery = ""
                            invitationViewModel.findUsersByUsername("") // Clear results
                        }) {
                            Icon(Icons.Filled.Clear, "Clear search")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoadingUsernameSearch) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.usernameSearchError != null) {
                Text(uiState.usernameSearchError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.usernameSearchResults.isEmpty() && usernameQuery.length >= 2) {
                Text("No users found matching \"$usernameQuery\".", modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (uiState.usernameSearchResults.isNotEmpty()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.usernameSearchResults, key = { it.userId }) { userResult ->
                        UserSearchResultItem(
                            userResult = userResult,
                            onInvite = {
                                invitationViewModel.sendGroupInvitation(
                                    groupId = groupIdToInviteTo,
                                    inviteeId = userResult.userId,
                                    roleToAssign = GroupRole.MEMBER // Default role
                                )
                            },
                            isProcessingInvite = uiState.isSendingInvite // Generic sending flag
                        )
                    }
                }
            } else if (usernameQuery.isEmpty()){
                 Text("Enter a username to search.", modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
fun UserSearchResultItem(
    userResult: UserSearchResult,
    onInvite: () -> Unit,
    isProcessingInvite: Boolean
) {
    Card(elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(userResult.photoUrl)
                        .placeholder(Icons.Filled.AccountCircle)
                        .error(Icons.Filled.AccountCircle)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${userResult.username} profile image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(userResult.username, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    userResult.displayName?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Button(onClick = onInvite, enabled = !isProcessingInvite, modifier = Modifier.height(IntrinsicSize.Min)) {
                if (isProcessingInvite) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                } else {
                    Icon(Icons.Filled.PersonAdd, "Invite User", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Invite")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InviteByUsernameScreenPreview() {
    SockAppTheme {
        // InviteByUsernameScreen(invitationViewModel = viewModel(), groupIdToInviteTo = "group1", groupName="Test Group", onNavigateBack = {})
        Text("InviteByUsernameScreen Preview (Needs ViewModel)")
    }
}

@Preview(showBackground = true)
@Composable
fun UserSearchResultItemPreview() {
     SockAppTheme {
        UserSearchResultItem(
            userResult = UserSearchResult("uid1", "TestUser", "Test User Display Name", null),
            onInvite = {},
            isProcessingInvite = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UserSearchResultItemProcessingPreview() {
     SockAppTheme {
        UserSearchResultItem(
            userResult = UserSearchResult("uid1", "TestUser", "Test User Display Name", null),
            onInvite = {},
            isProcessingInvite = true
        )
    }
}
