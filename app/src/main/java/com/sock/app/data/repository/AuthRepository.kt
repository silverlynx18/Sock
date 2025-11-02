package com.sock.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sock.app.data.api.ApiService
import com.sock.app.data.api.SignUpRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")
private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
private val USER_ID_KEY = stringPreferencesKey("user_id")

class AuthRepository(private val context: Context) {
    private val apiService = ApiService()
    private val dataStore = context.dataStore

    val isUserLoggedIn: Flow<Boolean>
        get() = dataStore.data.map { prefs ->
            prefs[AUTH_TOKEN_KEY] != null
        }

    suspend fun getCurrentUserId(): String? {
        return dataStore.data.first()[USER_ID_KEY]
    }

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        username: String,
        phoneNumber: String
    ): Result<String> {
        return try {
            val request = SignUpRequest(email, password, displayName, username, phoneNumber)
            val result = apiService.signUp(request)
            
            result.onSuccess { authResponse ->
                // Store auth token and user ID
                dataStore.edit { prefs ->
                    prefs[AUTH_TOKEN_KEY] = authResponse.token
                    prefs[USER_ID_KEY] = authResponse.user.uid
                }
                apiService.setAuthToken(authResponse.token)
            }
            
            result.map { it.user.uid }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(
        email: String,
        password: String
    ): Result<String> {
        return try {
            val result = apiService.signIn(email, password)
            
            result.onSuccess { authResponse ->
                // Store auth token and user ID
                dataStore.edit { prefs ->
                    prefs[AUTH_TOKEN_KEY] = authResponse.token
                    prefs[USER_ID_KEY] = authResponse.user.uid
                }
                apiService.setAuthToken(authResponse.token)
            }
            
            result.map { it.user.uid }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        dataStore.edit { prefs ->
            prefs.remove(AUTH_TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
        }
        apiService.setAuthToken(null)
    }

    suspend fun initializeAuth() {
        val token = dataStore.data.first()[AUTH_TOKEN_KEY]
        if (token != null) {
            apiService.setAuthToken(token)
        }
    }
}
