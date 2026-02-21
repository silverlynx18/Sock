package com.example.sockapp.ui.screens.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // ktlint-disable no-wildcard-imports
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.sockapp.data.models.group.Group
import com.example.sockapp.data.models.group.GroupRole
import com.example.sockapp.data.models.group.ManagedInviteLink
import com.example.sockapp.viewmodels.GroupViewModel
import com.example.sockapp.viewmodels.ProfileViewModel
import com.example.sockapp.ui.theme.SockAppTheme
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsPageScreen(
    groupId: String,
    groupViewModel: GroupViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onViewMembersList: (groupId: String) -> Unit,
    onInviteMembersClicked: (groupId: String, groupName: String) -> Unit // For navigating to invite flow
) {
    val groupUiState by groupViewModel.uiState.collectAsStateWithLifecycle()
    val profileUiState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val group = groupUiState.currentGroup
    val members = groupUiState.currentGroupMembers
    val currentUserRoleInGroup = groupUiState.currentUserRoleInGroup
    val currentUserProfile = profileUiState.user
    val managedInviteLinks = groupUiState.managedInviteLinks

    val snackbarHostState = remember { SnackbarHostState() }
    val localCoroutineScope = rememberCoroutineScope()

    var showEditGroupDialog by remember { mutableStateOf(false) }
    var showConfirmLeaveDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteGroupDialog by remember { mutableStateOf(false) }
    var showCreateInviteLinkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        if (groupUiState.currentGroup?.groupId != groupId) {
            groupViewModel.listenToGroupDetails(groupId) // Also fetches members and managed links
        }
         if (profileViewModel.auth.currentUser?.uid != null && profileUiState.user == null) {
             profileViewModel.listenToUserProfile(profileViewModel.auth.currentUser!!.uid)
        }
    }

    // Snackbar Handlers
    LaunchedEffect(groupUiState.actionError, groupUiState.generateLinkError) {
        listOfNotNull(groupUiState.actionError, groupUiState.generateLinkError).firstOrNull()?.let {
            localCoroutineScope.launch { snackbarHostState.showSnackbar("Error: $it") }
            groupViewModel.clearErrorsAndMessages()
        }
    }
    LaunchedEffect(groupUiState.actionSuccessMessage, groupUiState.generateLinkSuccess) {
         listOfNotNull(groupUiState.actionSuccessMessage, if(groupUiState.generateLinkSuccess) "Invite link action successful." else null).firstOrNull()?.let {
            localCoroutineScope.launch { snackbarHostState.showSnackbar(it) }
            groupViewModel.clearErrorsAndMessages()
            groupViewModel.resetGenerateLinkStatus()
        }
    }
    LaunchedEffect(profileUiState.updateSuccessMessage) {
        profileUiState.updateSuccessMessage?.let {
            if(it.contains("Default group")) {
                 localCoroutineScope.launch { snackbarHostState.showSnackbar(it) }
                 profileViewModel.clearMessagesAndErrors()
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "Group Details") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = {
                    if (currentUserRoleInGroup?.canManageSettings() == true) {
                        IconButton(onClick = { showEditGroupDialog = true }) { Icon(Icons.Filled.Edit, "Edit Group Details") }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (groupUiState.isLoadingCurrentGroup && group == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (group == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { Text("Group not found.") }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)
            ) {
                // Group Info Section (Banner, Name, Desc, Default Group Toggle, Metadata)
                // ... (existing code for this section from previous step) ...
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(group.bannerImageUrl).crossfade(true).build(), contentDescription = "${group.name} banner", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.medium))
                Spacer(Modifier.height(16.dp))
                Text(group.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Description", style = MaterialTheme.typography.titleMedium)
                Text(group.description ?: "No description.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))

                if (currentUserRoleInGroup != null) { // Only show default group toggle if user is a member
                    if (currentUserProfile?.defaultGroupIdOnOpen == group.groupId) {
                        OutlinedButton(onClick = { profileViewModel.setDefaultGroupOnOpen(null) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.HomeWork, "Clear default group"); Spacer(Modifier.width(8.dp)); Text("Clear as default start page")
                        }
                    } else {
                        Button(onClick = { profileViewModel.setDefaultGroupOnOpen(group.groupId) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.HomeWork, "Set as default group"); Spacer(Modifier.width(8.dp)); Text("Set as default start page")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text("Visibility: ${if (group.isPublic) "Public" else "Private"}", style = MaterialTheme.typography.bodySmall)
                Text("Members: ${group.memberCount}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))


                // Invite Members Button
                 if (currentUserRoleInGroup != null && (currentUserRoleInGroup == GroupRole.ADMIN || currentUserRoleInGroup == GroupRole.OWNER)) {
                    Button(onClick = { onInviteMembersClicked(group.groupId, group.name) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Invite Members")
                        Spacer(Modifier.width(8.dp))
                        Text("Invite New Members")
                    }
                    Spacer(Modifier.height(16.dp))
                }


                // Admin Invite Links Section
                if (currentUserRoleInGroup == GroupRole.ADMIN || currentUserRoleInGroup == GroupRole.OWNER) {
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier=Modifier.fillMaxWidth()){
                        Text("Admin Invite Links", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showCreateInviteLinkDialog = true }) {
                            Icon(Icons.Filled.AddLink, contentDescription = "Create New Invite Link")
                        }
                    }
                    if (groupUiState.isLoadingManagedInviteLinks) {
                        CircularProgressIndicator()
                    } else if (managedInviteLinks.isEmpty()) {
                        Text("No admin-generated invite links for this group yet.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        managedInviteLinks.forEach { link ->
                            ManagedInviteLinkItem(link = link, onRevoke = { groupViewModel.revokeManagedInviteLink(group.groupId, link.linkId) })
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Divider()
                }


                Spacer(Modifier.height(16.dp))
                Text("Members Overview", style = MaterialTheme.typography.titleMedium)
                // ... (existing members overview code) ...
                if (groupUiState.isLoadingCurrentGroupMembers) CircularProgressIndicator() else {
                    members.take(5).forEach { member -> CompactMemberListItem(member = member, onClick = { /* nav to member profile */ }) }
                    if (members.size > 5) TextButton(onClick = { onViewMembersList(groupId) }) { Text("View all ${members.size} members...") }
                    else if (members.isEmpty()) Text("No members yet.", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(24.dp))


                // Group Actions (Leave, Delete, Join)
                // ... (existing actions code) ...
                 if (currentUserRoleInGroup != null) {
                    if (currentUserRoleInGroup != GroupRole.OWNER) {
                        OutlinedButton(onClick = { showConfirmLeaveDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Icon(Icons.Filled.ExitToApp, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp)); Text("Leave Group") }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (currentUserRoleInGroup == GroupRole.OWNER) {
                        Button(onClick = { showConfirmDeleteGroupDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(8.dp)); Text("Delete Group") }
                    }
                } else if (group.isPublic) {
                    Button(onClick = { groupViewModel.joinPublicGroup(groupId) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.GroupAdd, null); Spacer(Modifier.width(8.dp)); Text("Join Group")
                    }
                }

            }
        }
    }

    if (showEditGroupDialog && group != null) { /* ... existing EditGroupDetailsDialog ... */ }
    if (showConfirmLeaveDialog) { /* ... existing leave dialog ... */ }
    if (showConfirmDeleteGroupDialog && group != null) { /* ... existing delete group dialog ... */ }

    if (showCreateInviteLinkDialog && group != null) {
        CreateManagedInviteLinkDialog(
            onDismiss = { showCreateInviteLinkDialog = false },
            onGenerateLink = { role, maxUses, expiresAt ->
                groupViewModel.createManagedInviteLink(group.groupId, role, maxUses, expiresAt)
                showCreateInviteLinkDialog = false
            },
            isGenerating = groupUiState.actionInProgress // Use a specific flag if available like isGeneratingLink
        )
    }
    // --- Re-paste EditGroupDetailsDialog if it was removed by mistake ---
    if (showEditGroupDialog && group != null) {
        EditGroupDetailsDialog(group, { showEditGroupDialog = false }) { name, desc, isPublic, profileUrl, bannerUrl ->
            // TODO: groupViewModel.updateGroupDetails(groupId, name, desc, isPublic, profileUrl, bannerUrl)
            showEditGroupDialog = false
        }
    }
}

@Composable
fun ManagedInviteLinkItem(link: ManagedInviteLink, onRevoke: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val linkUrl = "https://yourapp.com/join/${link.code}" // Replace with your actual base URL

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.padding(12.dp)) {
            SelectionContainer { Text("Code: ${link.code}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
            Text("Role: ${link.roleToAssign.displayName}", style = MaterialTheme.typography.bodySmall)
            Text("Uses: ${link.uses}${if (link.maxUses != null) "/${link.maxUses}" else ""}", style = MaterialTheme.typography.bodySmall)
            link.expiresAt?.let { Text("Expires: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(it.toDate())}", style = MaterialTheme.typography.bodySmall) }
            Text("Active: ${link.isActive}", style = MaterialTheme.typography.bodySmall, color = if(link.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)

            Row(Modifier.padding(top = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { clipboardManager.setText(AnnotatedString(linkUrl)) ; android.widget.Toast.makeText(context, "Link Copied!", android.widget.Toast.LENGTH_SHORT).show() },
                       shape = RoundedCornerShape(8.dp),
                       contentPadding = PaddingValues(horizontal=12.dp, vertical = 8.dp)
                ) { Text("Copy Link") }

                if (link.isActive) {
                    TextButton(onClick = onRevoke, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Revoke")
                    }
                } else {
                     Text("Link Revoked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateManagedInviteLinkDialog(
    onDismiss: () -> Unit,
    onGenerateLink: (role: GroupRole, maxUses: Long?, expiresAt: Timestamp?) -> Unit,
    isGenerating: Boolean
) {
    var selectedRole by remember { mutableStateOf(GroupRole.MEMBER) }
    var maxUsesInput by remember { mutableStateOf("") } // String for TextField
    var expiryOption by remember { mutableStateOf<String>("Never") } // "Never", "1 Day", "7 Days", "Custom"
    // TODO: Add state and UI for custom date/time picker if "Custom" is selected

    val expiryOptions = listOf("Never", "1 Hour", "1 Day", "7 Days", "30 Days") // Removed "Custom" for simplicity now

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate New Invite Link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Role Selection Dropdown
                var roleExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = roleExpanded, onExpandedChange = { roleExpanded = !roleExpanded }) {
                    OutlinedTextField(
                        value = selectedRole.displayName,
                        onValueChange = {}, readOnly = true, label = { Text("Assign Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = roleExpanded, onDismissRequest = { roleExpanded = false }) {
                        GroupRole.entries.filter { it != GroupRole.OWNER }.forEach { role -> // Cannot invite as Owner
                            DropdownMenuItem(text = { Text(role.displayName) }, onClick = { selectedRole = role; roleExpanded = false })
                        }
                    }
                }

                // Max Uses
                OutlinedTextField(
                    value = maxUsesInput,
                    onValueChange = { maxUsesInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Max Uses (Optional, 0 for unlimited)") },
                    placeholder = {Text("e.g., 100")},
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                // Expiry Selection Dropdown
                var expiryExpanded by remember { mutableStateOf(false) }
                 ExposedDropdownMenuBox(expanded = expiryExpanded, onExpandedChange = { expiryExpanded = !expiryExpanded }) {
                    OutlinedTextField(
                        value = expiryOption,
                        onValueChange = {}, readOnly = true, label = { Text("Link Expires") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expiryExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expiryExpanded, onDismissRequest = { expiryExpanded = false }) {
                        expiryOptions.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { expiryOption = option; expiryExpanded = false })
                        }
                    }
                }
                 if (isGenerating) { CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally)) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val maxUsesLong = maxUsesInput.toLongOrNull()?.takeIf { it > 0 }
                    val expiresAtTimestamp = when (expiryOption) {
                        "1 Hour" -> Timestamp(Timestamp.now().seconds + 3600, 0)
                        "1 Day" -> Timestamp(Timestamp.now().seconds + 24 * 3600, 0)
                        "7 Days" -> Timestamp(Timestamp.now().seconds + 7 * 24 * 3600, 0)
                        "30 Days" -> Timestamp(Timestamp.now().seconds + 30 * 24 * 3600, 0)
                        else -> null // "Never"
                    }
                    onGenerateLink(selectedRole, maxUsesLong, expiresAtTimestamp)
                },
                enabled = !isGenerating
            ) { Text("Generate") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isGenerating) { Text("Cancel") } }
    )
}


// EditGroupDetailsDialog from previous step
@Composable
fun EditGroupDetailsDialog( group: Group, onDismiss: () -> Unit, onSave: (name: String, description: String?, isPublic: Boolean, profileImageUrl: String?, bannerImageUrl: String?) -> Unit) { /* ... existing code ... */ }

@Preview(showBackground = true)
@Composable
fun GroupDetailsPageScreenPreview() { /* ... existing code ... */ }

@Preview
@Composable
fun CreateManagedInviteLinkDialogPreview() {
    SockAppTheme {
        CreateManagedInviteLinkDialog(onDismiss = {}, onGenerateLink = {_,_,_ ->}, isGenerating = false)
    }
}

@Preview
@Composable
fun ManagedInviteLinkItemPreview() {
    SockAppTheme {
        ManagedInviteLinkItem(link = ManagedInviteLink(linkId = "1", code="ABCDE", groupName="Test Group", createdBy = "user1", uses=5, maxUses = 10, isActive = true, expiresAt = Timestamp(Timestamp.now().seconds + 36000, 0)), onRevoke = {})
    }
}
