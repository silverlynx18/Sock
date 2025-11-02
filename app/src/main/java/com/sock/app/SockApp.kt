package com.sock.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sock.app.model.AvailabilityStatus
import com.sock.app.model.GroupSummary
import com.sock.app.model.InvitationSummary
import com.sock.app.navigation.SockDestinations
import com.sock.app.navigation.SockNavHost
import com.sock.app.navigation.SockTopLevelDestination
import com.sock.app.ui.SockAppState
import com.sock.app.ui.previewSockUiState
import com.sock.app.ui.components.RightNavigationRail
import com.sock.app.ui.rememberSockAppState
import com.sock.app.ui.theme.SockTheme

@Composable
fun SockApp(
    appState: SockAppState = rememberSockAppState()
) {
    SockTheme {
        var uiState by remember { mutableStateOf(previewSockUiState()) }
        var navExpanded by rememberSaveable { mutableStateOf(false) }

        val currentDestination = appState.currentDestination
        val currentTopLevelDestination = appState.currentTopLevelDestination
        val isDashboard = currentTopLevelDestination == SockTopLevelDestination.DASHBOARD
        val isTopLevel = currentTopLevelDestination != null
        val showNavRail = isDashboard || navExpanded

        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        ColumnWithTopBar(
                            title = when {
                                currentDestination?.route == SockDestinations.INVITATIONS_ROUTE -> "Invitations"
                                currentDestination?.route?.startsWith("group/") == true -> "Group"
                                currentTopLevelDestination != null -> currentTopLevelDestination.label
                                else -> "Sock"
                            },
                            showBackButton = !isTopLevel,
                            onBackClick = appState::onBackClick,
                            navExpanded = navExpanded,
                            onToggleNavigation = { navExpanded = !navExpanded }
                        ) { innerPadding ->
                            SockNavHost(
                                appState = appState,
                                uiState = uiState,
                                onUpdateGlobalStatus = {
                                    uiState = uiState.copy(
                                        globalStatus = nextStatus(uiState.globalStatus)
                                    )
                                },
                                onUpdateGroupStatus = { group ->
                                    uiState = uiState.copy(
                                        groupsManagedByUser = uiState.groupsManagedByUser.updateGroup(group.id) {
                                            it.copy(status = nextStatus(it.status))
                                        },
                                        groupsMemberOf = uiState.groupsMemberOf.updateGroup(group.id) {
                                            it.copy(status = nextStatus(it.status))
                                        }
                                    )
                                },
                                onRevertGroupStatus = { group ->
                                    uiState = uiState.copy(
                                        groupsManagedByUser = uiState.groupsManagedByUser.updateGroup(group.id) {
                                            it.copy(status = uiState.globalStatus)
                                        },
                                        groupsMemberOf = uiState.groupsMemberOf.updateGroup(group.id) {
                                            it.copy(status = uiState.globalStatus)
                                        }
                                    )
                                },
                                onAcceptInvitation = { invitation ->
                                    uiState = uiState.copy(
                                        invitations = uiState.invitations.filterNot { it.id == invitation.id },
                                        groupsMemberOf = uiState.groupsMemberOf + invitation.toGroupSummary(uiState.globalStatus)
                                    )
                                },
                                onDeclineInvitation = { invitation ->
                                    uiState = uiState.copy(
                                        invitations = uiState.invitations.filterNot { it.id == invitation.id }
                                    )
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }

                    if (showNavRail) {
                        RightNavigationRail(
                            destinations = appState.topLevelDestinations,
                            currentDestination = currentTopLevelDestination,
                            expanded = navExpanded,
                            onDestinationSelected = appState::navigateToTopLevelDestination,
                            modifier = Modifier
                                .padding(end = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnWithTopBar(
    title: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    navExpanded: Boolean,
    onToggleNavigation: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text(text = title) },
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = {
                IconButton(onClick = onToggleNavigation) {
                    Icon(
                        imageVector = Icons.Outlined.Weekend,
                        contentDescription = if (navExpanded) "Collapse navigation" else "Expand navigation"
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        content(Modifier.fillMaxSize())
    }
}

private fun List<GroupSummary>.updateGroup(
    groupId: String,
    transform: (GroupSummary) -> GroupSummary
): List<GroupSummary> = map { group ->
    if (group.id == groupId) transform(group) else group
}

private fun nextStatus(current: AvailabilityStatus): AvailabilityStatus {
    val statuses = AvailabilityStatus.entries
    val currentIndex = statuses.indexOf(current)
    val nextIndex = (currentIndex + 1) % statuses.size
    return statuses[nextIndex]
}

private fun InvitationSummary.toGroupSummary(globalStatus: AvailabilityStatus): GroupSummary =
    GroupSummary(
        id = id,
        name = groupName,
        memberCount = 1,
        primaryColor = 0xFF4E342E,
        secondaryColor = 0xFF260E04,
        status = globalStatus
    )
