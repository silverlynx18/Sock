package com.sock.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sock.app.ui.theme.SockTheme

@Composable
fun SettingsScreen(
    appVersion: String,
    onDeleteAccount: () -> Unit,
    onViewPrivacyPolicy: () -> Unit,
    onViewTerms: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsCard(title = "Account") {
            Text(
                text = "Manage your data and account visibility.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = onDeleteAccount) {
                Text(text = "Delete account")
            }
        }

        SettingsCard(title = "Notifications") {
            Text(
                text = "Notification preferences coming soon.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SettingsCard(title = "About") {
            Text(
                text = "App version $appVersion",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            TextButton(onClick = onViewPrivacyPolicy) {
                Text(text = "Privacy Policy")
            }
            TextButton(onClick = onViewTerms) {
                Text(text = "Terms of Service")
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable Column.() -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SockTheme {
        SettingsScreen(
            appVersion = "0.1.0",
            onDeleteAccount = {},
            onViewPrivacyPolicy = {},
            onViewTerms = {}
        )
    }
}
