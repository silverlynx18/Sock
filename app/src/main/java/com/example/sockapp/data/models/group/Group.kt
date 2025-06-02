package com.example.sockapp.data.models.group

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Group(
    val groupId: String = "",
    val name: String = "",
    val description: String? = null,
    val bannerImageUrl: String? = null,    // URL for the group's banner image
    val profileImageUrl: String? = null,   // URL for the group's profile image (avatar)
    val creatorId: String = "",            // UID of the user who created the group
    val createdAt: Timestamp = Timestamp.now(),
    var memberCount: Long = 0L,             // Number of members in the group, updated by Cloud Functions
    val isPublic: Boolean = true,          // True if the group is public, false if private
    val tags: List<String>? = null,        // List of tags for group discoverability

    // Optional fields for chat-like features within the group
    // val lastMessageText: String? = null,
    // val lastMessageAt: Timestamp? = null,
    // val lastMessageSenderId: String? = null, // UID of the sender of the last message
    // val lastMessageSenderName: String? = null // Denormalized name of the last message sender
) : Parcelable {
    // No-argument constructor for Firebase Firestore deserialization
    constructor() : this(
        groupId = "",
        name = "",
        description = null,
        bannerImageUrl = null,
        profileImageUrl = null,
        creatorId = "",
        createdAt = Timestamp.now(),
        memberCount = 0L,
        isPublic = true,
        tags = null
    )
}
