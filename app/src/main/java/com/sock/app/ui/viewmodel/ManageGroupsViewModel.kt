package com.sock.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sock.app.data.model.Group
import com.sock.app.data.model.Invitation
import com.sock.app.data.repository.AuthRepository
import com.sock.app.data.repository.GroupRepository
import com.sock.app.data.repository.InvitationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManageGroupsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val groups: List<Group> = emptyList(),
    val pendingInvitations: List<Invitation> = emptyList(),
    val managedGroups: List<Group> = emptyList(),
    val memberGroups: List<Group> = emptyList()
)

class ManageGroupsViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val groupRepository = GroupRepository()
    private val invitationRepository = InvitationRepository()

    private val _uiState = MutableStateFlow(ManageGroupsUiState())
    val uiState: StateFlow<ManageGroupsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "User not authenticated"
                )
                return@launch
            }

            // Load groups
            val groupsResult = groupRepository.getUserGroups(userId)
            val groups = groupsResult.getOrNull() ?: emptyList()

            // Load pending invitations
            val invitationsResult = invitationRepository.getPendingInvitations(userId)
            val invitations = invitationsResult.getOrNull() ?: emptyList()

            // Separate managed and member groups
            val managedGroups = groups.filter { it.ownerId == userId || 
                it.members[userId]?.role == "admin" || it.members[userId]?.role == "owner" }
            val memberGroups = groups.filter { it !in managedGroups }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                groups = groups,
                pendingInvitations = invitations,
                managedGroups = managedGroups,
                memberGroups = memberGroups
            )
        }
    }

    fun createGroup(
        groupName: String,
        primaryColor: String,
        secondaryColor: String,
        groupProfilePictureUrl: String? = null,
        onSuccess: (String, String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val result = groupRepository.createGroup(
                groupName = groupName,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                groupProfilePictureUrl = groupProfilePictureUrl
            )

            result.onSuccess { (groupId, inviteLinkCode) ->
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadData() // Refresh groups list
                onSuccess(groupId, inviteLinkCode)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = exception.message ?: "Failed to create group"
                )
            }
        }
    }

    fun acceptInvitation(invitationId: String) {
        viewModelScope.launch {
            val result = invitationRepository.acceptInvitation(invitationId)
            result.onSuccess {
                loadData() // Refresh data
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.message ?: "Failed to accept invitation"
                )
            }
        }
    }

    fun declineInvitation(invitationId: String) {
        viewModelScope.launch {
            val result = invitationRepository.declineInvitation(invitationId)
            result.onSuccess {
                loadData() // Refresh data
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.message ?: "Failed to decline invitation"
                )
            }
        }
    }

    fun refresh() {
        loadData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
