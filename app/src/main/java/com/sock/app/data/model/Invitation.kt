package com.sock.app.data.model

enum class InvitationStatus {
    PENDING_ACCEPTANCE,
    ACCEPTED,
    DECLINED
}

data class Invitation(
    val invitationId: String = "",
    val groupId: String = "",
    val groupName: String = "",
    val invitedUserID: String = "",
    val inviterUserID: String = "",
    val status: InvitationStatus = InvitationStatus.PENDING_ACCEPTANCE,
    val createdAt: Long? = null, // Unix timestamp in milliseconds
    val updatedAt: Long? = null // Unix timestamp in milliseconds
)
