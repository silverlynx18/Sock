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
import com.example.sockapp.ui.auth.LoginState
// import com.example.sockapp.ui.theme.SockAppTheme // Assuming you have a theme

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val loginState by authViewModel.loginState.collectAsStateWithLifecycle() // Recommended
    // val loginState by authViewModel.loginState.collectAsState() // Simpler alternative

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(loginState.success) {
        if (loginState.success) {
            onLoginSuccess()
            // authViewModel.clearLoginError() // Or handled by ViewModel on state change
        }
    }

    LaunchedEffect(loginState.error) {
        if (loginState.error != null) {
            // Show snackbar or some error message
            // For now, we'll just use a Text field below
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (loginState.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { authViewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }

        loginState.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onNavigateToSignUp) {
            Text("Don't have an account? Sign Up")
        }
    }
}

// Dummy collectAsStateWithLifecycle for preview if not available
// In a real app, this would come from androidx.lifecycle.runtime.compose
@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(
    context: CoroutineContext = EmptyCoroutineContext
): State<T> {
    return collectAsState(context = context)
}


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    // SockAppTheme { // Assuming you have a theme
        LoginScreen(
            onLoginSuccess = {},
            onNavigateToSignUp = {}
        )
    // }
}
