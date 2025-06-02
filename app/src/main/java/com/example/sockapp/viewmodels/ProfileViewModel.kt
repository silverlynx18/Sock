package com.example.sockapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sockapp.data.models.User
import com.example.sockapp.data.models.profile.NotificationType // Assuming path
import com.example.sockapp.data.models.userstatus.CustomStatusType
import com.example.sockapp.data.models.userstatus.GroupStatusDetail
import com.example.sockapp.data.models.userstatus.UserGeneratedStatusPreset
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query // Added for orderBy
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

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    // Status related state
    val groupStatusDetails: Map<String, GroupStatusDetail?> = emptyMap(), // Key: GroupId
    val isLoadingGroupStatus: Boolean = false,
    val userStatusPresets: List<UserGeneratedStatusPreset> = emptyList(),
    val isLoadingPresets: Boolean = false,

    // General update state
    val isUpdating: Boolean = false,
    val updateSuccessMessage: String? = null,

    // Account Deletion State
    val accountDeletionLoading: Boolean = false,
    val accountDeletionSuccess: Boolean = false,
    val accountDeletionError: String? = null
)

class ProfileViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val functions: FirebaseFunctions = Firebase.functions

    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private var currentUserId: String? = auth.currentUser?.uid

    init {
        currentUserId?.let { uid ->
            listenToUserProfile(uid)
            fetchUserStatusPresets(uid)
        }

        auth.addAuthStateListener { firebaseAuth ->
            val newUserId = firebaseAuth.currentUser?.uid
            if (newUserId == null) {
                _profileState.value = ProfileState()
                currentUserId = null
            } else if (currentUserId != newUserId) {
                currentUserId = newUserId
                listenToUserProfile(newUserId)
                fetchUserStatusPresets(newUserId)
                _profileState.update { it.copy(groupStatusDetails = emptyMap()) }
            }
        }
    }

    private fun listenToUserProfile(userId: String) {
        _profileState.update { it.copy(isLoading = true, error = null) }
        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _profileState.update { it.copy(error = "Failed to listen to profile: ${e.message}", isLoading = false) }
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                _profileState.update { it.copy(user = user, isLoading = false) }
            }
    }

    fun clearMessagesAndErrors() {
        _profileState.update { it.copy(error = null, updateSuccessMessage = null, accountDeletionError = null) }
    }

    // --- Profile & Social Enhancements ---
    fun updateUserProfileData(
        bio: String?,
        socialMediaLinks: Map<String, String>?, // Key: SocialPlatform.key
        profileImageUrl: String? // Keep existing profile image update
    ) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "User not authenticated.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            val updates = mutableMapOf<String, Any?>()
            // Only add to updates if the value has actually changed or is being set for the first time.
            if (bio != _profileState.value.user?.bio) updates["bio"] = bio
            if (socialMediaLinks != _profileState.value.user?.socialMediaLinks) updates["socialMediaLinks"] = socialMediaLinks
            if (profileImageUrl != _profileState.value.user?.profileImageUrl && profileImageUrl != null) { // Allow clearing if passed null intentionally
                 updates["profileImageUrl"] = profileImageUrl
            } else if (profileImageUrl == null && _profileState.value.user?.profileImageUrl != null) {
                 updates["profileImageUrl"] = FieldValue.delete() // Explicitly delete if null is passed and field exists
            }


            if (updates.isEmpty()) {
                _profileState.update { it.copy(isUpdating = false, error = "No changes to update.") }
                return@launch
            }
            try {
                db.collection("users").document(userId).update(updates).await()
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = "Profile updated.") }
            } catch (e: Exception) {
                _profileState.update { it.copy(isUpdating = false, error = "Failed to update profile: ${e.message}") }
            }
        }
    }

    fun updateNotificationPreferences(preferences: Map<String, Boolean>) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "User not authenticated.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update("notificationPreferences", preferences) // Overwrites the whole map
                    .await()
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = "Notification preferences updated.") }
            } catch (e: Exception) {
                _profileState.update { it.copy(isUpdating = false, error = "Failed to update preferences: ${e.message}") }
            }
        }
    }

    // Helper to update a single notification preference
    fun updateSingleNotificationPreference(prefType: NotificationType, enabled: Boolean) {
        val currentPrefs = _profileState.value.user?.notificationPreferences?.toMutableMap() ?: mutableMapOf()
        currentPrefs[prefType.key] = enabled
        updateNotificationPreferences(currentPrefs)
    }


    fun setDefaultGroupOnOpen(groupId: String?) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "User not authenticated.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update("defaultGroupIdOnOpen", groupId) // Sets or clears the field
                    .await()
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = "Default group preference updated.") }
            } catch (e: Exception) {
                _profileState.update { it.copy(isUpdating = false, error = "Failed to update default group: ${e.message}") }
            }
        }
    }


    // --- Global Status Update (from previous iteration, ensure it's compatible) ---
    fun updateUserGlobalStatus(
        activeStatusId: String?,
        customText: String?,
        customIconKey: String?,
        expiresAt: Timestamp?,
        overwriteAllGroupStatuses: Boolean?
    ) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "User not authenticated.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            val updates = mutableMapOf<String, Any?>(
                "activeStatusId" to activeStatusId,
                "globalCustomStatusText" to customText,
                "globalCustomStatusIconKey" to customIconKey,
                "globalStatusExpiresAt" to expiresAt
            )
            overwriteAllGroupStatuses?.let {
                updates["overwriteAllGroupStatusesWithGlobal"] = it
            }

            try {
                db.collection("users").document(userId).update(updates).await()
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = "Global status updated.") }
            } catch (e: Exception) {
                _profileState.update { it.copy(isUpdating = false, error = "Failed to update global status: ${e.message}") }
            }
        }
    }

    // --- Group-Specific Status (from previous iteration) ---
    fun fetchGroupSpecificStatus(groupId: String) {
        val userId = currentUserId ?: return
        _profileState.update { it.copy(isLoadingGroupStatus = true) }
        viewModelScope.launch {
            try {
                val docRef = db.collection("users").document(userId)
                    .collection("groupStatusDetails").document(groupId)
                val snapshot = docRef.get().await()
                val statusDetail = snapshot.toObject(GroupStatusDetail::class.java)
                _profileState.update {
                    val newMap = it.groupStatusDetails.toMutableMap()
                    newMap[groupId] = statusDetail
                    it.copy(groupStatusDetails = newMap, isLoadingGroupStatus = false)
                }
            } catch (e: Exception) {
                _profileState.update { it.copy(error = "Failed to fetch status for group $groupId: ${e.message}", isLoadingGroupStatus = false) }
            }
        }
    }

    fun updateGroupSpecificStatus(
        groupId: String,
        type: CustomStatusType,
        activeStatusReferenceId: String?,
        customText: String?,
        customIconKey: String?,
        expiresAt: Timestamp?
    ) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "User not authenticated.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            val statusDetail = GroupStatusDetail(
                groupId = groupId,
                type = type,
                activeStatusReferenceId = activeStatusReferenceId,
                customText = customText,
                customIconKey = customIconKey,
                expiresAt = expiresAt
                // lastUpdatedAt will be set by server via @ServerTimestamp
            )
            try {
                db.collection("users").document(userId)
                    .collection("groupStatusDetails").document(groupId)
                    .set(statusDetail)
                    .await()
                _profileState.update {
                    val newMap = it.groupStatusDetails.toMutableMap()
                    newMap[groupId] = statusDetail
                    it.copy(isUpdating = false, updateSuccessMessage = "Status for group $groupId updated.", groupStatusDetails = newMap)
                }
            } catch (e: Exception) {
                _profileState.update { it.copy(isUpdating = false, error = "Failed to update status for group $groupId: ${e.message}") }
            }
        }
    }

    fun clearGroupSpecificStatus(groupId: String) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "User not authenticated.") }
         _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("groupStatusDetails").document(groupId)
                    .delete()
                    .await()
                _profileState.update {
                    val newMap = it.groupStatusDetails.toMutableMap()
                    newMap.remove(groupId)
                    it.copy(isUpdating = false, updateSuccessMessage = "Status for group $groupId cleared.", groupStatusDetails = newMap)
                }
            } catch (e: Exception) {
                _profileState.update { it.copy(isUpdating = false, error = "Failed to clear status for group $groupId: ${e.message}") }
            }
        }
    }

    // --- User-Generated Status Presets (from previous iteration) ---
    fun fetchUserStatusPresets(userIdToFetch: String? = currentUserId) {
        val userId = userIdToFetch ?: return
        _profileState.update { it.copy(isLoadingPresets = true) }
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(userId)
                    .collection("customStatusPresets")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get().await()
                val presets = snapshot.toObjects(UserGeneratedStatusPreset::class.java)
                _profileState.update { it.copy(userStatusPresets = presets, isLoadingPresets = false) }
            } catch (e: Exception) {
                _profileState.update { it.copy(error = "Failed to fetch status presets: ${e.message}", isLoadingPresets = false) }
            }
        }
    }

    fun saveUserStatusPreset(presetName: String, statusText: String, iconKey: String, presetIdToUpdate: String? = null) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "User not authenticated.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            try {
                val collectionRef = db.collection("users").document(userId).collection("customStatusPresets")
                val presetDocRef = if (presetIdToUpdate != null) collectionRef.document(presetIdToUpdate) else collectionRef.document()

                val currentTimestamp = FieldValue.serverTimestamp()
                val existingPreset = if(presetIdToUpdate != null) _profileState.value.userStatusPresets.find{it.presetId == presetIdToUpdate} else null

                val preset = UserGeneratedStatusPreset(
                    presetId = presetDocRef.id,
                    userId = userId,
                    presetName = presetName,
                    statusText = statusText,
                    iconKey = iconKey,
                    createdAt = existingPreset?.createdAt ?: currentTimestamp, // Keep original if updating, else new timestamp
                    lastUpdatedAt = currentTimestamp
                )
                presetDocRef.set(preset).await()
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = "Status preset saved.") }
                fetchUserStatusPresets(userId)
            } catch (e: Exception) {
                _profileState.update { it.copy(isUpdating = false, error = "Failed to save preset: ${e.message}") }
            }
        }
    }

    fun deleteUserStatusPreset(presetId: String) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "User not authenticated.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .collection("customStatusPresets").document(presetId)
                    .delete().await()
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = "Status preset deleted.") }
                fetchUserStatusPresets(userId)
            } catch (e: Exception) {
                _profileState.update { it.copy(isUpdating = false, error = "Failed to delete preset: ${e.message}") }
            }
        }
    }

    // --- Account Deletion (from Module 1/Settings) ---
    fun deleteAccount() {
        val userId = currentUserId ?: return _profileState.update { it.copy(accountDeletionError = "User not authenticated.") }
        _profileState.update { it.copy(accountDeletionLoading = true, accountDeletionError = null, accountDeletionSuccess = false) }
        viewModelScope.launch {
            try {
                functions.getHttpsCallable("deleteUserAccount").call().await()
                _profileState.update { it.copy(accountDeletionLoading = false, accountDeletionSuccess = true) }
            } catch (e: Exception) {
                _profileState.update { it.copy(accountDeletionLoading = false, accountDeletionError = "Failed to delete account: ${e.message}") }
            }
        }
    }

    fun clearAccountDeletionStatus() {
        _profileState.update { it.copy(accountDeletionError = null, accountDeletionLoading = false, accountDeletionSuccess = false) }
    }
}
