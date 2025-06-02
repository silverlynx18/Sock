package com.example.sockapp.ui.screens.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sockapp.viewmodels.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    groupViewModel: GroupViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onGroupCreatedSuccessfully: (groupId: String?) -> Unit // groupId might not be available directly
) {
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    // Add states for profileImageUrl and bannerImageUrl if allowing URL input or image picking
    var profileImageUrl by remember { mutableStateOf("") } // Optional URL input
    var bannerImageUrl by remember { mutableStateOf("") }  // Optional URL input

    val uiState by groupViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.createGroupSuccess) {
        if (uiState.createGroupSuccess) {
            snackbarHostState.showSnackbar("Group created successfully!")
            // The actual groupId isn't directly returned by the createGroup call in VM as designed.
            // The success state just indicates the call completed.
            // Navigation or further actions might need to rely on refreshed group lists.
            onGroupCreatedSuccessfully(null) // Indicate success, specific groupId handling might be post-navigation
            groupViewModel.resetCreateGroupStatus() // Reset status in VM
        }
    }

    LaunchedEffect(uiState.createGroupError) {
        uiState.createGroupError?.let {
            snackbarHostState.showSnackbar("Error: $it")
            groupViewModel.clearErrorsAndMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Group") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Set up your new group", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Group Name*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = groupName.isBlank() && uiState.createGroupError != null // Basic validation indication
            )

            OutlinedTextField(
                value = groupDescription,
                onValueChange = { groupDescription = it },
                label = { Text("Group Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
            OutlinedTextField(
                value = profileImageUrl,
                onValueChange = { profileImageUrl = it },
                label = { Text("Profile Image URL (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = bannerImageUrl,
                onValueChange = { bannerImageUrl = it },
                label = { Text("Banner Image URL (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )


            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isPublic,
                    onCheckedChange = { isPublic = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Public Group (visible to everyone)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isCreatingGroup) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            groupViewModel.createGroup(
                                name = groupName,
                                description = groupDescription.ifBlank { null },
                                isPublic = isPublic,
                                profileImageUrl = profileImageUrl.ifBlank { null },
                                bannerImageUrl = bannerImageUrl.ifBlank { null }
                            )
                        } else {
                             // Trigger UI error or rely on ViewModel's error state for blank name
                             groupViewModel.resetCreateGroupStatus() // Clear previous errors
                             groupViewModel._uiState.value = uiState.copy(createGroupError = "Group name cannot be empty.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Create Group")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateGroupScreenPreview() {
    MaterialTheme {
        CreateGroupScreen(
            onNavigateBack = {},
            onGroupCreatedSuccessfully = {}
        )
    }
}
