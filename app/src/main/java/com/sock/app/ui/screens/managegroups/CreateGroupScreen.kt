package com.sock.app.ui.screens.managegroups

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sock.app.ui.viewmodel.ManageGroupsViewModel

@Composable
fun CreateGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupCreated: (String, String, String) -> Unit // groupId, inviteLinkCode, groupName
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

    var groupName by remember { mutableStateOf("") }
    var selectedPrimaryColor by remember { mutableStateOf("#6200EE") }
    var selectedSecondaryColor by remember { mutableStateOf("#03DAC6") }

    val colorOptions = listOf(
        "#6200EE" to "#03DAC6", // Purple/Teal
        "#1976D2" to "#03A9F4", // Blue/Light Blue
        "#388E3C" to "#66BB6A", // Green/Light Green
        "#F57C00" to "#FFB74D", // Orange/Light Orange
        "#C2185B" to "#F48FB1", // Pink/Light Pink
        "#7B1FA2" to "#BA68C8", // Purple/Light Purple
        "#00796B" to "#4DB6AC", // Teal/Light Teal
        "#D32F2F" to "#EF5350"  // Red/Light Red
    )

    LaunchedEffect(Unit) {
        viewModel.clearError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Group") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create a new group to coordinate with roommates and friends",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Group Name
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name") },
                placeholder = { Text("e.g., Apartment 3B, College Friends") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Primary Color Selection
            Text(
                text = "Primary Color",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorOptions.forEach { (primary, _) ->
                    ColorOption(
                        color = Color(android.graphics.Color.parseColor(primary)),
                        isSelected = selectedPrimaryColor == primary,
                        onClick = { selectedPrimaryColor = primary }
                    )
                }
            }

            // Secondary Color Selection
            Text(
                text = "Secondary Color",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorOptions.forEach { (_, secondary) ->
                    ColorOption(
                        color = Color(android.graphics.Color.parseColor(secondary)),
                        isSelected = selectedSecondaryColor == secondary,
                        onClick = { selectedSecondaryColor = secondary }
                    )
                }
            }

            // Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(android.graphics.Color.parseColor(selectedPrimaryColor))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = groupName.ifBlank { "Group Name" },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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

            // Create Button
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        viewModel.createGroup(
                            groupName = groupName,
                            primaryColor = selectedPrimaryColor,
                            secondaryColor = selectedSecondaryColor,
                            onSuccess = { groupId, inviteLinkCode ->
                                onGroupCreated(groupId, inviteLinkCode, groupName)
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && groupName.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Create Group")
                }
            }
        }
    }
}

@Composable
private fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable { onClick() }
    ) {
        Surface(
            color = color,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxSize(),
                    border = if (isSelected) {
                BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            }
        ) {}
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
