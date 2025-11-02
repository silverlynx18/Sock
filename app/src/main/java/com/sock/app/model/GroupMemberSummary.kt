package com.sock.app.model

data class GroupMemberSummary(
    val id: String,
    val displayName: String,
    val username: String,
    val status: AvailabilityStatus,
    val isAdmin: Boolean = false
)
