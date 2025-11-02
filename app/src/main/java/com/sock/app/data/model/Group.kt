package com.sock.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class GroupMember(
    val role: String = "member", // "owner", "admin", "member"
    val username: String = "",
    val joinedAt: Timestamp? = null
) {
    companion object {
        fun fromMap(map: Map<String, Any>): GroupMember? {
            return try {
                GroupMember(
                    role = map["role"] as? String ?: "member",
                    username = map["username"] as? String ?: "",
                    joinedAt = map["joinedAt"] as? Timestamp
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class Group(
    val groupId: String = "",
    val name: String = "",
    val groupProfilePictureUrl: String? = null,
    val primaryColor: String = "#6200EE",
    val secondaryColor: String = "#03DAC6",
    val createdAt: Timestamp? = null,
    val ownerId: String = "",
    val inviteLinkCode: String = "",
    val members: Map<String, GroupMember> = emptyMap() // userID -> GroupMember
) {
    companion object {
        fun fromDocument(document: DocumentSnapshot): Group? {
            return try {
                val membersMap = (document.get("members") as? Map<*, *>)?.mapNotNull { entry ->
                    val userId = entry.key.toString()
                    val memberData = entry.value as? Map<*, *>
                    memberData?.let {
                        userId to GroupMember.fromMap(it.mapKeys { k -> k.key.toString() }
                            .mapValues { v -> v.value as Any })
                    }
                }?.toMap() ?: emptyMap()

                Group(
                    groupId = document.id,
                    name = document.getString("name") ?: "",
                    groupProfilePictureUrl = document.getString("groupProfilePictureUrl"),
                    primaryColor = document.getString("primaryColor") ?: "#6200EE",
                    secondaryColor = document.getString("secondaryColor") ?: "#03DAC6",
                    createdAt = document.getTimestamp("createdAt"),
                    ownerId = document.getString("ownerId") ?: "",
                    inviteLinkCode = document.getString("inviteLinkCode") ?: "",
                    members = membersMap
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
