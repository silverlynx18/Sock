package com.example.sockapp.data.models.invitation

enum class InvitationStatus {
    PENDING,  // Invitation sent, awaiting response
    ACCEPTED, // Invitation accepted by the recipient
    DECLINED, // Invitation declined by the recipient
    EXPIRED,  // Invitation expired due to time limit (if expiresAt is set)
    REVOKED;  // Invitation cancelled by the sender before acceptance/decline
}
