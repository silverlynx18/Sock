package com.sock.app.data.repository

import com.sock.app.data.api.ApiService

// This repository is kept for compatibility but now uses REST API
class FirebaseFunctionsRepository {
    private val apiService = ApiService()

    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return apiService.checkUsernameAvailability(username)
    }

    suspend fun createGroup(
        groupName: String,
        primaryColor: String,
        secondaryColor: String,
        groupProfilePictureUrl: String?,
        creatorUid: String,
        creatorUsername: String
    ): Result<Map<String, String>> {
        return try {
            val request = com.sock.app.data.api.CreateGroupRequest(
                groupName = groupName,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                groupProfilePictureUrl = groupProfilePictureUrl
            )
            val result = apiService.createGroup(request)
            result.map { mapOf("groupId" to it.groupId, "inviteLinkCode" to it.inviteLinkCode) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processInviteLink(
        inviteLinkCode: String,
        invitedUserUid: String
    ): Result<Map<String, String>> {
        return try {
            val result = apiService.processInviteLink(inviteLinkCode)
            result.map { invitation ->
                mapOf(
                    "success" to "true",
                    "invitationId" to invitation.invitationId,
                    "message" to "Invitation processed"
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptInvitation(
        invitationId: String,
        acceptingUserUid: String,
        acceptingUserUsername: String
    ): Result<String> {
        return try {
            val result = apiService.acceptInvitation(invitationId)
            result.map { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineInvitation(
        invitationId: String,
        decliningUserUid: String
    ): Result<String> {
        return try {
            val result = apiService.declineInvitation(invitationId)
            result.map { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccount(userIdToDelete: String): Result<String> {
        return apiService.deleteUserAccount(userIdToDelete)
    }
}
