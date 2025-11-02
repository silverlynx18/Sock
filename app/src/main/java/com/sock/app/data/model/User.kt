package com.sock.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class User(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String? = null,
    val createdAt: Timestamp? = null,
    val globalStatusId: String? = null,
    val groupSpecificStatuses: Map<String, String> = emptyMap(), // groupID -> statusID
    val groups: List<String> = emptyList() // groupIDs
) {
    companion object {
        fun fromDocument(document: DocumentSnapshot): User? {
            return try {
                User(
                    uid = document.id,
                    username = document.getString("username") ?: "",
                    displayName = document.getString("displayName") ?: "",
                    email = document.getString("email") ?: "",
                    phoneNumber = document.getString("phoneNumber") ?: "",
                    profilePictureUrl = document.getString("profilePictureUrl"),
                    createdAt = document.getTimestamp("createdAt"),
                    globalStatusId = document.getString("globalStatusId"),
                    groupSpecificStatuses = (document.get("groupSpecificStatuses") as? Map<*, *>)
                        ?.mapKeys { it.key.toString() }
                        ?.mapValues { it.value.toString() }
                        ?: emptyMap(),
                    groups = (document.get("groups") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
