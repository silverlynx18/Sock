package com.sock.app.data.repository

import com.sock.app.data.api.ApiService
import com.sock.app.data.api.UserDto
import com.sock.app.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

class UserRepository {
    private val apiService = ApiService()

    suspend fun createUser(user: User): Result<Unit> {
        // User creation is handled during signup, so this might not be needed
        // or can be used for profile updates
        return Result.success(Unit)
    }

    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val result = apiService.getUser(userId)
            result.map { it.toUser() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeUser(userId: String): Flow<User?> = flow {
        // For now, just fetch once. Can be enhanced with polling or WebSocket later
        val result = getUser(userId)
        emit(result.getOrNull())
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val result = apiService.updateUser(userId, updates)
            result.map { Unit }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return apiService.checkUsernameAvailability(username)
    }

    private fun UserDto.toUser(): User {
        val createdAtTimestamp = try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormat.parse(createdAt)?.time
        } catch (e: Exception) {
            null
        }

        return User(
            uid = uid,
            username = username,
            displayName = displayName,
            email = email,
            phoneNumber = phoneNumber,
            profilePictureUrl = profilePictureUrl,
            createdAt = createdAtTimestamp,
            globalStatusId = globalStatusId,
            groupSpecificStatuses = groupSpecificStatuses,
            groups = groups
        )
    }
}
