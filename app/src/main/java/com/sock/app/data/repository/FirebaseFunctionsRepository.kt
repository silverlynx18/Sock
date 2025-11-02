package com.sock.app.data.repository

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.tasks.await

class FirebaseFunctionsRepository {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            val result = functions
                .getHttpsCallable("checkUsernameAvailability")
                .call(mapOf("username" to username))
                .await()
            
            val data = result.data as? Map<*, *>
            val isAvailable = data?.get("isAvailable") as? Boolean ?: false
            Result.success(isAvailable)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            val data = mapOf(
                "groupName" to groupName,
                "primaryColor" to primaryColor,
                "secondaryColor" to secondaryColor,
                "groupProfilePictureUrl" to (groupProfilePictureUrl ?: ""),
                "creatorUid" to creatorUid,
                "creatorUsername" to creatorUsername
            )
            
            val result = functions
                .getHttpsCallable("createGroup")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
            val groupId = resultData?.get("groupId") as? String ?: ""
            val inviteLinkCode = resultData?.get("inviteLinkCode") as? String ?: ""
            
            Result.success(mapOf("groupId" to groupId, "inviteLinkCode" to inviteLinkCode))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processInviteLink(
        inviteLinkCode: String,
        invitedUserUid: String
    ): Result<Map<String, String>> {
        return try {
            val data = mapOf(
                "inviteLinkCode" to inviteLinkCode,
                "invitedUserUid" to invitedUserUid
            )
            
            val result = functions
                .getHttpsCallable("processInviteLink")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
            val success = resultData?.get("success") as? Boolean ?: false
            val invitationId = resultData?.get("invitationId") as? String ?: ""
            val message = resultData?.get("message") as? String ?: ""
            
            Result.success(mapOf(
                "success" to success.toString(),
                "invitationId" to invitationId,
                "message" to message
            ))
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
            val data = mapOf(
                "invitationId" to invitationId,
                "acceptingUserUid" to acceptingUserUid,
                "acceptingUserUsername" to acceptingUserUsername
            )
            
            val result = functions
                .getHttpsCallable("acceptInvitation")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
            val message = resultData?.get("message") as? String ?: "Invitation accepted"
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineInvitation(
        invitationId: String,
        decliningUserUid: String
    ): Result<String> {
        return try {
            val data = mapOf(
                "invitationId" to invitationId,
                "decliningUserUid" to decliningUserUid
            )
            
            val result = functions
                .getHttpsCallable("declineInvitation")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
            val message = resultData?.get("message") as? String ?: "Invitation declined"
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccount(userIdToDelete: String): Result<String> {
        return try {
            val data = mapOf("userIdToDelete" to userIdToDelete)
            
            val result = functions
                .getHttpsCallable("deleteUserAccount")
                .call(data)
                .await()
            
            val resultData = result.data as? Map<*, *>
            val message = resultData?.get("message") as? String ?: "Account deleted"
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
