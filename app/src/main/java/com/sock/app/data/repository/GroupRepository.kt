package com.sock.app.data.repository

import com.sock.app.data.api.ApiService
import com.sock.app.data.api.CreateGroupRequest
import com.sock.app.data.api.GroupDto
import com.sock.app.data.model.Group
import com.sock.app.data.model.GroupMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

class GroupRepository {
    private val apiService = ApiService()

    suspend fun getGroup(groupId: String): Result<Group?> {
        return try {
            val result = apiService.getGroup(groupId)
            result.map { it.toGroup() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeGroup(groupId: String): Flow<Group?> = flow {
        // For now, just fetch once. Can be enhanced with polling or WebSocket later
        val result = getGroup(groupId)
        emit(result.getOrNull())
    }

    suspend fun getUserGroups(userId: String): Result<List<Group>> {
        return try {
            val result = apiService.getUserGroups(userId)
            result.map { groups -> groups.mapNotNull { it.toGroup() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGroup(
        groupName: String,
        primaryColor: String,
        secondaryColor: String,
        groupProfilePictureUrl: String?
    ): Result<Pair<String, String>> {
        return try {
            val request = CreateGroupRequest(groupName, primaryColor, secondaryColor, groupProfilePictureUrl)
            val result = apiService.createGroup(request)
            result.map { it.groupId to it.inviteLinkCode }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGroup(groupId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val result = apiService.updateGroup(groupId, updates)
            result.map { Unit }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun GroupDto.toGroup(): Group? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val createdAtTimestamp = try {
                dateFormat.parse(createdAt)?.time
            } catch (e: Exception) {
                null
            }

            val membersMap = members.mapValues { (_, memberDto) ->
                val joinedAtTimestamp = try {
                    dateFormat.parse(memberDto.joinedAt)?.time
                } catch (e: Exception) {
                    null
                }
                GroupMember(
                    role = memberDto.role,
                    username = memberDto.username,
                    joinedAt = joinedAtTimestamp
                )
            }

            Group(
                groupId = groupId,
                name = name,
                groupProfilePictureUrl = groupProfilePictureUrl,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                createdAt = createdAtTimestamp,
                ownerId = ownerId,
                inviteLinkCode = inviteLinkCode,
                members = membersMap
            )
        } catch (e: Exception) {
            null
        }
    }
}
