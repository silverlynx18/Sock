package com.example.sockapp.data.models.group

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

// Stored at: groups/{groupId}/managedInviteLinks/{linkId}
@Parcelize
data class ManagedInviteLink(
    val linkId: String = "", // Document ID, same as the unique code for simplicity or separate field
    val code: String = "",   // Short, unique (per group or globally), shareable code for the link URL

    val groupId: String = "", // Denormalized for easier access if needed, though path contains it
    val groupName: String? = null, // Denormalized at creation time

    val createdBy: String = "", // UID of admin/owner who created it
    val createdAt: Timestamp = Timestamp.now(),

    var uses: Long = 0L, // Current number of times this link has been used to generate an Invitation
    val maxUses: Long? = null, // e.g., 100 uses. Null for unlimited.
    val expiresAt: Timestamp? = null, // e.g., valid for 7 days. Null for no expiry.
    val roleToAssign: GroupRole = GroupRole.MEMBER, // Role assigned to user accepting through this link
    var isActive: Boolean = true // Admin can toggle this to revoke the link
) : Parcelable {
    // No-argument constructor for Firebase deserialization
    constructor() : this(
        linkId = "",
        code = "",
        groupId = "",
        groupName = null,
        createdBy = "",
        createdAt = Timestamp.now(),
        uses = 0L,
        maxUses = null,
        expiresAt = null,
        roleToAssign = GroupRole.MEMBER,
        isActive = true
    )
}
