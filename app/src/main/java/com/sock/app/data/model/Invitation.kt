package com.sock.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

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
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    companion object {
        fun fromDocument(document: DocumentSnapshot): Invitation? {
            return try {
                val statusString = document.getString("status") ?: "pending_acceptance"
                val status = when (statusString) {
                    "pending_acceptance" -> InvitationStatus.PENDING_ACCEPTANCE
                    "accepted" -> InvitationStatus.ACCEPTED
                    "declined" -> InvitationStatus.DECLINED
                    else -> InvitationStatus.PENDING_ACCEPTANCE
                }

                Invitation(
                    invitationId = document.id,
                    groupId = document.getString("groupID") ?: "",
                    groupName = document.getString("groupName") ?: "",
                    invitedUserID = document.getString("invitedUserID") ?: "",
                    inviterUserID = document.getString("inviterUserID") ?: "",
                    status = status,
                    createdAt = document.getTimestamp("createdAt"),
                    updatedAt = document.getTimestamp("updatedAt")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
