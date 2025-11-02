package com.sock.app.data.api

import com.sock.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ApiConfig {
    // TODO: Replace with your actual backend URL
    const val BASE_URL = "http://localhost:3000/api"
}

@Serializable
data class AuthRequest(val email: String, val password: String)

@Serializable
data class SignUpRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val username: String,
    val phoneNumber: String
)

@Serializable
data class AuthResponse(val token: String, val user: UserDto)

@Serializable
data class UserDto(
    val uid: String,
    val username: String,
    val displayName: String,
    val email: String,
    val phoneNumber: String,
    val profilePictureUrl: String? = null,
    val createdAt: String,
    val globalStatusId: String? = null,
    val groupSpecificStatuses: Map<String, String> = emptyMap(),
    val groups: List<String> = emptyList()
)

@Serializable
data class GroupDto(
    val groupId: String,
    val name: String,
    val groupProfilePictureUrl: String? = null,
    val primaryColor: String,
    val secondaryColor: String,
    val createdAt: String,
    val ownerId: String,
    val inviteLinkCode: String,
    val members: Map<String, GroupMemberDto> = emptyMap()
)

@Serializable
data class GroupMemberDto(
    val role: String,
    val username: String,
    val joinedAt: String
)

@Serializable
data class InvitationDto(
    val invitationId: String,
    val groupId: String,
    val groupName: String,
    val invitedUserID: String,
    val inviterUserID: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class CreateGroupRequest(
    val groupName: String,
    val primaryColor: String,
    val secondaryColor: String,
    val groupProfilePictureUrl: String? = null
)

@Serializable
data class CreateGroupResponse(
    val groupId: String,
    val inviteLinkCode: String
)

class ApiService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    private fun HttpRequestBuilder.addAuthHeader() {
        authToken?.let {
            header(HttpHeaders.Authorization, "Bearer $it")
        }
    }

    // Auth endpoints
    suspend fun signIn(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = client.post("${ApiConfig.BASE_URL}/auth/signin") {
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(email, password))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(request: SignUpRequest): Result<AuthResponse> {
        return try {
            val response = client.post("${ApiConfig.BASE_URL}/auth/signup") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // User endpoints
    suspend fun getUser(userId: String): Result<UserDto> {
        return try {
            val response = client.get("${ApiConfig.BASE_URL}/users/$userId") {
                addAuthHeader()
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<UserDto> {
        return try {
            val response = client.patch("${ApiConfig.BASE_URL}/users/$userId") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(updates)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return try {
            val response = client.get("${ApiConfig.BASE_URL}/users/check-username/$username") {
                addAuthHeader()
            }
            val data = response.body<Map<String, Boolean>>()
            Result.success(data["available"] ?: false)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Group endpoints
    suspend fun getGroup(groupId: String): Result<GroupDto> {
        return try {
            val response = client.get("${ApiConfig.BASE_URL}/groups/$groupId") {
                addAuthHeader()
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserGroups(userId: String): Result<List<GroupDto>> {
        return try {
            val response = client.get("${ApiConfig.BASE_URL}/users/$userId/groups") {
                addAuthHeader()
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGroup(request: CreateGroupRequest): Result<CreateGroupResponse> {
        return try {
            val response = client.post("${ApiConfig.BASE_URL}/groups") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGroup(groupId: String, updates: Map<String, Any>): Result<GroupDto> {
        return try {
            val response = client.patch("${ApiConfig.BASE_URL}/groups/$groupId") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(updates)
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Invitation endpoints
    suspend fun getPendingInvitations(userId: String): Result<List<InvitationDto>> {
        return try {
            val response = client.get("${ApiConfig.BASE_URL}/invitations/pending/$userId") {
                addAuthHeader()
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processInviteLink(inviteLinkCode: String): Result<InvitationDto> {
        return try {
            val response = client.post("${ApiConfig.BASE_URL}/invitations/process-link") {
                addAuthHeader()
                contentType(ContentType.Application.Json)
                setBody(mapOf("inviteLinkCode" to inviteLinkCode))
            }
            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptInvitation(invitationId: String): Result<String> {
        return try {
            val response = client.post("${ApiConfig.BASE_URL}/invitations/$invitationId/accept") {
                addAuthHeader()
            }
            val data = response.body<Map<String, String>>()
            Result.success(data["message"] ?: "Invitation accepted")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineInvitation(invitationId: String): Result<String> {
        return try {
            val response = client.post("${ApiConfig.BASE_URL}/invitations/$invitationId/decline") {
                addAuthHeader()
            }
            val data = response.body<Map<String, String>>()
            Result.success(data["message"] ?: "Invitation declined")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccount(userId: String): Result<String> {
        return try {
            val response = client.delete("${ApiConfig.BASE_URL}/users/$userId") {
                addAuthHeader()
            }
            val data = response.body<Map<String, String>>()
            Result.success(data["message"] ?: "Account deleted")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
