package com.example.sockapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sockapp.data.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- UI State Data Classes ---

/**
 * Represents the overall authentication state of the application.
 * @param isAuthenticated True if a user is currently authenticated.
 * @param currentUser The [User] object if authenticated, null otherwise.
 * @param isLoading True if an authentication operation is in progress (e.g., fetching user document).
 * @param error A message describing any error that occurred during auth state changes or user fetching.
 * @param passwordResetEmailSent True if a password reset email was successfully sent.
 */
data class AuthState(
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val passwordResetEmailSent: Boolean = false // Added for password reset feedback
)

/**
 * Represents the state of the Sign-Up screen/process.
 * @param isLoading True if the sign-up operation is in progress.
 * @param success True if sign-up was successful.
 * @param error A message describing any error during sign-up.
 * @param usernameAvailable True if the chosen username is available, false otherwise.
 * @param usernameCheckLoading True if username availability check is in progress.
 */
data class SignUpState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
    val usernameAvailable: Boolean = true,
    val usernameCheckLoading: Boolean = false
)

/**
 * Represents the state of the Login screen/process.
 * @param isLoading True if the login operation is in progress.
 * @param success True if login was successful.
 * @param error A message describing any error during login.
 */
data class LoginState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore
    private val functions: FirebaseFunctions = Firebase.functions

    private val _authState = MutableStateFlow(AuthState()) // Initial state, currentUser will be populated by listener
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _signUpState = MutableStateFlow(SignUpState())
    val signUpState: StateFlow<SignUpState> = _signUpState.asStateFlow()

    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    init {
        // Listen to Firebase Auth state changes.
        // This is the primary driver for updating the isAuthenticated and currentUser fields.
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // User is signed in, fetch their custom user document from Firestore.
                if (_authState.value.currentUser?.userId != firebaseUser.uid || _authState.value.currentUser == null) {
                     _authState.update { it.copy(isLoading = true) }
                    fetchUserDocument(firebaseUser.uid)
                }
            } else {
                // User is signed out.
                _authState.value = AuthState(isAuthenticated = false, currentUser = null)
            }
        }
    }

    /**
     * Creates a basic User object from the FirebaseUser.
     * This is used as a temporary placeholder if Firestore document fetch is delayed or fails.
     */
    private fun getCurrentUserModelFromFirebaseAuth(): User? {
        val firebaseUser = auth.currentUser ?: return null
        return User(userId = firebaseUser.uid, email = firebaseUser.email ?: "", username = firebaseUser.displayName ?: "")
    }

    /**
     * Fetches the custom User document from Firestore based on the UID.
     * Updates the AuthState with the fetched user or an error.
     */
    private fun fetchUserDocument(uid: String) {
        viewModelScope.launch {
            try {
                val documentSnapshot = db.collection("users").document(uid).get().await()
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    _authState.update { it.copy(isAuthenticated = true, currentUser = user, isLoading = false, error = null) }
                } else {
                     // This case might happen if Firestore doc creation failed after auth user creation,
                    // or if the doc was somehow deleted.
                    _authState.update { it.copy(isAuthenticated = true, currentUser = getCurrentUserModelFromFirebaseAuth(), error = "User profile data not found.", isLoading = false) }
                }
            } catch (e: Exception) {
                _authState.update {
                    it.copy(
                        isAuthenticated = true, // Still authenticated with Firebase Auth
                        currentUser = getCurrentUserModelFromFirebaseAuth(), // Use basic info
                        error = "Failed to fetch user details. Some features might be limited.",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Attempts to log in a user with the given email and password.
     * Updates LoginState with loading, success, or error status.
     */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState(error = "Email and password cannot be empty.")
            return
        }
        _loginState.value = LoginState(isLoading = true)
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                // AuthStateListener will handle fetching user document and updating _authState.
                _loginState.value = LoginState(success = true, isLoading = false)
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidUserException -> "No account found with this email."
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Incorrect password. Please try again."
                    else -> "Login failed. Please check your connection or try again later."
                }
                functions.logger.error("Login failed for email $email:", e)
                _loginState.value = LoginState(error = errorMessage, isLoading = false)
            }
        }
    }

    /**
     * Attempts to sign up a new user.
     * Validates inputs, checks username availability, creates Firebase Auth user,
     * and then creates a corresponding User document in Firestore.
     * Updates SignUpState accordingly.
     */
    fun signUp(email: String, password: String, username: String) {
        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            _signUpState.value = SignUpState(error = "All fields are required.")
            return
        }
        if (username.length < 3 || username.length > 20) {
             _signUpState.value = SignUpState(error = "Username must be between 3 and 20 characters.")
            return
        }
        // TODO: Add more robust email and password strength validation.

        _signUpState.value = SignUpState(isLoading = true)
        viewModelScope.launch {
            try {
                // 1. Check username availability (can be done earlier in UI too)
                val usernameCheckResult = functions.getHttpsCallable("checkUsernameAvailability")
                   .call(mapOf("username" to username.trim())).await()
                val data = usernameCheckResult.data as? Map<String, Any>
                if (data?.get("available") != true) {
                    _signUpState.value = SignUpState(error = data?.get("message") as? String ?: "Username is not available.", usernameAvailable = false)
                    return@launch
                }

                // 2. Create user in Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // 3. Create user document in Firestore
                    // Default values for new fields (bio, socialMediaLinks etc.) will be set by User data class constructor
                    val newUser = User(
                        userId = firebaseUser.uid,
                        username = username.trim(),
                        email = email,
                        createdAt = Timestamp.now(),
                        activeStatusId = AppPresetStatus.Online.id // Default status
                    )
                    db.collection("users").document(firebaseUser.uid).set(newUser).await()
                    // AuthStateListener will eventually pick up the new user, but sign-up success is immediate.
                    _signUpState.value = SignUpState(success = true, isLoading = false)
                } else {
                    _signUpState.value = SignUpState(error = "Sign up failed: Could not retrieve user data after creation.", isLoading = false)
                }
            } catch (e: FirebaseAuthUserCollisionException) {
                _signUpState.value = SignUpState(error = "This email is already registered. Please try logging in.", isLoading = false)
            } catch (e: Exception) {
                functions.logger.error("Sign up failed for $email, username $username:", e)
                val message = if (e.message?.contains("checkUsernameAvailability") == true) {
                    "Username check failed. Please try a different username."
                } else {
                    "Sign up failed. Please try again later."
                }
                _signUpState.value = SignUpState(error = message, isLoading = false)
            }
        }
    }

    /**
     * Checks username availability via a Cloud Function.
     * Updates SignUpState with the result.
     */
    fun checkUsernameAvailability(username: String) {
        if (username.isBlank() || username.length < 3) {
            _signUpState.update { it.copy(error = "Username must be at least 3 characters.", usernameAvailable = false, usernameCheckLoading = false) }
            return
        }
        _signUpState.update { it.copy(usernameCheckLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = functions.getHttpsCallable("checkUsernameAvailability")
                    .call(mapOf("username" to username))
                    .await()
                val data = result.data as? Map<String, Any>
                if (data?.get("available") == true) {
                    _signUpState.update { it.copy(usernameAvailable = true, usernameCheckLoading = false, error = null) }
                } else {
                    _signUpState.update { it.copy(
                        usernameAvailable = false,
                        error = data?.get("message") as? String ?: "Username is taken or invalid.",
                        usernameCheckLoading = false
                    )}
                }
            } catch (e: Exception) {
                 functions.logger.error("Error checking username $username:", e)
                _signUpState.update { it.copy(
                    usernameAvailable = false,
                    error = "Could not verify username. Please try again.",
                    usernameCheckLoading = false
                )}
            }
        }
    }

    /**
     * Sends a password reset email to the given email address.
     * Updates AuthState with success or error.
     */
    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _authState.update { it.copy(error = "Email address cannot be empty.") }
            return
        }
        _authState.update { it.copy(isLoading = true, error = null, passwordResetEmailSent = false) }
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _authState.update { it.copy(isLoading = false, passwordResetEmailSent = true, error = null) }
            } catch (e: Exception) {
                functions.logger.error("Password reset failed for $email:", e)
                 val message = when(e) {
                    is FirebaseAuthInvalidUserException -> "No account found with this email address."
                    else -> "Failed to send password reset email. Please try again."
                }
                _authState.update { it.copy(isLoading = false, error = message, passwordResetEmailSent = false) }
            }
        }
    }

    /**
     * Clears any error messages from SignUpState.
     */
    fun clearSignUpError() = _signUpState.update { it.copy(error = null) }

    /**
     * Clears any error messages from LoginState.
     */
    fun clearLoginError() = _loginState.update { it.copy(error = null) }

    /**
     * Clears password reset status and any general errors from AuthState.
     */
    fun clearPasswordResetStatus() = _authState.update { it.copy(passwordResetEmailSent = false, error = null, isLoading = false) }

    /**
     * Clears general errors from AuthState.
     */
    fun clearErrors() = _authState.update { it.copy(error = null, isLoading = false) }


    /**
     * Signs out the current user.
     * AuthState is updated via the AuthStateListener.
     * Resets LoginState and SignUpState.
     */
    fun logout() {
        auth.signOut()
        // AuthStateListener will set AuthState to unauthenticated.
        _loginState.value = LoginState()
        _signUpState.value = SignUpState()
    }
}
