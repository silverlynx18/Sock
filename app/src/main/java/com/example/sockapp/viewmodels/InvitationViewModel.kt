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
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- Supporting Data Classes for UI State ---

/**
 * Simplified user data for displaying username search results.
 */
data class UserSearchResult(
    val userId: String,
    val username: String,
    val displayName: String?,
    val photoUrl: String?
)

/**
 * Holds content required for the client to initiate an SMS intent for inviting a user.
 * @param phoneNumber The target phone number.
 * @param messageBody The pre-filled SMS message body including the invite link.
 * @param inviteLink The raw invitation link (for reference or alternative sharing).
 */
data class SmsInviteContent(
    val phoneNumber: String,
    val messageBody: String,
    val inviteLink: String
)

/**
 * UI State for invitation-related screens.
 * @param isLoading True when fetching general invitation data (e.g., list of pending invites).
 * @param error General error message for invitation operations.
 * @param pendingInvitations List of [Invitation]s pending for the current user.
 * @param viewedInvitationDetails Details of a specific invitation being viewed (e.g., from a link).
 * @param actionInProgressMap Tracks loading state for actions on individual invitations (key: invitationId).
 * @param actionMessage A general success or informational message.
 * @param isSendingInvite True when a direct group invitation (to existing user) is being sent.
 * @param sendInviteError Error message specific to sending direct group invitations.
 * @param sendInviteSuccess True if direct group invitation was sent successfully.
 * @param usernameSearchResults List of [UserSearchResult] from a username query.
 * @param isLoadingUsernameSearch True when a username search is in progress.
 * @param usernameSearchError Error message specific to username search.
 * @param smsInviteContent Content for pre-filling an SMS invite, null if not active.
 * @param isGeneratingSmsInvite True when content for an SMS invite is being generated.
 * @param smsInviteError Error message specific to generating SMS invite content.
 */
data class InvitationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingInvitations: List<Invitation> = emptyList(),
    val viewedInvitationDetails: Invitation? = null,
    val actionInProgressMap: Map<String, Boolean> = emptyMap(),
    val actionMessage: String? = null,

    val isSendingInvite: Boolean = false,
    val sendInviteError: String? = null,
    val sendInviteSuccess: Boolean = false,

    val usernameSearchResults: List<UserSearchResult> = emptyList(),
    val isLoadingUsernameSearch: Boolean = false,
    val usernameSearchError: String? = null,

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

    // --- UI State Clearers ---
    fun clearActionMessage() = _uiState.update { it.copy(actionMessage = null) }
    fun clearSmsInviteContent() = _uiState.update { it.copy(smsInviteContent = null) }
    fun clearErrors() = _uiState.update { it.copy(error = null, sendInviteError = null, usernameSearchError = null, smsInviteError = null) }
    fun resetSendInviteStatus() = _uiState.update { it.copy(isSendingInvite = false, sendInviteError = null, sendInviteSuccess = false) }


    /**
     * Fetches all PENDING invitations for the currently authenticated user.
     * This includes invitations directly to their UID and unclaimed invitations to their email.
     */
    fun fetchMyPendingInvitations() {
        val userId = auth.currentUser?.uid ?: return _uiState.update { it.copy(error = "Please sign in to view invitations.")}
        val userEmail = auth.currentUser?.email // For matching email invites that haven't been linked to UID yet

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                // Query for invitations directly assigned to the user's ID
                val directInvitesTask = db.collection("invitations")
                    .whereEqualTo("inviteeId", userId)
                    .whereEqualTo("status", InvitationStatus.PENDING.name) // Storing enum as string
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()

                // Query for invitations sent to the user's email, where inviteeId might not yet be set
                // (e.g., user signed up after email invite was sent)
                val emailInvitesTask = if (userEmail != null) {
                    db.collection("invitations")
                        .whereEqualTo("inviteeEmail", userEmail)
                        .whereEqualTo("status", InvitationStatus.PENDING.name)
                        .whereEqualTo("inviteeId", null) // Crucial: only those not yet claimed by a UID
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .get()
                } else null

                val directInvites = directInvitesTask.await().toObjects(Invitation::class.java)
                val emailInvites = emailInvitesTask?.await()?.toObjects(Invitation::class.java) ?: emptyList()

                // Combine and ensure no duplicates if an email invite was somehow also directly assigned (should be rare)
                val allInvites = (directInvites + emailInvites).distinctBy { it.invitationId }
                                 .sortedByDescending { it.createdAt } // Re-sort after distinct just in case

                _uiState.update { it.copy(pendingInvitations = allInvites, isLoading = false) }

            } catch (e: Exception) {
                Firebase.functions.logger.error("InvitationViewModel: Error fetching pending invitations for $userId", e)
                _uiState.update { it.copy(error = "Could not load your invitations. Please try again.", isLoading = false) }
            }
        }
    }

    /**
     * Loads details for a specific invitation, typically from an invite link/code.
     * Calls the 'processInviteLink' Cloud Function which validates the invite code and
     * returns details of the specific invitation to be acted upon.
     * @param inviteCode The unique code from a managed invite link.
     */
    fun loadInvitationDetailsByInviteCode(inviteCode: String) {
        _uiState.update { it.copy(isLoading = true, viewedInvitationDetails = null, error = null) }
        viewModelScope.launch {
            try {
                val result = functions.getHttpsCallable("processInviteLink")
                    .call(mapOf("inviteCode" to inviteCode)) // CF expects "inviteCode"
                    .await()

                val data = result.data as? Map<String, Any>
                if (data != null && data["success"] == true && data["invitationId"] != null) {
                    // After processing a managed link, CF returns a *new specific* invitationId.
                    // We should then fetch *that* invitation's details for display and action.
                    val specificInvitationId = data["invitationId"] as String
                    val specificInviteDoc = db.collection("invitations").document(specificInvitationId).get().await()
                    val invitation = specificInviteDoc.toObject(Invitation::class.java)

                    if (invitation != null) {
                         _uiState.update { it.copy(viewedInvitationDetails = invitation, isLoading = false) }
                    } else {
                         _uiState.update { it.copy(error = "Invitation details not found after processing link.", isLoading = false) }
                    }
                } else {
                     Firebase.functions.logger.warn("InvitationViewModel: processInviteLink CF returned unexpected data or success=false for code $inviteCode", data)
                     _uiState.update { it.copy(error = (data?.get("message") as? String) ?: "Invalid or expired invite link.", isLoading = false) }
                }
            } catch (e: Exception) {
                Firebase.functions.logger.error("InvitationViewModel: Error processing invite code $inviteCode", e)
                val message = if (e is FirebaseFunctionsException) e.message ?: "Failed to process invite link." else "An unexpected error occurred."
                _uiState.update { it.copy(error = message, isLoading = false) }
            }
        }
    }


    private fun setActionInProgress(invitationId: String, inProgress: Boolean) { /* ... existing code ... */ }

    /**
     * Calls Cloud Function to accept an invitation.
     * Refreshes pending invitations on success.
     */
    fun acceptInvitation(invitationId: String, groupName: String?) { /* ... existing code with refined error messages ... */
        setActionInProgress(invitationId, true)
        _uiState.update { it.copy(actionMessage = null, error = null) }
        viewModelScope.launch {
            try {
                functions.getHttpsCallable("acceptInvitation").call(mapOf("invitationId" to invitationId)).await()
                _uiState.update {
                    it.copy(
                        actionMessage = "Successfully joined '${groupName ?: "the group"}'!",
                        viewedInvitationDetails = it.viewedInvitationDetails?.copy(status = InvitationStatus.ACCEPTED)
                    )
                }
                fetchMyPendingInvitations()
            } catch (e: Exception) {
                Firebase.functions.logger.error("InvitationViewModel: Error accepting invitation $invitationId", e)
                val message = if (e is FirebaseFunctionsException) e.message ?: "Failed to accept invitation." else "An unexpected error occurred."
                _uiState.update { it.copy(error = message) }
            } finally {
                setActionInProgress(invitationId, false)
            }
        }
    }

    /**
     * Calls Cloud Function to decline an invitation.
     * Refreshes pending invitations on success.
     */
    fun declineInvitation(invitationId: String, groupName: String?) { /* ... existing code with refined error messages ... */
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
                Firebase.functions.logger.error("InvitationViewModel: Error declining invitation $invitationId", e)
                val message = if (e is FirebaseFunctionsException) e.message ?: "Failed to decline invitation." else "An unexpected error occurred."
                _uiState.update { it.copy(error = message) }
            } finally {
                setActionInProgress(invitationId, false)
            }
        }
    }

    /**
     * Sends a group invitation to an existing user by their UID.
     * Calls the 'sendGroupInvitation' Cloud Function.
     */
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
                 Firebase.functions.logger.error("InvitationViewModel: Error sending direct group invitation for group $groupId to user $inviteeId", e)
                val message = if (e is FirebaseFunctionsException) e.message ?: "Failed to send invitation." else "An unexpected error occurred."
                _uiState.update { it.copy(isSendingInvite = false, sendInviteError = message) }
            }
        }
    }

    /**
     * Searches for users by username via a Cloud Function.
     * Updates UI state with search results or an error.
     */
    fun findUsersByUsername(usernameQuery: String) {
        if (usernameQuery.trim().length < 2) { // Minimum query length
            _uiState.update { it.copy(usernameSearchResults = emptyList(), usernameSearchError = if (usernameQuery.isNotEmpty()) "Enter at least 2 characters." else null )}
            return
        }
        _uiState.update { it.copy(isLoadingUsernameSearch = true, usernameSearchError = null) }
        viewModelScope.launch {
            try {
                val result = functions.getHttpsCallable("findUsersByUsername")
                    .call(mapOf("usernameQuery" to usernameQuery.trim()))
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
                Firebase.functions.logger.error("InvitationViewModel: Error finding users by username for query '$usernameQuery'", e)
                val message = if (e is FirebaseFunctionsException) e.message ?: "Username search failed." else "An unexpected error occurred during search."
                _uiState.update { it.copy(isLoadingUsernameSearch = false, usernameSearchError = message, usernameSearchResults = emptyList()) }
            }
        }
    }

    /**
     * Generates content for an SMS invitation by calling a Cloud Function.
     * The UI layer should observe [InvitationUiState.smsInviteContent] and launch an SMS intent.
     * @param phoneNumber The E.164 formatted phone number of the recipient.
     * @param groupId The ID of the group to invite to.
     * @param groupName The name of the group (for the SMS body).
     */
    fun generateSmsInviteContent(phoneNumber: String, groupId: String, groupName: String) {
         val inviterName = auth.currentUser?.displayName ?: "A friend"
        _uiState.update { it.copy(isGeneratingSmsInvite = true, smsInviteError = null, smsInviteContent = null) }
        viewModelScope.launch {
            try {
                val data = mapOf(
                    "phoneNumber" to phoneNumber,
                    "groupId" to groupId,
                    "groupName" to groupName,
                    "inviterName" to inviterName
                )
                val result = functions.getHttpsCallable("sendSMSToNonUserAndGenerateInvite").call(data).await()
                val resultData = result.data as? Map<String, String>
                if (resultData != null && resultData["inviteLink"] != null && resultData["smsBody"] != null) {
                    _uiState.update {
                        it.copy(
                            smsInviteContent = SmsInviteContent(phoneNumber, resultData["smsBody"]!!, resultData["inviteLink"]!!),
                            isGeneratingSmsInvite = false,
                            actionMessage = "SMS invite ready to send." // UI can use this to trigger intent
                        )
                    }
                } else {
                    throw IllegalStateException("Cloud Function result for SMS invite was not in the expected format.")
                }
            } catch (e: Exception) {
                Firebase.functions.logger.error("InvitationViewModel: Error generating SMS invite for $phoneNumber to group $groupId", e)
                val message = if (e is FirebaseFunctionsException) e.message ?: "Failed to create SMS invite." else "Could not prepare SMS invite."
                _uiState.update { it.copy(isGeneratingSmsInvite = false, smsInviteError = message) }
            }
        }
    }
}
