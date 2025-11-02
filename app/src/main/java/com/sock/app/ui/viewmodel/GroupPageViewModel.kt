package com.sock.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sock.app.data.model.Group
import com.sock.app.data.model.GroupMember
import com.sock.app.data.model.StatusType
import com.sock.app.data.model.User
import com.sock.app.data.repository.AuthRepository
import com.sock.app.data.repository.GroupRepository
import com.sock.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupPageUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val group: Group? = null,
    val currentUser: User? = null,
    val userStatus: StatusType? = null,
    val membersWithStatus: List<MemberWithStatus> = emptyList()
)

data class MemberWithStatus(
    val userId: String,
    val member: GroupMember,
    val status: StatusType?
)

class GroupPageViewModel(application: Application, groupId: String) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val groupRepository = GroupRepository()
    private val userRepository = UserRepository()

    private val _uiState = MutableStateFlow(GroupPageUiState())
    val uiState: StateFlow<GroupPageUiState> = _uiState.asStateFlow()

    init {
        loadGroupData(groupId)
    }

    private fun loadGroupData(groupId: String) {
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

            // Load group
            val groupResult = groupRepository.getGroup(groupId)
            val group = groupResult.getOrNull()

            if (group == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Group not found"
                )
                return@launch
            }

            // Load current user
            val userResult = userRepository.getUser(userId)
            val currentUser = userResult.getOrNull()

            // Get user's status for this group
            val userStatus = currentUser?.let { user ->
                val statusId = user.groupSpecificStatuses[groupId] ?: user.globalStatusId
                statusId?.let { StatusType.fromId(it) }
            }

            // Load member statuses
            val membersWithStatus = group.members.map { (memberUserId, member) ->
                val memberStatus = if (memberUserId == userId) {
                    userStatus
                } else {
                    // Load other user's status
                    val otherUserResult = userRepository.getUser(memberUserId)
                    val otherUser = otherUserResult.getOrNull()
                    val statusId = otherUser?.groupSpecificStatuses?.get(groupId) 
                        ?: otherUser?.globalStatusId
                    statusId?.let { StatusType.fromId(it) }
                }
                MemberWithStatus(memberUserId, member, memberStatus)
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                group = group,
                currentUser = currentUser,
                userStatus = userStatus,
                membersWithStatus = membersWithStatus
            )
        }
    }

    fun updateGroupStatus(groupId: String, status: StatusType) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch

            val currentUser = _uiState.value.currentUser ?: return@launch
            val updatedStatuses = currentUser.groupSpecificStatuses.toMutableMap()
            updatedStatuses[groupId] = status.id

            val result = userRepository.updateUser(
                userId,
                mapOf("groupSpecificStatuses" to updatedStatuses)
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    userStatus = status,
                    currentUser = currentUser.copy(groupSpecificStatuses = updatedStatuses)
                )
                // Reload to update member list
                loadGroupData(groupId)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.message ?: "Failed to update status"
                )
            }
        }
    }

    fun revertToGlobalStatus(groupId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch

            val currentUser = _uiState.value.currentUser ?: return@launch
            val updatedStatuses = currentUser.groupSpecificStatuses.toMutableMap()
            updatedStatuses.remove(groupId)

            val result = userRepository.updateUser(
                userId,
                mapOf("groupSpecificStatuses" to updatedStatuses)
            )

            result.onSuccess {
                val globalStatus = currentUser.globalStatusId?.let { StatusType.fromId(it) }
                _uiState.value = _uiState.value.copy(
                    userStatus = globalStatus,
                    currentUser = currentUser.copy(groupSpecificStatuses = updatedStatuses)
                )
                // Reload to update member list
                loadGroupData(groupId)
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.message ?: "Failed to revert status"
                )
            }
        }
    }

    fun refresh(groupId: String) {
        loadGroupData(groupId)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
