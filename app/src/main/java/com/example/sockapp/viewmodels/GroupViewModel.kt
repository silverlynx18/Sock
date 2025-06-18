package com.example.sockapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sockapp.data.models.group.Group
import com.example.sockapp.data.models.group.GroupMember
import com.example.sockapp.data.models.group.GroupRole
import com.example.sockapp.data.models.group.ManagedInviteLink
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

/**
 * UI State for group-related screens.
 * @param isLoading True when any general group data is being loaded.
 * @param error General error message for group operations.
 * @param userGroups List of groups the current user is a member of.
 * @param isLoadingUserGroups True when the list of user's groups is being fetched.
 * @param currentGroup The currently viewed or selected group's details.
 * @param isLoadingCurrentGroup True when details of the current group are being fetched.
 * @param currentGroupMembers List of members for the [currentGroup].
 * @param isLoadingCurrentGroupMembers True when members of the [currentGroup] are being fetched.
 * @param currentUserRoleInGroup The role of the current authenticated user in the [currentGroup].
 * @param isCreatingGroup True when a new group creation is in progress.
 * @param createGroupSuccess True if group creation was successful.
 * @param createGroupError Error message specific to group creation.
 * @param managedInviteLinks List of admin-generated invite links for the [currentGroup].
 * @param isLoadingManagedInviteLinks True when managed invite links are being fetched.
 * @param generateLinkError Error message specific to generating an invite link.
 * @param generateLinkSuccess True if invite link generation was successful.
 * @param actionInProgress True when a generic group action (like leave, remove member) is in progress.
 * @param actionError Error message for generic group actions.
 * @param actionSuccessMessage Success message for generic group actions.
 */
data class GroupUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userGroups: List<Group> = emptyList(),
    val isLoadingUserGroups: Boolean = false,
    val currentGroup: Group? = null,
    val isLoadingCurrentGroup: Boolean = false,
    val currentGroupMembers: List<GroupMember> = emptyList(),
    val isLoadingCurrentGroupMembers: Boolean = false,
    val currentUserRoleInGroup: GroupRole? = null,
    val isCreatingGroup: Boolean = false,
    val createGroupSuccess: Boolean = false,
    val createGroupError: String? = null,
    val managedInviteLinks: List<ManagedInviteLink> = emptyList(),
    val isLoadingManagedInviteLinks: Boolean = false,
    val generateLinkError: String? = null,
    val generateLinkSuccess: Boolean = false,
    val actionInProgress: Boolean = false,
    val actionError: String? = null,
    val actionSuccessMessage: String? = null
)

class GroupViewModel : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore
    private val functions: FirebaseFunctions = Firebase.functions
    internal val auth = Firebase.auth // internal for easier access by Composables if needed

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    // Firestore listener registrations to be cleaned up in onCleared
    private var groupListenerRegistration: ListenerRegistration? = null
    private var membersListenerRegistration: ListenerRegistration? = null
    private var managedLinksListenerRegistration: ListenerRegistration? = null

    /**
     * Clears general errors, action-specific errors, and success messages from the UI state.
     */
    fun clearErrorsAndMessages() {
        _uiState.update {
            it.copy(
                error = null, createGroupError = null, actionError = null,
                actionSuccessMessage = null, generateLinkError = null
            )
        }
    }

    /**
     * Resets the state related to generating a managed invite link.
     */
    fun resetGenerateLinkStatus() = _uiState.update { it.copy(generateLinkSuccess = false, generateLinkError = null, actionInProgress = false) }

    /**
     * Resets the state related to creating a group.
     */
    fun resetCreateGroupStatus() = _uiState.update { it.copy(createGroupSuccess = false, createGroupError = null, isCreatingGroup = false) }


    // --- Group CRUD Operations ---
    /**
     * Calls a Cloud Function to create a new group.
     * @param name The name of the group.
     * @param description Optional description for the group.
     * @param isPublic Whether the group is public or private.
     * @param profileImageUrl Optional URL for the group's profile image.
     * @param bannerImageUrl Optional URL for the group's banner image.
     */
    fun createGroup(name: String, description: String?, isPublic: Boolean, profileImageUrl: String? = null, bannerImageUrl: String? = null) {
        val currentUser = auth.currentUser ?: return _uiState.update { it.copy(createGroupError = "Please sign in to create a group.") }
        if (name.isBlank()) return _uiState.update { it.copy(createGroupError = "Group name cannot be empty.")}

        _uiState.update { it.copy(isCreatingGroup = true, createGroupError = null, createGroupSuccess = false) }
        viewModelScope.launch {
            try {
                val groupData = hashMapOf(
                    "name" to name.trim(), "description" to description?.trim(), "isPublic" to isPublic,
                    "profileImageUrl" to profileImageUrl?.trim()?.ifEmpty { null },
                    "bannerImageUrl" to bannerImageUrl?.trim()?.ifEmpty { null },
                    "creatorDisplayName" to (currentUser.displayName ?: currentUser.email?.split("@")?.get(0) ?: "New User"), // Best effort display name
                    "creatorPhotoUrl" to currentUser.photoUrl?.toString()
                )
                functions.getHttpsCallable("createGroup").call(groupData).await()
                _uiState.update { it.copy(isCreatingGroup = false, createGroupSuccess = true, actionSuccessMessage = "Group '${name.trim()}' created successfully!") }
                fetchUserGroups() // Refresh the list of user's groups
            } catch (e: Exception) {
                Firebase.functions.logger.error("GroupViewModel: Error creating group '$name'", e)
                _uiState.update { it.copy(isCreatingGroup = false, createGroupError = "Failed to create group. Please try again.") }
            }
        }
    }

    /**
     * Calls a Cloud Function to update details of an existing group.
     * Note: The corresponding Cloud Function 'updateGroupDetails' needs to be implemented.
     * @param groupId ID of the group to update.
     * @param name New name for the group.
     * @param description New description.
     * @param isPublic New visibility status.
     * @param profileImageUrl New profile image URL.
     * @param bannerImageUrl New banner image URL.
     */
    fun updateGroupDetails(groupId: String, name: String, description: String?, isPublic: Boolean, profileImageUrl: String?, bannerImageUrl: String?) {
        if (name.isBlank()) return _uiState.update { it.copy(actionError = "Group name cannot be empty.")}
        _uiState.update { it.copy(actionInProgress = true, actionError = null, actionSuccessMessage = null) }
        viewModelScope.launch {
            try {
                val groupUpdateData = mapOf(
                    "groupId" to groupId, "name" to name.trim(), "description" to description?.trim(),
                    "isPublic" to isPublic, "profileImageUrl" to profileImageUrl?.trim()?.ifEmpty { null },
                    "bannerImageUrl" to bannerImageUrl?.trim()?.ifEmpty { null }
                )
                // Assuming a Cloud Function "updateGroupDetails" handles this securely
                functions.getHttpsCallable("updateGroupDetails").call(groupUpdateData).await()
                _uiState.update { it.copy(actionInProgress = false, actionSuccessMessage = "Group details updated.") }
                // Group details will update via listener if successful
            } catch (e: Exception) {
                Firebase.functions.logger.error("GroupViewModel: Error updating group '$groupId'", e)
                _uiState.update { it.copy(actionInProgress = false, actionError = "Failed to update group details. Please try again.") }
            }
        }
    }

    /**
     * Calls a Cloud Function to delete a group.
     * Note: The corresponding Cloud Function 'deleteGroup' handles owner verification and data cleanup.
     * @param groupId ID of the group to delete.
     */
    fun deleteGroup(groupId: String) {
        _uiState.update { it.copy(actionInProgress = true, actionError = null, actionSuccessMessage = null) }
        viewModelScope.launch {
            try {
                functions.getHttpsCallable("deleteGroup").call(mapOf("groupId" to groupId)).await()
                _uiState.update { it.copy(actionInProgress = false, actionSuccessMessage = "Group deleted successfully.") }
                fetchUserGroups() // Refresh list
                 if (_uiState.value.currentGroup?.groupId == groupId) { // If current group was the one deleted
                    stopListeningToGroupDetails() // Clear current group state
                }
            } catch (e: Exception) {
                 Firebase.functions.logger.error("GroupViewModel: Error deleting group '$groupId'", e)
                _uiState.update { it.copy(actionInProgress = false, actionError = "Failed to delete group. Please ensure you are the owner and try again.") }
            }
        }
    }


    // --- Fetching Data ---
    /**
     * Fetches groups the current user is a member of.
     * Relies on 'joinedGroupIds' field in the User document.
     */
    fun fetchUserGroups() { /* ... (existing code with chunking logic) ... */ }

    /**
     * Listens for real-time updates to a specific group's details.
     * Also triggers fetching members and managed invite links for that group.
     * @param groupId ID of the group to listen to.
     */
    fun listenToGroupDetails(groupId: String) { /* ... (existing code) ... */ }

    /**
     * Listens for real-time updates to a group's member list.
     * Determines the current user's role within that group.
     * @param groupId ID of the group whose members to listen to.
     */
    fun listenToGroupMembers(groupId: String) { /* ... (existing code) ... */ }

    /**
     * Stops all active listeners for group details, members, and managed links.
     * Clears related data from UI state.
     */
    fun stopListeningToGroupDetails() { /* ... (existing code) ... */ }

    // --- Managed Invite Links ---
    /**
     * Fetches and listens for real-time updates to admin-generated invite links for a group.
     * @param groupId ID of the group whose managed links to fetch.
     */
    fun fetchManagedInviteLinks(groupId: String) { /* ... (existing code) ... */ }

    /**
     * Calls a Cloud Function to create a new managed invite link for a group.
     * @param groupId ID of the group.
     * @param roleToAssign The [GroupRole] to assign to users joining via this link.
     * @param maxUses Optional maximum number of times this link can be used.
     * @param expiresAt Optional [Timestamp] when this link should expire.
     */
    fun createManagedInviteLink(groupId: String, roleToAssign: GroupRole, maxUses: Long?, expiresAt: Timestamp?) { /* ... (existing code with refined error message) ... */ }

    /**
     * Calls a Cloud Function to revoke (deactivate) a managed invite link.
     * @param groupId ID of the group the link belongs to.
     * @param linkId ID of the managed invite link to revoke.
     */
    fun revokeManagedInviteLink(groupId: String, linkId: String) { /* ... (existing code with refined error message) ... */ }


    // --- Other Group Actions (Assumed complete from previous iterations) ---
    fun leaveGroup(groupId: String) { /* ... (existing code with refined error message) ... */ }
    fun removeMember(groupId: String, memberUserId: String) { /* ... (existing code with refined error message) ... */ }
    fun updateMemberRole(groupId: String, memberUserId: String, newRole: GroupRole) { /* ... (existing code with refined error message) ... */ }
    fun joinPublicGroup(groupId: String) { /* ... (existing code with refined error message) ... */ }

    override fun onCleared() {
        super.onCleared()
        groupListenerRegistration?.remove()
        membersListenerRegistration?.remove()
        managedLinksListenerRegistration?.remove()
    }

    // Full implementations for assumed existing methods (leaveGroup, removeMember, etc.)
    // These would have refined error messages similar to the new methods.
    // Example:
    // fun leaveGroup(groupId: String) {
    //     val userId = auth.currentUser?.uid ?: return _uiState.update{it.copy(actionError="Please sign in.")}
    //     _uiState.update { it.copy(actionInProgress = true, actionError = null) }
    //     viewModelScope.launch {
    //         try {
    //             val data = mapOf("groupId" to groupId, "action" to "leave") // CF 'handleUserLeaveOrRemove'
    //             functions.getHttpsCallable("handleUserLeaveOrRemove").call(data).await()
    //             _uiState.update { it.copy(actionInProgress = false, actionSuccessMessage = "Successfully left the group.") }
    //             fetchUserGroups()
    //             if (_uiState.value.currentGroup?.groupId == groupId) {
    //                 stopListeningToGroupDetails()
    //             }
    //         } catch (e: Exception) {
    //             Firebase.functions.logger.error("GroupViewModel: Error leaving group '$groupId' for user '$userId'", e)
    //             _uiState.update { it.copy(actionInProgress = false, actionError = "Failed to leave group. Please try again.") }
    //         }
    //     }
    // }
    // ... (similar for removeMember, updateMemberRole, joinPublicGroup)
}
