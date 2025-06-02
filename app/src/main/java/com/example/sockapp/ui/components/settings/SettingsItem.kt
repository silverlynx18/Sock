package com.example.sockapp.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.unit.dp
import com.example.sockapp.ui.theme.SockAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    titleColor: Color = Color.Unspecified, // Default to ListItemDefaults.contentColor for headlineText
    iconColor: Color = ListItemDefaults.colors().leadingIconColor,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val itemModifierWithClick = if (onClick != null) {
        modifier.clickable(onClick = onClick, enabled = onClick != null)
    } else {
        modifier
    }

    ListItem(
        headlineText = { Text(title, color = titleColor) },
        modifier = itemModifierWithClick,
        supportingText = subtitle?.let { { Text(it) } },
        leadingContent = icon?.let {
            { Icon(imageVector = it, contentDescription = title, tint = iconColor) }
        },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(
            // You can customize colors further if needed, e.g., containerColor
        )
    )
    if (showDivider) {
        Divider(modifier = Modifier.padding(start = if (icon != null) 56.dp else 16.dp, end = 16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsItemPreview() {
    SockAppTheme {
        SettingsItem(
            icon = Icons.Filled.Info,
            title = "About App",
            subtitle = "Version 1.0.0",
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsItemWithSwitchPreview() {
    SockAppTheme {
        SettingsItem(
            icon = Icons.Filled.Notifications,
            title = "Enable Notifications",
            subtitle = "Receive updates and alerts",
            trailingContent = { Switch(checked = true, onCheckedChange = null) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsItemClickableNoIconPreview() {
    SockAppTheme {
        SettingsItem(
            icon = null,
            title = "Privacy Policy",
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsItemErrorPreview() {
    SockAppTheme {
        SettingsItem(
            icon = Icons.Filled.Info,
            title = "Delete Account",
            titleColor = MaterialTheme.colorScheme.error,
            iconColor = MaterialTheme.colorScheme.error,
            onClick = {}
        )
    }
}
