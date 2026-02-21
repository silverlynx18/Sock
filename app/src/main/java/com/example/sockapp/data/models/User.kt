package com.example.sockapp.data.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "", // Storing email, ensure security rules protect it
    val phoneNumber: String? = null, // E.164 format, for contact matching & phone auth (if enabled)
    val profileImageUrl: String? = null,

    // --- Profile & Social Enhancements ---
    val bio: String? = null,
    val socialMediaLinks: Map<String, String>? = null,
    val notificationPreferences: Map<String, Boolean>? = null,
    val defaultGroupIdOnOpen: String? = null,

    // --- Global Status Fields (from previous iteration) ---
    val activeStatusId: String? = "online",
    val globalCustomStatusText: String? = null,
    val globalCustomStatusIconKey: String? = null,
    val globalStatusExpiresAt: Timestamp? = null,
    val overwriteAllGroupStatusesWithGlobal: Boolean = false,

    // --- General Timestamps ---
    val createdAt: Timestamp = Timestamp.now(),
    val lastLogin: Timestamp? = null,

    // --- Denormalized lists for client-side querying efficiency ---
    val joinedGroupIds: List<String> = emptyList(),

    // Potentially add:
    // val followerCount: Int = 0,
    // val followingCount: Int = 0,
    // val isOnline: Boolean = false,
) : Parcelable {
    // No-argument constructor for Firebase deserialization
    constructor() : this(
        userId = "",
        username = "",
        email = "",
        phoneNumber = null,
        profileImageUrl = null,
        bio = null,
        socialMediaLinks = null,
        notificationPreferences = null,
        defaultGroupIdOnOpen = null,
        activeStatusId = "online",
        globalCustomStatusText = null,
        globalCustomStatusIconKey = null,
        globalStatusExpiresAt = null,
        overwriteAllGroupStatusesWithGlobal = false,
        createdAt = Timestamp.now(),
        lastLogin = null,
        joinedGroupIds = emptyList()
    )
}
