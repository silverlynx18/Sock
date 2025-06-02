package com.example.sockapp.data.models.profile

// Using string keys for Firestore compatibility
enum class NotificationType(
    val key: String,
    val displayName: String,
    val description: String, // A bit more detail for the settings screen
    val defaultEnabled: Boolean
) {
    // Account Related
    ACCOUNT_SECURITY_ALERTS("account_security", "Security Alerts", "Important notifications about your account security.", true),

    // Group Related
    GROUP_INVITATIONS("group_invitations", "Group Invitations", "Receive invites to join new groups.", true),
    GROUP_JOIN_REQUESTS("group_join_requests", "Group Join Requests", "If you own/admin a group, get notified of new join requests.", true), // For private groups
    GROUP_ROLE_CHANGES("group_role_changes", "Role Changes in Groups", "When your role is changed in a group.", true),
    GROUP_KICKED("group_kicked", "Removed From Group", "When you are removed from a group.", true),
    GROUP_ANNOUNCEMENTS("group_announcements", "Group Announcements", "Major announcements from groups you are in.", true), // If groups have announcements feature

    // Social Interaction (placeholders, depends on app features)
    // NEW_FOLLOWER("new_follower", "New Followers", "When someone follows your profile.", true), // If following users is a feature
    // DIRECT_MESSAGES("direct_messages", "Direct Messages", "When you receive a new direct message.", true), // If DMs exist
    // POST_MENTION("post_mention", "Mentions", "When someone mentions you in a post.", true), // If posts/mentions exist
    // POST_REACTION("post_reaction", "Reactions to your Posts", "When someone reacts to your content.", true),

    // App Related
    APP_UPDATES_FEATURES("app_updates", "App Updates & Features", "News about new features and app improvements.", true);

    companion object {
        fun fromKey(key: String): NotificationType? {
            return entries.find { it.key == key }
        }
        // Define display order or categorization if needed for settings screen
        val generalPreferences = listOf(APP_UPDATES_FEATURES, ACCOUNT_SECURITY_ALERTS)
        val groupPreferences = listOf(GROUP_INVITATIONS, GROUP_JOIN_REQUESTS, GROUP_ROLE_CHANGES, GROUP_KICKED, GROUP_ANNOUNCEMENTS)
        // val socialPreferences = listOf(NEW_FOLLOWER, DIRECT_MESSAGES, ...)
    }
}
