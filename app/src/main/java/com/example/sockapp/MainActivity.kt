package com.example.sockapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.sockapp.navigation.AppNavigation
import com.example.sockapp.ui.theme.SockAppTheme
import com.example.sockapp.viewmodels.AuthViewModel

// If using Hilt, annotate with @AndroidEntryPoint
// @AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false) // Optional: For edge-to-edge

        setContent {
            SockAppTheme { // Apply your app's theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    // If using Hilt, AuthViewModel can be injected using hiltViewModel()
                    // val authViewModel: AuthViewModel = hiltViewModel()
                    // For non-Hilt, you might provide it differently or use default factory as below
                    val authViewModel: AuthViewModel = viewModel() // Default factory, or provide your own

                    AppNavigation(navController = navController, authViewModel = authViewModel)
                }
            }
        }
    }
}
