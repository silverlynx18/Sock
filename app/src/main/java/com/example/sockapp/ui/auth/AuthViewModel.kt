package com.example.sockapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sockapp.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

// --- UI State Data Classes ---
data class AuthState(
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null, // Store our custom User object
    val isLoading: Boolean = false,
    val error: String? = null,
    val requiresMfa: Boolean = false, // Example for future MFA expansion
)

data class SignUpState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val usernameAvailable: Boolean = true,
    val usernameCheckLoading: Boolean = false
)

data class LoginState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val functions: FirebaseFunctions = Firebase.functions // Region can be specified if needed

    private val _authState = MutableStateFlow(AuthState(currentUser = getCurrentUserModel()))
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _signUpState = MutableStateFlow(SignUpState())
    val signUpState: StateFlow<SignUpState> = _signUpState.asStateFlow()

    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                fetchUserDocument(firebaseUser.uid)
            } else {
                _authState.value = AuthState(isAuthenticated = false, currentUser = null)
            }
        }
    }

    private fun getCurrentUserModel(): User? {
        val firebaseUser = auth.currentUser ?: return null
        // This is a basic mapping. Ideally, you fetch the full User object from Firestore.
        return User(userId = firebaseUser.uid, email = firebaseUser.email ?: "")
    }


    private fun fetchUserDocument(uid: String) {
        viewModelScope.launch {
            try {
                val documentSnapshot = db.collection("users").document(uid).get().await()
                val user = documentSnapshot.toObject(User::class.java)
                _authState.value = AuthState(isAuthenticated = true, currentUser = user, isLoading = false)
            } catch (e: Exception) {
                _authState.value = AuthState(isAuthenticated = true, currentUser = getCurrentUserModel(), error = "Failed to fetch user details: ${e.message}")
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState(error = "Email and password cannot be empty.")
            return
        }
        _loginState.value = LoginState(isLoading = true)
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                // Auth state listener will handle fetching user document and updating _authState
                _loginState.value = LoginState(success = true)
            } catch (e: Exception) {
                _loginState.value = LoginState(error = "Login failed: ${e.message}")
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            _signUpState.value = SignUpState(error = "Email, password, and username cannot be empty.")
            return
        }
        // Add more validation for email, password strength, username rules here

        _signUpState.value = SignUpState(isLoading = true, usernameCheckLoading = false)
        viewModelScope.launch {
            try {
                // 1. Check username availability (example, can be done on field blur too)
                // For simplicity, doing it directly before auth creation.
                // In a real app, you might want to check this earlier in the UX.
                // val usernameCheckResult = functions.getHttpsCallable("checkUsernameAvailability")
                //    .call(mapOf("username" to username)).await()
                // val data = usernameCheckResult.data as? Map<String, Any>
                // if (data?.get("available") != true) {
                //     _signUpState.value = SignUpState(error = data?.get("message") as? String ?: "Username not available", usernameAvailable = false)
                //     return@launch
                // }

                // 2. Create user in Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // 3. Create user document in Firestore
                    val newUser = User(
                        userId = firebaseUser.uid,
                        username = username.trim(),
                        email = email,
                        createdAt = Timestamp.now()
                    )
                    db.collection("users").document(firebaseUser.uid).set(newUser).await()
                    // Auth state listener will handle updating _authState
                    _signUpState.value = SignUpState(success = true)
                } else {
                    _signUpState.value = SignUpState(error = "Sign up succeeded but user data is null.")
                }
            } catch (e: FirebaseAuthUserCollisionException) {
                _signUpState.value = SignUpState(error = "Sign up failed: Email already in use.")
            } catch (e: Exception) {
                 // Check if it's an HttpsCallableException from username check
                if (e.message?.contains("checkUsernameAvailability") == true) {
                     _signUpState.value = SignUpState(error = "Username check failed: ${e.message}", usernameAvailable = false)
                } else {
                    _signUpState.value = SignUpState(error = "Sign up failed: ${e.message}")
                }
            }
        }
    }

    fun checkUsernameAvailability(username: String) {
        if (username.isBlank() || username.length < 3) {
            _signUpState.value = _signUpState.value.copy(error = "Username must be at least 3 characters.", usernameAvailable = false, usernameCheckLoading = false)
            return
        }
        _signUpState.value = _signUpState.value.copy(usernameCheckLoading = true, error = null)
        viewModelScope.launch {
            try {
                val result = functions.getHttpsCallable("checkUsernameAvailability")
                    .call(mapOf("username" to username))
                    .await()
                val data = result.data as? Map<String, Any>
                if (data?.get("available") == true) {
                    _signUpState.value = _signUpState.value.copy(usernameAvailable = true, usernameCheckLoading = false, error = null)
                } else {
                    _signUpState.value = _signUpState.value.copy(
                        usernameAvailable = false,
                        error = data?.get("message") as? String ?: "Username is taken.",
                        usernameCheckLoading = false
                    )
                }
            } catch (e: Exception) {
                _signUpState.value = _signUpState.value.copy(
                    usernameAvailable = false, // Assume unavailable on error
                    error = "Error checking username: ${e.message}",
                    usernameCheckLoading = false
                )
            }
        }
    }

    fun clearSignUpError() {
        _signUpState.value = _signUpState.value.copy(error = null)
    }

    fun clearLoginError() {
        _loginState.value = _loginState.value.copy(error = null)
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState(isAuthenticated = false, currentUser = null)
        _loginState.value = LoginState() // Reset login state
        _signUpState.value = SignUpState() // Reset sign up state
    }
}
