package com.example.sockapp.data.models.userstatus

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

// Represents a user's status specific to a group.
// Documents will be stored in `users/{userId}/groupStatusDetails/{groupId}`.
// The document ID for these will be the groupId.
data class GroupStatusDetail(
    val groupId: String = "", // Redundant if doc ID is groupId, but good for data integrity / queries

    // Type of status: Is it an app preset, a user-saved preset, or an ad-hoc custom message?
    val type: CustomStatusType = CustomStatusType.APP_PRESET,

    // If type is APP_PRESET, this holds the ID of the AppPresetStatus (e.g., "online", "busy").
    // If type is USER_GENERATED_PRESET, this holds the ID of the UserGeneratedStatusPreset.
    // If type is AD_HOC_CUSTOM, this might be null or a generic "custom" key.
    val activeStatusReferenceId: String? = AppPresetStatus.Online.id, // Default to "online"

    // For AD_HOC_CUSTOM type, or if a USER_GENERATED_PRESET's text is overridden for this specific group instance.
    val customText: String? = null,
    // For AD_HOC_CUSTOM type, or if a USER_GENERATED_PRESET's icon is overridden. Refers to an icon key.
    val customIconKey: String? = null,

    val expiresAt: Timestamp? = null, // When this group-specific status should automatically clear

    @ServerTimestamp
    val lastUpdatedAt: Timestamp? = null
) {
    // No-argument constructor for Firebase deserialization
    constructor() : this(
        groupId = "",
        type = CustomStatusType.APP_PRESET,
        activeStatusReferenceId = AppPresetStatus.Online.id,
        customText = null,
        customIconKey = null,
        expiresAt = null,
        lastUpdatedAt = null
    )
}
