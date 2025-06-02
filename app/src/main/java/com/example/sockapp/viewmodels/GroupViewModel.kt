package com.example.sockapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sockapp.data.models.group.Group
import com.example.sockapp.data.models.group.GroupMember
import com.example.sockapp.data.models.group.GroupRole
import com.example.sockapp.data.models.group.ManagedInviteLink // Added
import com.google.firebase.Timestamp // Added
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

data class GroupUiState(
    // General state
    val isLoading: Boolean = false,
    val error: String? = null,

    // Group list state
    val userGroups: List<Group> = emptyList(),
    val isLoadingUserGroups: Boolean = false,

    // Current selected group state
    val currentGroup: Group? = null,
    val isLoadingCurrentGroup: Boolean = false,
    val currentGroupMembers: List<GroupMember> = emptyList(),
    val isLoadingCurrentGroupMembers: Boolean = false,
    val currentUserRoleInGroup: GroupRole? = null,

    // Create group state
    val isCreatingGroup: Boolean = false,
    val createGroupSuccess: Boolean = false,
    val createGroupError: String? = null,

    // Managed Invite Links State
    val managedInviteLinks: List<ManagedInviteLink> = emptyList(),
    val isLoadingManagedInviteLinks: Boolean = false,
    val generateLinkError: String? = null,
    val generateLinkSuccess: Boolean = false, // For feedback on link creation

    // Other action states
    val actionInProgress: Boolean = false,
    val actionError: String? = null,
    val actionSuccessMessage: String? = null
)

class GroupViewModel : ViewModel() {

    private val db: FirebaseFirestore = Firebase.firestore
    private val functions: FirebaseFunctions = Firebase.functions
    val auth = Firebase.auth // Made public for easier access in Composables if needed for currentUserId

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    private var groupListenerRegistration: ListenerRegistration? = null
    private var membersListenerRegistration: ListenerRegistration? = null
    private var managedLinksListenerRegistration: ListenerRegistration? = null


    fun clearErrorsAndMessages() {
        _uiState.update {
            it.copy(
                error = null, createGroupError = null, actionError = null,
                actionSuccessMessage = null, generateLinkError = null
            )
        }
    }
     fun resetGenerateLinkStatus() {
        _uiState.update { it.copy(generateLinkSuccess = false, generateLinkError = null) }
    }


    // --- Group Creation ---
    fun createGroup(name: String, description: String?, isPublic: Boolean, profileImageUrl: String? = null, bannerImageUrl: String? = null) {
        val currentUser = auth.currentUser ?: run {
            _uiState.update { it.copy(createGroupError = "User not authenticated.") }
            return
        }
        _uiState.update { it.copy(isCreatingGroup = true, createGroupError = null, createGroupSuccess = false) }
        viewModelScope.launch {
            try {
                val groupData = hashMapOf(
                    "name" to name, "description" to description, "isPublic" to isPublic,
                    "profileImageUrl" to profileImageUrl, "bannerImageUrl" to bannerImageUrl,
                    // CF will use context.auth.uid for creatorId
                    "creatorDisplayName" to (currentUser.displayName ?: "Unknown User"),
                    "creatorPhotoUrl" to currentUser.photoUrl?.toString()
                )
                functions.getHttpsCallable("createGroup").call(groupData).await()
                _uiState.update { it.copy(isCreatingGroup = false, createGroupSuccess = true, actionSuccessMessage = "Group created successfully!") }
                fetchUserGroups()
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreatingGroup = false, createGroupError = "Failed to create group: ${e.message}") }
            }
        }
    }
     fun resetCreateGroupStatus() = _uiState.update { it.copy(createGroupSuccess = false, createGroupError = null, isCreatingGroup = false) }


    // --- Fetching Data ---
    fun fetchUserGroups() {
        val userId = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoadingUserGroups = true) }
        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val groupIds = userDoc.get("joinedGroupIds") as? List<String>
            if (groupIds.isNullOrEmpty()) {
                _uiState.update { it.copy(userGroups = emptyList(), isLoadingUserGroups = false) }
                return@addOnSuccessListener
            }
             // Firestore "in" query limit is 30 (new limit, previously 10). Fetch in chunks if more.
            val chunks = groupIds.chunked(30)
            val allGroups = mutableListOf<Group>()
            viewModelScope.launch {
                try {
                    for (chunk in chunks) {
                        val snapshot = db.collection("groups").whereIn("groupId", chunk).get().await()
                        allGroups.addAll(snapshot.toObjects(Group::class.java))
                    }
                    _uiState.update { it.copy(userGroups = allGroups, isLoadingUserGroups = false) }
                } catch (e: Exception) {
                     _uiState.update { it.copy(error = "Failed to fetch user groups: ${e.message}", isLoadingUserGroups = false) }
                }
            }
        }.addOnFailureListener { e ->
            _uiState.update { it.copy(error = "Failed to fetch user's group IDs: ${e.message}", isLoadingUserGroups = false) }
        }
    }

    fun listenToGroupDetails(groupId: String) {
        _uiState.update { it.copy(isLoadingCurrentGroup = true, currentGroup = null, currentUserRoleInGroup = null) }
        groupListenerRegistration?.remove()
        groupListenerRegistration = db.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _uiState.update { it.copy(error = "Failed to listen to group details: ${e.message}", isLoadingCurrentGroup = false) }
                    return@addSnapshotListener
                }
                val group = snapshot?.toObject(Group::class.java)
                _uiState.update { it.copy(currentGroup = group, isLoadingCurrentGroup = false) }
                if (group != null) {
                    listenToGroupMembers(groupId)
                    fetchManagedInviteLinks(groupId) // Fetch admin links when group details are loaded
                } else { // Group might have been deleted
                    _uiState.update { it.copy(managedInviteLinks = emptyList())}
                }
            }
    }

    fun listenToGroupMembers(groupId: String) {
        val currentUserId = auth.currentUser?.uid
        _uiState.update { it.copy(isLoadingCurrentGroupMembers = true) }
        membersListenerRegistration?.remove()
        membersListenerRegistration = db.collection("groups").document(groupId).collection("members")
            .orderBy("role", Query.Direction.DESCENDING) // Show Owner, Admin first
            .orderBy("displayName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _uiState.update { it.copy(error = "Failed to listen to group members: ${e.message}", isLoadingCurrentGroupMembers = false) }
                    return@addSnapshotListener
                }
                val members = snapshot?.toObjects(GroupMember::class.java) ?: emptyList()
                val userRole = members.find { it.userId == currentUserId }?.role
                _uiState.update { it.copy(currentGroupMembers = members, currentUserRoleInGroup = userRole, isLoadingCurrentGroupMembers = false) }
            }
    }

    fun stopListeningToGroupDetails() {
        groupListenerRegistration?.remove()
        membersListenerRegistration?.remove()
        managedLinksListenerRegistration?.remove()
        _uiState.update { it.copy(currentGroup = null, currentGroupMembers = emptyList(), currentUserRoleInGroup = null, managedInviteLinks = emptyList()) }
    }

    // --- Managed Invite Links ---
    fun fetchManagedInviteLinks(groupId: String) {
        _uiState.update { it.copy(isLoadingManagedInviteLinks = true) }
        managedLinksListenerRegistration?.remove()
        managedLinksListenerRegistration = db.collection("groups").document(groupId).collection("managedInviteLinks")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _uiState.update { it.copy(error = "Failed to fetch managed invite links: ${e.message}", isLoadingManagedInviteLinks = false) }
                    return@addSnapshotListener
                }
                val links = snapshot?.toObjects(ManagedInviteLink::class.java) ?: emptyList()
                _uiState.update { it.copy(managedInviteLinks = links, isLoadingManagedInviteLinks = false) }
            }
    }

    fun createManagedInviteLink(groupId: String, roleToAssign: GroupRole, maxUses: Long?, expiresAt: Timestamp?) {
        _uiState.update { it.copy(actionInProgress = true, generateLinkError = null, generateLinkSuccess = false) }
        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "groupId" to groupId,
                    "roleToAssign" to roleToAssign.name,
                    "maxUses" to maxUses,
                    "expiresAt" to expiresAt?.toDate()?.toInstant()?.toString() // Send as ISO string
                )
                val result = functions.getHttpsCallable("generateManagedGroupInviteLink").call(data).await()
                val resultData = result.data as? Map<String, Any>
                _uiState.update { it.copy(actionInProgress = false, generateLinkSuccess = true, actionSuccessMessage = "Invite link created: ${resultData?.get("inviteCode") ?: ""}") }
                // list will update via listener
            } catch (e: Exception) {
                _uiState.update { it.copy(actionInProgress = false, generateLinkError = "Failed to create link: ${e.message}") }
            }
        }
    }

    fun revokeManagedInviteLink(groupId: String, linkId: String) {
        _uiState.update { it.copy(actionInProgress = true, actionError = null) }
        viewModelScope.launch {
            try {
                functions.getHttpsCallable("revokeManagedGroupInviteLink")
                    .call(mapOf("groupId" to groupId, "linkId" to linkId))
                    .await()
                _uiState.update { it.copy(actionInProgress = false, actionSuccessMessage = "Invite link revoked.") }
                 // list will update via listener
            } catch (e: Exception) {
                _uiState.update { it.copy(actionInProgress = false, actionError = "Failed to revoke link: ${e.message}") }
            }
        }
    }


    // --- Other Group Actions ---
    fun leaveGroup(groupId: String) { /* ... existing code ... */ }
    fun removeMember(groupId: String, memberUserId: String) { /* ... existing code ... */ }
    fun updateMemberRole(groupId: String, memberUserId: String, newRole: GroupRole) { /* ... existing code ... */ }
    fun joinPublicGroup(groupId: String) { /* ... existing code ... */ }

    override fun onCleared() {
        super.onCleared()
        groupListenerRegistration?.remove()
        membersListenerRegistration?.remove()
        managedLinksListenerRegistration?.remove()
    }
}

// Ensure existing leaveGroup, removeMember, updateMemberRole, joinPublicGroup methods are present from previous version
// For brevity, their full code is not repeated here but assumed to be part of the class.
// Example:
// fun leaveGroup(groupId: String) {
//     _uiState.update { it.copy(actionInProgress = true, actionError = null) }
//     viewModelScope.launch {
//         try {
//             val data = mapOf("groupId" to groupId, "action" to "leave")
//             functions.getHttpsCallable("handleUserLeaveOrRemove").call(data).await()
//             _uiState.update { it.copy(actionInProgress = false, actionSuccessMessage = "Successfully left the group.") }
//             fetchUserGroups()
//             if (_uiState.value.currentGroup?.groupId == groupId) {
//                 _uiState.update { it.copy(currentGroup = null, currentGroupMembers = emptyList(), currentUserRoleInGroup = null) }
//             }
//         } catch (e: Exception) {
//             _uiState.update { it.copy(actionInProgress = false, actionError = "Failed to leave group: ${e.message}") }
//         }
//     }
// }
// fun removeMember(groupId: String, memberUserId: String) {
//    // ...
// }
// fun updateMemberRole(groupId: String, memberUserId: String, newRole: GroupRole) {
//    // ...
// }
// fun joinPublicGroup(groupId: String) {
//    // ...
// }
