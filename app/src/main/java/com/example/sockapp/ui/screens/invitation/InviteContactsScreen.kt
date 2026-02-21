package com.example.sockapp.ui.screens.invitation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment // For SMS
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sockapp.data.models.group.GroupRole
import com.example.sockapp.ui.theme.SockAppTheme
import com.example.sockapp.viewmodels.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteContactsScreen(
    contactsViewModel: ContactsViewModel, // Pass instance
    invitationViewModel: InvitationViewModel, // Pass instance
    groupIdToInviteTo: String, // Group ID must be provided to invite
    groupName: String, // For display and SMS body
    onNavigateBack: () -> Unit
) {
    val contactsUiState by contactsViewModel.uiState.collectAsStateWithLifecycle()
    val invitationUiState by invitationViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- Permission Handling for READ_CONTACTS ---
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        contactsViewModel.setContactsPermissionGranted(isGranted)
        if (isGranted) {
            contactsViewModel.loadPhoneContacts(context.contentResolver)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Contacts permission is required to invite from contacts.")
            }
        }
    }

    LaunchedEffect(Unit) {
        // Check permission on screen entry or when contactsUiState.permissionGranted is false
        if (!contactsUiState.permissionGranted) {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            // If permission already granted, load contacts if not already loaded
            if (contactsUiState.contacts.isEmpty() && !contactsUiState.isLoadingContacts) {
                 contactsViewModel.loadPhoneContacts(context.contentResolver)
            }
        }
    }

    // --- Handle SMS Invite Content ---
    LaunchedEffect(invitationUiState.smsInviteContent) {
        invitationUiState.smsInviteContent?.let { content ->
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${content.phoneNumber}")
                putExtra("sms_body", content.messageBody)
            }
            // Ensure there's an app to handle this intent
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar("No SMS app found.")}
            }
            invitationViewModel.clearSmsInviteContent() // Clear after launching
        }
    }

    // --- Snackbar for errors/messages ---
    LaunchedEffect(contactsUiState.error, invitationUiState.sendInviteError, invitationUiState.smsInviteError, invitationUiState.actionMessage) {
        listOfNotNull(contactsUiState.error, invitationUiState.sendInviteError, invitationUiState.smsInviteError, invitationUiState.actionMessage)
            .firstOrNull()?.let { message ->
            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
            contactsViewModel.clearError()
            invitationViewModel.clearErrors()
            invitationViewModel.clearActionMessage()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite Contacts to $groupName") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = {
                        if (contactsUiState.permissionGranted) contactsViewModel.loadPhoneContacts(context.contentResolver)
                        else contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }, enabled = !contactsUiState.isLoadingContacts && !contactsUiState.isLoadingSockStatuses) {
                        Icon(Icons.Filled.Refresh, "Refresh Contacts")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (!contactsUiState.permissionGranted) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Contacts permission is needed to invite friends.")
                        Button(onClick = { (context as? Activity)?.let {
                            // Open app settings to allow permission manually
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts("package", context.packageName, null)
                            context.startActivity(intent)
                        } }) {
                            Text("Open Settings")
                        }
                    }
                }
                return@Scaffold
            }

            if (contactsUiState.isLoadingContacts) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Loading contacts...")
                }
            } else if (contactsUiState.contacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No contacts found or permission denied.")
                }
            } else {
                if(contactsUiState.isLoadingSockStatuses && contactsUiState.contacts.any { it.sockAppStatus == SockAppUserStatus.LOADING }){
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contactsUiState.contacts, key = { it.id }) { contact ->
                        ContactInviteItem(
                            contact = contact,
                            onInviteExistingUser = { userId ->
                                invitationViewModel.sendGroupInvitation(groupIdToInviteTo, userId, GroupRole.MEMBER)
                            },
                            onInviteNewUserBySms = { phoneNumber ->
                                invitationViewModel.sendSmsInvite(phoneNumber, groupIdToInviteTo, groupName)
                            },
                            isProcessing = invitationUiState.isSendingInvite || invitationUiState.isGeneratingSmsInvite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactInviteItem(
    contact: PhoneContact,
    onInviteExistingUser: (userId: String) -> Unit,
    onInviteNewUserBySms: (phoneNumber: String) -> Unit,
    isProcessing: Boolean
) {
    Card(elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name ?: "Unknown Name", style = MaterialTheme.typography.titleMedium)
                Text(contact.phoneNumber, style = MaterialTheme.typography.bodyMedium)
                when (contact.sockAppStatus) {
                    SockAppUserStatus.IS_USER -> Text("On SockApp (${contact.sockAppDisplayName ?: contact.sockAppUserId})", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    SockAppUserStatus.NOT_USER -> Text("Not on SockApp yet", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    SockAppUserStatus.LOADING -> Text("Checking status...", style = MaterialTheme.typography.bodySmall)
                    SockAppUserStatus.CHECK_FAILED -> Text("Could not check status", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    SockAppUserStatus.UNKNOWN -> Text("Status unknown", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.width(8.dp))
            when (contact.sockAppStatus) {
                SockAppUserStatus.IS_USER -> {
                    Button(
                        onClick = { contact.sockAppUserId?.let { onInviteExistingUser(it) } },
                        enabled = !isProcessing && contact.sockAppUserId != null,
                        modifier = Modifier.height(IntrinsicSize.Min)
                    ) { Icon(Icons.Filled.PersonAdd, "Invite"); Spacer(Modifier.width(4.dp)); Text("Invite") }
                }
                SockAppUserStatus.NOT_USER -> {
                    Button(
                        onClick = { onInviteNewUserBySms(contact.phoneNumber) },
                        enabled = !isProcessing,
                        modifier = Modifier.height(IntrinsicSize.Min)
                    ) { Icon(Icons.Filled.Comment, "SMS"); Spacer(Modifier.width(4.dp)); Text("SMS") }
                }
                SockAppUserStatus.LOADING -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                else -> { /* Can't invite or status check failed */ }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InviteContactsScreenPreview() {
    SockAppTheme {
        // InviteContactsScreen(contactsViewModel = viewModel(), invitationViewModel = viewModel(), groupIdToInviteTo = "group123", groupName="Test Group", onNavigateBack = {})
        Text("InviteContactsScreen Preview (Needs ViewModels & Permissions)")
    }
}

@Preview(showBackground = true)
@Composable
fun ContactInviteItem_IsUser_Preview() {
    SockAppTheme {
        ContactInviteItem(
            contact = PhoneContact("1", "Jane Doe", "+11234567890", SockAppUserStatus.IS_USER, "janeUserId", "JaneD"),
            onInviteExistingUser = {}, onInviteNewUserBySms = {}, isProcessing = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ContactInviteItem_NotUser_Preview() {
    SockAppTheme {
        ContactInviteItem(
            contact = PhoneContact("2", "John Smith", "+10987654321", SockAppUserStatus.NOT_USER),
            onInviteExistingUser = {}, onInviteNewUserBySms = {}, isProcessing = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ContactInviteItem_Loading_Preview() {
    SockAppTheme {
        ContactInviteItem(
            contact = PhoneContact("3", "Loading User", "+15555555555", SockAppUserStatus.LOADING),
            onInviteExistingUser = {}, onInviteNewUserBySms = {}, isProcessing = true
        )
    }
}
