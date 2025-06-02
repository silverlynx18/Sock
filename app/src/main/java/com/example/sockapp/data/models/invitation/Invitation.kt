package com.example.sockapp.data.models.invitation

import android.os.Parcelable
import com.example.sockapp.data.models.group.GroupRole // Assuming this path is correct from Module 3
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Invitation(
    val invitationId: String = "", // Firestore Document ID
    var type: InvitationType = InvitationType.DIRECT_USER_ID, // Made var in case it's updated by CF, e.g. generic link to specific user

    val groupId: String = "",
    val groupName: String? = null,
    val groupImageUrl: String? = null,

    val inviterId: String = "",
    val inviterName: String? = null,
    val inviterPhotoUrl: String? = null,

    var inviteeId: String? = null, // Can be updated by CFs (e.g., email/phone/username match, or link claim)
    val inviteeEmail: String? = null,
    val inviteeUsername: String? = null,
    val inviteePhoneNumber: String? = null, // E.164 format, for PHONE_CONTACT type

    var status: InvitationStatus = InvitationStatus.PENDING,
    val roleToAssign: GroupRole = GroupRole.MEMBER,

    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp? = null,
    var processedAt: Timestamp? = null,

    var isUsernameResolved: Boolean? = null,
    var resolutionError: String? = null,

    // If this invitation was generated from an admin-created ManagedInviteLink
    val originatingManagedLinkId: String? = null
) : Parcelable {
    constructor() : this( // For Firestore
        invitationId = "",
        type = InvitationType.DIRECT_USER_ID,
        groupId = "",
        groupName = null,
        groupImageUrl = null,
        inviterId = "",
        inviterName = null,
        inviterPhotoUrl = null,
        inviteeId = null,
        inviteeEmail = null,
        inviteeUsername = null,
        inviteePhoneNumber = null,
        status = InvitationStatus.PENDING,
        roleToAssign = GroupRole.MEMBER,
        createdAt = Timestamp.now(),
        expiresAt = null,
        processedAt = null,
        isUsernameResolved = null,
        resolutionError = null,
        originatingManagedLinkId = null
    )
}
