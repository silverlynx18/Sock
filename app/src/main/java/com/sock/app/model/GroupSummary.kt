package com.sock.app.model

data class GroupSummary(
    val id: String,
    val name: String,
    val memberCount: Int,
    val primaryColor: Long,
    val secondaryColor: Long,
    val status: AvailabilityStatus
)
