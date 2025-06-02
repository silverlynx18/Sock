package com.example.sockapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sockapp.data.models.User // For UserSearchResult
import com.example.sockapp.data.models.invitation.Invitation
import com.example.sockapp.data.models.invitation.InvitationStatus
import com.example.sockapp.data.models.invitation.InvitationType
import com.example.sockapp.data.models.group.GroupRole
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- Supporting Data Classes for UI State ---
data class UserSearchResult(
    val userId: String,
    val username: String,
    val displayName: String?,
    val photoUrl: String?
)

data class SmsInviteContent(
    val phoneNumber: String,
    val messageBody: String, // Pre-filled SMS body with link
    val inviteLink: String // The raw link, for reference or alternative sharing
)

data class InvitationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingInvitations: List<Invitation> = emptyList(),
    val viewedInvitationDetails: Invitation? = null,
    val actionInProgressMap: Map<String, Boolean> = emptyMap(),
    val actionMessage: String? = null,

    // Send invite specific state
    val isSendingInvite: Boolean = false,
    val sendInviteError: String? = null,
    val sendInviteSuccess: Boolean = false,

    // Username search for inviting existing users
    val usernameSearchResults: List<UserSearchResult> = emptyList(),
    val isLoadingUsernameSearch: Boolean = false,
    val usernameSearchError: String? = null,

    // SMS Invite content (for client to launch intent)
    val smsInviteContent: SmsInviteContent? = null,
    val isGeneratingSmsInvite: Boolean = false,
    val smsInviteError: String? = null
)

class InvitationViewModel : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore
    private val functions: FirebaseFunctions = Firebase.functions
    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow(InvitationUiState())
    val uiState: StateFlow<InvitationUiState> = _uiState.asStateFlow()

    fun clearActionMessage() = _uiState.update { it.copy(actionMessage = null) }
    fun clearSmsInviteContent() = _uiState.update { it.copy(smsInviteContent = null) }
    fun clearErrors() = _uiState.update { it.copy(error = null, sendInviteError = null, usernameSearchError = null, smsInviteError = null) }
    fun resetSendInviteStatus() = _uiState.update { it.copy(isSendingInvite = false, sendInviteError = null, sendInviteSuccess = false) }


    fun fetchMyPendingInvitations() {
        val userId = auth.currentUser?.uid ?: return
        val userEmail = auth.currentUser?.email
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val directInvitesTask = db.collection("invitations")
                    .whereEqualTo("inviteeId", userId)
                    .whereEqualTo("status", InvitationStatus.PENDING.name)
                    .get()

                val emailInvitesTask = if (userEmail != null) {
                    db.collection("invitations")
                        .whereEqualTo("inviteeEmail", userEmail)
                        .whereEqualTo("status", InvitationStatus.PENDING.name)
                        .whereEqualTo("inviteeId", null)
                        .get()
                } else null

                val directInvites = directInvitesTask.await().toObjects(Invitation::class.java)
                val emailInvites = emailInvitesTask?.await()?.toObjects(Invitation::class.java) ?: emptyList()

                val allInvites = (directInvites + emailInvites).distinctBy { it.invitationId }
                                 .sortedByDescending { it.createdAt }
                _uiState.update { it.copy(pendingInvitations = allInvites, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to fetch invitations: ${e.message}", isLoading = false) }
            }
        }
    }

    fun loadInvitationDetails(invitationId: String) {
        _uiState.update { it.copy(isLoading = true, viewedInvitationDetails = null) }
        viewModelScope.launch {
            try {
                val result = functions.getHttpsCallable("processInviteLink") // CF should now take inviteCode if it's for managed links
                    .call(mapOf("invitationId" to invitationId)) // Or "inviteCode"
                    .await()
                val data = result.data as? Map<String, Any>
                if (data != null) {
                     val invitation = Invitation( // Manual mapping
                        invitationId = data["invitationId"] as? String ?: invitationId,
                        groupId = data["groupId"] as? String ?: "",
                        groupName = data["groupName"] as? String,
                        groupImageUrl = data["groupImageUrl"] as? String,
                        inviterName = data["inviterName"] as? String,
                        status = InvitationStatus.valueOf(data["status"] as? String ?: "PENDING"),
                        roleToAssign = GroupRole.valueOf(data["roleToAssign"] as? String ?: "MEMBER"),
                        type = InvitationType.valueOf(data["type"] as? String ?: InvitationType.DIRECT_USER_ID.name)
                    )
                    _uiState.update { it.copy(viewedInvitationDetails = invitation, isLoading = false) }
                } else {
                     _uiState.update { it.copy(error = "Could not retrieve invitation details.", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load invitation: ${e.message}", isLoading = false) }
            }
        }
    }

    private fun setActionInProgress(invitationId: String, inProgress: Boolean) {
        _uiState.update {
            val newMap = it.actionInProgressMap.toMutableMap()
            newMap[invitationId] = inProgress
            it.copy(actionInProgressMap = newMap)
        }
    }

    fun acceptInvitation(invitationId: String, groupName: String?) {
        setActionInProgress(invitationId, true)
        _uiState.update { it.copy(actionMessage = null, error = null) }
        viewModelScope.launch {
            try {
                functions.getHttpsCallable("acceptInvitation").call(mapOf("invitationId" to invitationId)).await()
                _uiState.update {
                    it.copy(
                        actionMessage = "Invitation to join '${groupName ?: "the group"}' accepted!",
                        viewedInvitationDetails = it.viewedInvitationDetails?.copy(status = InvitationStatus.ACCEPTED)
                    )
                }
                fetchMyPendingInvitations()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to accept invitation: ${e.message}") }
            } finally {
                setActionInProgress(invitationId, false)
            }
        }
    }

    fun declineInvitation(invitationId: String, groupName: String?) {
        setActionInProgress(invitationId, true)
        _uiState.update { it.copy(actionMessage = null, error = null) }
        viewModelScope.launch {
            try {
                functions.getHttpsCallable("declineInvitation").call(mapOf("invitationId" to invitationId)).await()
                _uiState.update {
                    it.copy(
                        actionMessage = "Invitation to join '${groupName ?: "the group"}' declined.",
                        viewedInvitationDetails = it.viewedInvitationDetails?.copy(status = InvitationStatus.DECLINED)
                    )
                }
                fetchMyPendingInvitations()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to decline invitation: ${e.message}") }
            } finally {
                setActionInProgress(invitationId, false)
            }
        }
    }

    fun sendGroupInvitation(groupId: String, inviteeId: String, roleToAssign: GroupRole = GroupRole.MEMBER) {
        _uiState.update { it.copy(isSendingInvite = true, sendInviteError = null, sendInviteSuccess = false) }
        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "groupId" to groupId,
                    "roleToAssign" to roleToAssign.name,
                    "identifierType" to InvitationType.DIRECT_USER_ID.name,
                    "inviteeId" to inviteeId
                )
                functions.getHttpsCallable("sendGroupInvitation").call(data).await()
                _uiState.update { it.copy(isSendingInvite = false, sendInviteSuccess = true, actionMessage = "Invitation sent successfully!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSendingInvite = false, sendInviteError = "Failed to send invitation: ${e.message}") }
            }
        }
    }

    fun findUsersByUsername(usernameQuery: String) {
        if (usernameQuery.length < 2) {
            _uiState.update { it.copy(usernameSearchResults = emptyList(), usernameSearchError = "Enter at least 2 characters.") }
            return
        }
        _uiState.update { it.copy(isLoadingUsernameSearch = true, usernameSearchError = null) }
        viewModelScope.launch {
            try {
                val result = functions.getHttpsCallable("findUsersByUsername")
                    .call(mapOf("usernameQuery" to usernameQuery))
                    .await()

                @Suppress("UNCHECKED_CAST")
                val usersData = result.data as? List<Map<String, Any>> ?: emptyList()
                val searchResults = usersData.map { userMap ->
                    UserSearchResult(
                        userId = userMap["userId"] as String,
                        username = userMap["username"] as String,
                        displayName = userMap["displayName"] as? String,
                        photoUrl = userMap["photoUrl"] as? String
                    )
                }
                _uiState.update { it.copy(usernameSearchResults = searchResults, isLoadingUsernameSearch = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingUsernameSearch = false, usernameSearchError = "Search failed: ${e.message}", usernameSearchResults = emptyList()) }
            }
        }
    }

    fun sendSmsInvite(phoneNumber: String, groupId: String, groupName: String) {
         val inviterName = auth.currentUser?.displayName ?: "A friend"
        _uiState.update { it.copy(isGeneratingSmsInvite = true, smsInviteError = null, smsInviteContent = null) }
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "phoneNumber" to phoneNumber, // Should be E.164
                    "groupId" to groupId,
                    "groupName" to groupName, // For SMS body
                    "inviterName" to inviterName // For SMS body
                )
                val result = functions.getHttpsCallable("sendSMSToNonUserAndGenerateInvite").call(data).await()
                val resultData = result.data as? Map<String, String>
                if (resultData != null && resultData["inviteLink"] != null && resultData["smsBody"] != null) {
                    _uiState.update {
                        it.copy(
                            smsInviteContent = SmsInviteContent(phoneNumber, resultData["smsBody"]!!, resultData["inviteLink"]!!),
                            isGeneratingSmsInvite = false,
                            actionMessage = "SMS invite content ready."
                        )
                    }
                } else {
                    throw IllegalStateException("CF result for SMS invite was not in expected format.")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingSmsInvite = false, smsInviteError = "Failed to generate SMS invite: ${e.message}") }
            }
        }
    }
}
