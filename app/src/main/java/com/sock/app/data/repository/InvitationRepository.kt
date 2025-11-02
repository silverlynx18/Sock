package com.sock.app.data.repository

import com.sock.app.data.api.ApiService
import com.sock.app.data.api.InvitationDto
import com.sock.app.data.model.Invitation
import com.sock.app.data.model.InvitationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

class InvitationRepository {
    private val apiService = ApiService()

    suspend fun getPendingInvitations(userId: String): Result<List<Invitation>> {
        return try {
            val result = apiService.getPendingInvitations(userId)
            result.map { invitations -> invitations.mapNotNull { it.toInvitation() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observePendingInvitations(userId: String): Flow<List<Invitation>> = flow {
        // For now, just fetch once. Can be enhanced with polling or WebSocket later
        val result = getPendingInvitations(userId)
        emit(result.getOrNull() ?: emptyList())
    }

    suspend fun getInvitation(invitationId: String): Result<Invitation?> {
        // This would need a separate endpoint or we fetch from pending list
        return Result.success(null)
    }

    suspend fun processInviteLink(inviteLinkCode: String): Result<Invitation?> {
        return try {
            val result = apiService.processInviteLink(inviteLinkCode)
            result.map { it.toInvitation() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptInvitation(invitationId: String): Result<Unit> {
        return try {
            val result = apiService.acceptInvitation(invitationId)
            result.map { Unit }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineInvitation(invitationId: String): Result<Unit> {
        return try {
            val result = apiService.declineInvitation(invitationId)
            result.map { Unit }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun InvitationDto.toInvitation(): Invitation? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val createdAtTimestamp = try {
                dateFormat.parse(createdAt)?.time
            } catch (e: Exception) {
                null
            }

            val updatedAtTimestamp = updatedAt?.let {
                try {
                    dateFormat.parse(it)?.time
                } catch (e: Exception) {
                    null
                }
            }

            val status = when (status.lowercase()) {
                "pending_acceptance" -> InvitationStatus.PENDING_ACCEPTANCE
                "accepted" -> InvitationStatus.ACCEPTED
                "declined" -> InvitationStatus.DECLINED
                else -> InvitationStatus.PENDING_ACCEPTANCE
            }

            Invitation(
                invitationId = invitationId,
                groupId = groupId,
                groupName = groupName,
                invitedUserID = invitedUserID,
                inviterUserID = inviterUserID,
                status = status,
                createdAt = createdAtTimestamp,
                updatedAt = updatedAtTimestamp
            )
        } catch (e: Exception) {
            null
        }
    }
}
