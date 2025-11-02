package com.sock.app.model

import java.time.Instant

data class InvitationSummary(
    val id: String,
    val groupName: String,
    val inviterName: String,
    val sentAt: Instant
)
