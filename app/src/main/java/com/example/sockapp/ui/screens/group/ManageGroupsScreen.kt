package com.example.sockapp.ui.screens.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.sockapp.data.models.group.Group
import com.example.sockapp.viewmodels.GroupViewModel
import com.example.sockapp.viewmodels.GroupUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageGroupsScreen(
    groupViewModel: GroupViewModel = viewModel(),
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToGroupPage: (groupId: String) -> Unit
) {
    val uiState by groupViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        groupViewModel.fetchUserGroups()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar("Error: $it")
            groupViewModel.clearErrorsAndMessages()
        }
    }
     LaunchedEffect(uiState.actionSuccessMessage) {
        uiState.actionSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            groupViewModel.clearErrorsAndMessages()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Groups") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Create Group") },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Create new group") },
                onClick = onNavigateToCreateGroup
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
            if (uiState.isLoadingUserGroups && uiState.userGroups.isEmpty()) {
                CircularProgressIndicator()
            } else if (uiState.userGroups.isEmpty()) {
                Text("You are not a member of any groups yet. Create one!")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.userGroups, key = { it.groupId }) { group ->
                        GroupListItem(
                            group = group,
                            onClick = { onNavigateToGroupPage(group.groupId) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ManageGroupsScreenPreview() {
    MaterialTheme {
        // Mock ViewModel for preview if needed, or pass a dummy state
        val previewViewModel = GroupViewModel()
        // Simulate some state for preview
        // previewViewModel._uiState.value = GroupUiState(userGroups = listOf(Group(groupId="1", name="Preview Group", memberCount=10)))

        ManageGroupsScreen(
            groupViewModel = previewViewModel,
            onNavigateToCreateGroup = {},
            onNavigateToGroupPage = {}
        )
    }
}
