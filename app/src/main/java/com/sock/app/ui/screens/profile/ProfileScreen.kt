package com.sock.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sock.app.ui.theme.SockTheme

@Composable
fun ProfileScreen(
    displayName: String,
    username: String,
    phoneNumber: String?,
    onEditDisplayName: () -> Unit,
    onChangePhoto: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            tonalElevation = 3.dp
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        TextButton(onClick = onChangePhoto) {
            Text(text = "Change photo")
        }

        ProfileField(title = "Display name", value = displayName) {
            OutlinedButton(onClick = onEditDisplayName) {
                Text(text = "Edit")
            }
        }

        ProfileField(title = "Username", value = "@$username")

        if (!phoneNumber.isNullOrBlank()) {
            ProfileField(title = "Phone number", value = phoneNumber)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLogOut,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Log out")
        }
    }
}

@Composable
private fun ProfileField(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            action?.invoke()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    SockTheme {
        ProfileScreen(
            displayName = "Alex Chen",
            username = "alex",
            phoneNumber = "+1 (555) 010-1234",
            onEditDisplayName = {},
            onChangePhoto = {},
            onLogOut = {}
        )
    }
}
