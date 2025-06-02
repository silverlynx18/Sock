package com.example.sockapp.data.models.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language // Generic website icon
// It's better to have actual brand icons, but for this generation, we'll use placeholders
// or assume they are available in a hypothetical AppIcons.Custom object.
// For simplicity, I'll use some from Material Icons if they fit, otherwise a generic one.
import androidx.compose.material.icons.filled.AlternateEmail // Placeholder for Twitter/X
import androidx.compose.material.icons.filled.BusinessCenter // Placeholder for LinkedIn
import androidx.compose.material.icons.filled.Code // Placeholder for GitHub
import androidx.compose.material.icons.filled.PhotoCamera // Placeholder for Instagram


import androidx.compose.ui.graphics.vector.ImageVector

// Using string keys for Firestore compatibility
enum class SocialPlatform(
    val key: String,
    val displayName: String,
    val icon: ImageVector, // Placeholder, ideally use brand icons
    val hint: String, // e.g., "username" or "full URL"
    val prefixUrl: String? = null // Base URL for platforms that use handles
) {
    TWITTER("twitter", "X / Twitter", Icons.Filled.AlternateEmail, "username (e.g., your_handle)", "https://x.com/"),
    LINKEDIN("linkedin", "LinkedIn", Icons.Filled.BusinessCenter, "profile name (e.g., yourname-123)", "https://linkedin.com/in/"),
    GITHUB("github", "GitHub", Icons.Filled.Code, "username", "https://github.com/"),
    INSTAGRAM("instagram", "Instagram", Icons.Filled.PhotoCamera, "username", "https://instagram.com/"),
    WEBSITE("website", "Website", Icons.Filled.Language, "full URL (e.g., https://your.site)", null);
    // Add others like Discord, Facebook, Personal Blog etc.

    companion object {
        fun fromKey(key: String): SocialPlatform? {
            return entries.find { it.key == key }
        }
        val platformOrder = listOf(TWITTER, GITHUB, LINKEDIN, INSTAGRAM, WEBSITE) // Define display order
    }
}

// Example of how you might get a full URL:
fun getFullSocialUrl(platform: SocialPlatform, value: String): String {
    return if (platform.prefixUrl != null && !value.startsWith("http", ignoreCase = true)) {
        "${platform.prefixUrl}${value.trim()}"
    } else {
        value.trim()
    }
}
