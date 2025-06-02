package com.example.sockapp.data.models.invitation

enum class InvitationType {
    DIRECT_USER_ID, // Sent to a specific known userId
    EMAIL,          // Sent to an email address, user claims it upon login/signup
    USERNAME,       // Sent to a username (needs resolution to userId by backend)
    PHONE_CONTACT;  // Sent via SMS to a phone number, user claims it.
    // GENERAL_LINK, // A generic link that multiple people could click.
                    // This type would require more complex handling.
                    // For now, keeping it to targeted or resolvable types.
}
