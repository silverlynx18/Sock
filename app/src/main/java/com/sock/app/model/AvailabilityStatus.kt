package com.sock.app.model

enum class AvailabilityStatus(
    val displayName: String,
    val description: String
) {
    OPEN_TO_HANGOUT(
        displayName = "Open to Hangout",
        description = "I'm free and would love to connect."
    ),
    BUSY(
        displayName = "Busy",
        description = "Unavailable for now; feel free to reach out later."
    ),
    GOING_THROUGH_IT(
        displayName = "Going Through It",
        description = "Need space, but close friends can check in thoughtfully."
    ),
    BUSY_ANYONE_CAN_JOIN(
        displayName = "Busy (Anyone can Join)",
        description = "Occupied, but company is welcome."
    ),
    WORKING(
        displayName = "Working",
        description = "Focused on work; ping if it's important."
    ),
    DO_NOT_DISTURB(
        displayName = "Do Not Disturb",
        description = "Please reach out later unless it's urgent."
    ),
    DO_NOT_APPROACH(
        displayName = "Do Not Approach",
        description = "Need serious space right now."
    );

    companion object {
        val default = OPEN_TO_HANGOUT

        fun fromDisplayName(name: String): AvailabilityStatus? = entries.firstOrNull {
            it.displayName.equals(name, ignoreCase = true)
        }
    }
}
