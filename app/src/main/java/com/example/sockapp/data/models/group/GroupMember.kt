package com.example.sockapp.data.models.group

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class GroupMember(
    val userId: String = "", // Corresponds to Firebase Auth UID
    val role: GroupRole = GroupRole.MEMBER,
    val joinedAt: Timestamp = Timestamp.now(),
    // Denormalized fields from the User's profile for efficient display in member lists.
    // These should be updated if the corresponding User profile changes.
    val displayName: String? = null,
    val photoUrl: String? = null,
    // val lastSeenStatusId: String? = null, // Optional: if displaying live status within group context
    // val customStatusText: String? = null  // Optional: if displaying live status
) : Parcelable {
    // No-argument constructor for Firebase Firestore deserialization
    constructor() : this(
        userId = "",
        role = GroupRole.MEMBER,
        joinedAt = Timestamp.now(),
        displayName = null,
        photoUrl = null
    )
}
