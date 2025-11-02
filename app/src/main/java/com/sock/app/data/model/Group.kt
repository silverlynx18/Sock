package com.sock.app.data.model

data class GroupMember(
    val role: String = "member", // "owner", "admin", "member"
    val username: String = "",
    val joinedAt: Long? = null // Unix timestamp in milliseconds
)

data class Group(
    val groupId: String = "",
    val name: String = "",
    val groupProfilePictureUrl: String? = null,
    val primaryColor: String = "#6200EE",
    val secondaryColor: String = "#03DAC6",
    val createdAt: Long? = null, // Unix timestamp in milliseconds
    val ownerId: String = "",
    val inviteLinkCode: String = "",
    val members: Map<String, GroupMember> = emptyMap() // userID -> GroupMember
)
