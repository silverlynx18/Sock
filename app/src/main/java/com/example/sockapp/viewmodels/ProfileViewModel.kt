package com.example.sockapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sockapp.data.models.User
import com.example.sockapp.data.models.profile.NotificationType
import com.example.sockapp.data.models.userstatus.CustomStatusType
import com.example.sockapp.data.models.userstatus.GroupStatusDetail
import com.example.sockapp.data.models.userstatus.UserGeneratedStatusPreset
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
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

/**
 * Represents the UI state for profile-related screens.
 * @param user The current [User] object being viewed or edited.
 * @param isLoading True if user profile data is currently being loaded.
 * @param error General error message for profile operations.
 * @param groupStatusDetails Map of Group ID to [GroupStatusDetail] for the current user.
 * @param isLoadingGroupStatus True if group-specific status is being fetched.
 * @param userStatusPresets List of [UserGeneratedStatusPreset] created by the user.
 * @param isLoadingPresets True if status presets are being fetched.
 * @param isUpdating True if a profile update (bio, social links, status, etc.) is in progress.
 * @param updateSuccessMessage A message to show upon successful update.
 * @param accountDeletionLoading True if account deletion is in progress.
 * @param accountDeletionSuccess True if account deletion was successful.
 * @param accountDeletionError Error message related to account deletion.
 */
data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    val groupStatusDetails: Map<String, GroupStatusDetail?> = emptyMap(),
    val isLoadingGroupStatus: Boolean = false,
    val userStatusPresets: List<UserGeneratedStatusPreset> = emptyList(),
    val isLoadingPresets: Boolean = false,

    val isUpdating: Boolean = false,
    val updateSuccessMessage: String? = null,

    val accountDeletionLoading: Boolean = false,
    val accountDeletionSuccess: Boolean = false,
    val accountDeletionError: String? = null
)

class ProfileViewModel : ViewModel() {

    // Firebase services instances
    internal val auth: FirebaseAuth = Firebase.auth // internal for access in previews if needed, otherwise private
    private val db: FirebaseFirestore = Firebase.firestore
    private val functions: FirebaseFunctions = Firebase.functions

    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    // Store the UID of the currently authenticated user
    // This is updated by the authStateListener
    var currentUserId: String? = auth.currentUser?.uid
        private set

    init {
        // Initial data fetch for the logged-in user
        currentUserId?.let { uid ->
            listenToUserProfile(uid)
            fetchUserStatusPresets(uid)
        }

        // Listen for changes in authentication state (login/logout)
        auth.addAuthStateListener { firebaseAuth ->
            val newUserId = firebaseAuth.currentUser?.uid
            if (newUserId == null && currentUserId != null) { // User logged out
                _profileState.value = ProfileState() // Reset state
                currentUserId = null
            } else if (currentUserId != newUserId && newUserId != null) { // User changed or logged in
                currentUserId = newUserId
                listenToUserProfile(newUserId)
                fetchUserStatusPresets(newUserId)
                _profileState.update { it.copy(groupStatusDetails = emptyMap()) } // Clear previous user's group statuses
            }
        }
    }

    /**
     * Sets up a real-time listener for the user's profile document in Firestore.
     * Updates the [ProfileState.user] field.
     * @param userId The UID of the user whose profile to listen to.
     */
    fun listenToUserProfile(userId: String) {
        _profileState.update { it.copy(isLoading = true, error = null) }
        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Firebase.functions.logger.error("ProfileViewModel: Error listening to user profile for $userId", e)
                    _profileState.update { it.copy(error = "Unable to load profile. Please check your connection.", isLoading = false) }
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                _profileState.update { it.copy(user = user, isLoading = false) }
            }
    }

    /**
     * Clears any general error, success messages, or account deletion errors from the UI state.
     */
    fun clearMessagesAndErrors() {
        _profileState.update { it.copy(error = null, updateSuccessMessage = null, accountDeletionError = null) }
    }

    /**
     * Updates general user profile fields like bio, social media links, and profile image URL.
     * @param bio The new bio text.
     * @param socialMediaLinks A map of social media platform keys to user handles/URLs.
     * @param profileImageUrl The new URL for the profile image.
     */
    fun updateUserProfileData(
        bio: String?,
        socialMediaLinks: Map<String, String>?,
        profileImageUrl: String?
    ) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "Please sign in to update your profile.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            val updates = mutableMapOf<String, Any?>()
            if (bio != _profileState.value.user?.bio) updates["bio"] = bio?.trim()
            if (socialMediaLinks != _profileState.value.user?.socialMediaLinks) updates["socialMediaLinks"] = socialMediaLinks

            val currentImageUrl = _profileState.value.user?.profileImageUrl
            if (profileImageUrl != currentImageUrl) { // Checks if different, including if one is null
                updates["profileImageUrl"] = profileImageUrl // Setting to null will delete field or set as null
            }

            if (updates.isEmpty()) {
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = "No changes detected.") }
                return@launch
            }
            try {
                db.collection("users").document(userId).update(updates).await()
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = "Profile updated successfully.") }
            } catch (e: Exception) {
                Firebase.functions.logger.error("ProfileViewModel: Error updating profile data for $userId", e)
                _profileState.update { it.copy(isUpdating = false, error = "Failed to update profile. Please try again.") }
            }
        }
    }

    /**
     * Updates the entire map of notification preferences for the user.
     * @param preferences A map where keys are [NotificationType.key] and values are booleans.
     */
    fun updateNotificationPreferences(preferences: Map<String, Boolean>) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "Please sign in to update preferences.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update("notificationPreferences", preferences)
                    .await()
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = "Notification preferences updated.") }
            } catch (e: Exception) {
                 Firebase.functions.logger.error("ProfileViewModel: Error updating notification preferences for $userId", e)
                _profileState.update { it.copy(isUpdating = false, error = "Failed to update preferences. Please try again.") }
            }
        }
    }

    /**
     * Updates a single notification preference.
     * @param prefType The [NotificationType] to update.
     * @param enabled The new boolean value for the preference.
     */
    fun updateSingleNotificationPreference(prefType: NotificationType, enabled: Boolean) {
        val currentPrefs = _profileState.value.user?.notificationPreferences?.toMutableMap() ?: mutableMapOf()
        currentPrefs[prefType.key] = enabled
        updateNotificationPreferences(currentPrefs)
    }

    /**
     * Sets or clears the user's default group to navigate to when the app opens.
     * @param groupId The ID of the group to set as default, or null to clear.
     */
    fun setDefaultGroupOnOpen(groupId: String?) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "Please sign in to set default group.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            try {
                db.collection("users").document(userId)
                    .update("defaultGroupIdOnOpen", groupId)
                    .await()
                val message = if (groupId != null) "Default group set." else "Default group cleared."
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = message) }
            } catch (e: Exception) {
                 Firebase.functions.logger.error("ProfileViewModel: Error setting default group for $userId", e)
                _profileState.update { it.copy(isUpdating = false, error = "Failed to update default group preference.") }
            }
        }
    }

    // --- Global Status Update ---
    fun updateUserGlobalStatus( /* ... existing code from previous iteration with refined error messages ... */
        activeStatusId: String?, customText: String?, customIconKey: String?,
        expiresAt: Timestamp?, overwriteAllGroupStatuses: Boolean?
    ) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "Please sign in to update status.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            val updates = mutableMapOf<String, Any?>(
                "activeStatusId" to activeStatusId,
                "globalCustomStatusText" to customText,
                "globalCustomStatusIconKey" to customIconKey,
                "globalStatusExpiresAt" to expiresAt
            )
            overwriteAllGroupStatuses?.let { updates["overwriteAllGroupStatusesWithGlobal"] = it }
            try {
                db.collection("users").document(userId).update(updates).await()
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = "Global status updated.") }
            } catch (e: Exception) {
                Firebase.functions.logger.error("ProfileViewModel: Error updating global status for $userId", e)
                _profileState.update { it.copy(isUpdating = false, error = "Failed to update global status.") }
            }
        }
    }

    // --- Group-Specific Status ---
    fun fetchGroupSpecificStatus(groupId: String) { /* ... existing code with refined error messages ... */
        val userId = currentUserId ?: return
        _profileState.update { it.copy(isLoadingGroupStatus = true) }
        viewModelScope.launch {
            try {
                val statusDetail = db.collection("users").document(userId)
                    .collection("groupStatusDetails").document(groupId)
                    .get().await().toObject(GroupStatusDetail::class.java)
                _profileState.update {
                    val newMap = it.groupStatusDetails.toMutableMap(); newMap[groupId] = statusDetail
                    it.copy(groupStatusDetails = newMap, isLoadingGroupStatus = false)
                }
            } catch (e: Exception) {
                Firebase.functions.logger.error("ProfileViewModel: Error fetching group status for $userId in group $groupId", e)
                _profileState.update { it.copy(error = "Failed to load status for group.", isLoadingGroupStatus = false) }
            }
        }
    }
    fun updateGroupSpecificStatus( /* ... existing code with refined error messages ... */
        groupId: String, type: CustomStatusType, activeStatusReferenceId: String?,
        customText: String?, customIconKey: String?, expiresAt: Timestamp?
    ) {
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "Please sign in to update status.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            val statusDetail = GroupStatusDetail(groupId, type, activeStatusReferenceId, customText, customIconKey, expiresAt)
            try {
                db.collection("users").document(userId).collection("groupStatusDetails").document(groupId).set(statusDetail).await()
                _profileState.update {
                    val newMap = it.groupStatusDetails.toMutableMap(); newMap[groupId] = statusDetail
                    it.copy(isUpdating = false, updateSuccessMessage = "Status for group updated.", groupStatusDetails = newMap)
                }
            } catch (e: Exception) {
                 Firebase.functions.logger.error("ProfileViewModel: Error updating group status for $userId in group $groupId", e)
                _profileState.update { it.copy(isUpdating = false, error = "Failed to update group status.") }
            }
        }
    }
    fun clearGroupSpecificStatus(groupId: String) { /* ... existing code with refined error messages ... */
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "Please sign in to clear status.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            try {
                db.collection("users").document(userId).collection("groupStatusDetails").document(groupId).delete().await()
                _profileState.update {
                    val newMap = it.groupStatusDetails.toMutableMap(); newMap.remove(groupId)
                    it.copy(isUpdating = false, updateSuccessMessage = "Group-specific status cleared.", groupStatusDetails = newMap)
                }
            } catch (e: Exception) {
                Firebase.functions.logger.error("ProfileViewModel: Error clearing group status for $userId in group $groupId", e)
                _profileState.update { it.copy(isUpdating = false, error = "Failed to clear group status.") }
            }
        }
    }

    // --- User-Generated Status Presets ---
    fun fetchUserStatusPresets(userIdToFetch: String? = currentUserId) { /* ... existing code ... */ }
    fun saveUserStatusPreset(presetName: String, statusText: String, iconKey: String, presetIdToUpdate: String? = null) { /* ... existing code with refined error messages ... */
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "Please sign in to save presets.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            try { /* ... preset saving logic ... */
                val collectionRef = db.collection("users").document(userId).collection("customStatusPresets")
                val presetDocRef = if (presetIdToUpdate != null) collectionRef.document(presetIdToUpdate) else collectionRef.document()
                val currentTimestamp = FieldValue.serverTimestamp()
                val existingPreset = if(presetIdToUpdate != null) _profileState.value.userStatusPresets.find{it.presetId == presetIdToUpdate} else null
                val preset = UserGeneratedStatusPreset(presetDocRef.id, userId, presetName, statusText, iconKey, existingPreset?.createdAt ?: currentTimestamp, currentTimestamp)
                presetDocRef.set(preset).await()
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = "Status preset saved.") }
                fetchUserStatusPresets(userId)
            } catch (e: Exception) {
                Firebase.functions.logger.error("ProfileViewModel: Error saving status preset for $userId", e)
                _profileState.update { it.copy(isUpdating = false, error = "Failed to save preset.") }
            }
        }
    }
    fun deleteUserStatusPreset(presetId: String) { /* ... existing code with refined error messages ... */
        val userId = currentUserId ?: return _profileState.update { it.copy(error = "Please sign in to delete presets.") }
        _profileState.update { it.copy(isUpdating = true, error = null, updateSuccessMessage = null) }
        viewModelScope.launch {
            try {
                db.collection("users").document(userId).collection("customStatusPresets").document(presetId).delete().await()
                _profileState.update { it.copy(isUpdating = false, updateSuccessMessage = "Status preset deleted.") }
                fetchUserStatusPresets(userId)
            } catch (e: Exception) {
                Firebase.functions.logger.error("ProfileViewModel: Error deleting status preset $presetId for $userId", e)
                _profileState.update { it.copy(isUpdating = false, error = "Failed to delete preset.") }
            }
        }
    }

    // --- Account Deletion ---
    fun deleteAccount() { /* ... existing code with refined error messages ... */
        val userId = currentUserId ?: return _profileState.update { it.copy(accountDeletionError = "Please sign in to delete your account.") }
        _profileState.update { it.copy(accountDeletionLoading = true, accountDeletionError = null, accountDeletionSuccess = false) }
        viewModelScope.launch {
            try {
                functions.getHttpsCallable("deleteUserAccount").call().await()
                _profileState.update { it.copy(accountDeletionLoading = false, accountDeletionSuccess = true) }
            } catch (e: Exception) {
                Firebase.functions.logger.error("ProfileViewModel: Error deleting account for $userId", e)
                _profileState.update { it.copy(accountDeletionLoading = false, accountDeletionError = "Account deletion failed. Please try again or contact support.") }
            }
        }
    }
    fun clearAccountDeletionStatus() { /* ... existing code ... */ }
}
