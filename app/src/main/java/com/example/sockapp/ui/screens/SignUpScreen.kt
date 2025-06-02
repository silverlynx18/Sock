package com.example.sockapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sockapp.ui.auth.AuthViewModel
// import com.example.sockapp.ui.theme.SockAppTheme // Assuming you have a theme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel = viewModel(),
    onSignUpSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val signUpState by authViewModel.signUpState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var passwordsMatchError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var usernameCheckJob by remember { mutableStateOf<Job?>(null) }


    LaunchedEffect(signUpState.success) {
        if (signUpState.success) {
            onSignUpSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Sign Up", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                signUpState.error?.let { authViewModel.clearSignUpError() } // Clear general error
                usernameCheckJob?.cancel() // Cancel previous job
                if (it.length >= 3) {
                    usernameCheckJob = coroutineScope.launch {
                        delay(500) // Debounce
                        authViewModel.checkUsernameAvailability(it)
                    }
                }
            },
            label = { Text("Username") },
            singleLine = true,
            isError = !signUpState.usernameAvailable && username.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        if (signUpState.usernameCheckLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else if (!signUpState.usernameAvailable && username.isNotEmpty()) {
            Text(signUpState.error ?: "Username is taken.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; signUpState.error?.let { authViewModel.clearSignUpError() } },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; signUpState.error?.let { authViewModel.clearSignUpError() } },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                passwordsMatchError = if (password != it && it.isNotEmpty()) "Passwords do not match." else null
                signUpState.error?.let { authViewModel.clearSignUpError() }
            },
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordsMatchError != null,
            modifier = Modifier.fillMaxWidth()
        )
        passwordsMatchError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (signUpState.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (password != confirmPassword) {
                        passwordsMatchError = "Passwords do not match."
                        return@Button
                    }
                    if (!signUpState.usernameAvailable && username.isNotEmpty()){
                        // To ensure the latest check result is considered,
                        // or trigger a check if user tries to submit quickly.
                        // This is a basic check, more robust handling might be needed.
                        authViewModel.checkUsernameAvailability(username) // re-check if somehow bypassed
                        return@Button
                    }
                     authViewModel.signUp(email, password, username)
                },
                enabled = passwordsMatchError == null && signUpState.usernameAvailable,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Up")
            }
        }

        signUpState.error?.let {
             // Display general errors not related to username availability (which is shown above)
            if (!it.contains("username", ignoreCase = true)) { // Avoid duplicating username error
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }


        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Login")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    // SockAppTheme { // Assuming you have a theme
        SignUpScreen(
            onSignUpSuccess = {},
            onNavigateToLogin = {}
        )
    // }
}
