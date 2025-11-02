package com.sock.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sock.app.data.model.Group
import com.sock.app.data.model.StatusType
import com.sock.app.data.model.User
import com.sock.app.data.repository.AuthRepository
import com.sock.app.data.repository.GroupRepository
import com.sock.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUser: User? = null,
    val groups: List<Group> = emptyList(),
    val globalStatus: StatusType? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)
    private val userRepository = UserRepository()
    private val groupRepository = GroupRepository()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
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

            // Load user data
            val userResult = userRepository.getUser(userId)
            val user = userResult.getOrNull()
            
            if (user == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load user data"
                )
                return@launch
            }

            // Load groups
            val groupsResult = groupRepository.getUserGroups(userId)
            val groups = groupsResult.getOrNull() ?: emptyList()

            // Get global status
            val globalStatus = user.globalStatusId?.let { StatusType.fromId(it) }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentUser = user,
                groups = groups,
                globalStatus = globalStatus
            )
        }
    }

    fun updateGlobalStatus(status: StatusType) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            
            val result = userRepository.updateUser(userId, mapOf("globalStatusId" to status.id))
            
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    globalStatus = status,
                    currentUser = _uiState.value.currentUser?.copy(globalStatusId = status.id)
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = exception.message ?: "Failed to update status"
                )
            }
        }
    }

    fun getGroupStatus(groupId: String): StatusType? {
        val user = _uiState.value.currentUser ?: return null
        val statusId = user.groupSpecificStatuses[groupId] ?: user.globalStatusId
        return statusId?.let { StatusType.fromId(it) }
    }

    fun refresh() {
        loadDashboardData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
