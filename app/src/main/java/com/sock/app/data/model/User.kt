package com.sock.app.data.model

data class User(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String? = null,
    val createdAt: Long? = null, // Unix timestamp in milliseconds
    val globalStatusId: String? = null,
    val groupSpecificStatuses: Map<String, String> = emptyMap(), // groupID -> statusID
    val groups: List<String> = emptyList() // groupIDs
)
