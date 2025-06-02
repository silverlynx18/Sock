package com.example.sockapp.data.models.userstatus

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

// Represents a custom status preset saved by a user.
// Stored in `users/{userId}/customStatusPresets/{presetId}`.
data class UserGeneratedStatusPreset(
    val presetId: String = "", // Firestore Document ID
    val userId: String = "",   // UID of the user who owns this preset

    val presetName: String = "", // User-defined name for this preset (e.g., "Working from home")
    val statusText: String = "",
    val iconKey: String = "",    // Key for an icon (e.g., from a predefined list of selectable icons)

    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val lastUpdatedAt: Timestamp? = null
) {
    // No-argument constructor for Firebase deserialization
    constructor() : this(
        presetId = "",
        userId = "",
        presetName = "",
        statusText = "",
        iconKey = "",
        createdAt = null,
        lastUpdatedAt = null
    )
}
