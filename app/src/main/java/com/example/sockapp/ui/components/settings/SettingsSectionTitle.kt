package com.example.sockapp.ui.components.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sockapp.ui.theme.SockAppTheme

@Composable
fun SettingsSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(top = 8.dp) // Extra top padding for separation
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsSectionTitlePreview() {
    SockAppTheme {
        SettingsSectionTitle(title = "Account Settings")
    }
}
