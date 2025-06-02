package com.example.sockapp.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sockapp.data.models.userstatus.UserGeneratedStatusPreset
import com.example.sockapp.ui.components.status.getIconForKey // Reuse helper
import com.example.sockapp.ui.theme.SockAppTheme
import com.example.sockapp.viewmodels.ProfileViewModel
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStatusPresetsScreen(
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val presets = uiState.userStatusPresets
    var showEditDialog by remember { mutableStateOf(false) }
    var currentEditingPreset by remember { mutableStateOf<UserGeneratedStatusPreset?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        profileViewModel.fetchUserStatusPresets()
    }
     LaunchedEffect(uiState.updateSuccessMessage) {
        uiState.updateSuccessMessage?.let {
            if(it.contains("preset")) { // Only show preset related messages
                snackbarHostState.showSnackbar(it)
                profileViewModel.clearMessagesAndErrors()
            }
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
             if(it.contains("preset")) {
                snackbarHostState.showSnackbar("Error: $it")
                profileViewModel.clearMessagesAndErrors()
             }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Status Presets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                currentEditingPreset = null // Clear for new preset
                showEditDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add new status preset")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoadingPresets && presets.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (presets.isEmpty()) {
                Text("You have no saved status presets yet. Tap '+' to create one!", modifier = Modifier.align(Alignment.Center).padding(16.dp))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presets, key = { it.presetId }) { preset ->
                        StatusPresetListItem(
                            preset = preset,
                            onEdit = {
                                currentEditingPreset = preset
                                showEditDialog = true
                            },
                            onDelete = {
                                profileViewModel.deleteUserStatusPreset(preset.presetId)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditStatusPresetDialog(
            preset = currentEditingPreset,
            onDismiss = { showEditDialog = false },
            onSave = { name, text, iconKey ->
                profileViewModel.saveUserStatusPreset(
                    presetName = name,
                    statusText = text,
                    iconKey = iconKey,
                    presetIdToUpdate = currentEditingPreset?.presetId
                )
                showEditDialog = false
            },
            isSaving = uiState.isUpdating
        )
    }
}

@Composable
private fun StatusPresetListItem(
    preset: UserGeneratedStatusPreset,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(1.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(getIconForKey(preset.iconKey), contentDescription = preset.presetName, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(preset.presetName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(preset.statusText, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit Preset")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Preset", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EditStatusPresetDialog(
    preset: UserGeneratedStatusPreset?,
    onDismiss: () -> Unit,
    onSave: (name: String, text: String, iconKey: String) -> Unit,
    isSaving: Boolean
) {
    var name by remember(preset) { mutableStateOf(preset?.presetName ?: "") }
    var text by remember(preset) { mutableStateOf(preset?.statusText ?: "") }
    var iconKey by remember(preset) { mutableStateOf(preset?.iconKey ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preset == null) "Create Status Preset" else "Edit Status Preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name (e.g., Working From Home)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= MAX_CUSTOM_STATUS_LENGTH) text = it },
                    label = { Text("Status Text") },
                    singleLine = true,
                     supportingText = { Text("${text.length}/${MAX_CUSTOM_STATUS_LENGTH}") }
                )
                OutlinedTextField(
                    value = iconKey,
                    onValueChange = { iconKey = it },
                    label = { Text("Icon Key (e.g., coffee, work)") },
                    singleLine = true,
                    leadingIcon = { Icon(getIconForKey(iconKey), "Selected Icon") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, text, iconKey) }, enabled = name.isNotBlank() && text.isNotBlank() && !isSaving) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") } }
    )
}

@Preview(showBackground = true)
@Composable
fun ManageStatusPresetsScreenPreview() {
    SockAppTheme {
        // ManageStatusPresetsScreen(profileViewModel = viewModel(), onNavigateBack = {})
        Text("ManageStatusPresetsScreen Preview (Needs ViewModel)")
    }
}

@Preview
@Composable
fun EditStatusPresetDialogNewPreview(){
    SockAppTheme {
        EditStatusPresetDialog(preset = null, onDismiss = { }, onSave = {_,_,_ ->}, isSaving = false)
    }
}

@Preview
@Composable
fun EditStatusPresetDialogEditPreview(){
    SockAppTheme {
        EditStatusPresetDialog(
            preset = UserGeneratedStatusPreset("id", "uid", "Old Name", "Old text", "coffee", Timestamp.now(), Timestamp.now()),
            onDismiss = { }, onSave = {_,_,_ ->}, isSaving = false)
    }
}
