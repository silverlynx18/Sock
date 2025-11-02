package com.sock.app.data.model

/**
 * Represents the 7 predefined MVP status options
 */
enum class StatusType(val id: String, val displayText: String, val iconName: String) {
    OPEN_TO_HANGOUT("open_to_hangout", "Open to Hangout", "handshake"),
    BUSY("busy", "Busy", "event_busy"),
    GOING_THROUGH_IT("going_through_it", "Going Through it", "mood_bad"),
    BUSY_ANYONE_CAN_JOIN("busy_anyone_can_join", "Busy (Anyone can Join)", "groups"),
    WORKING("working", "Working", "work"),
    DO_NOT_DISTURB("do_not_disturb", "Do Not Disturb", "notifications_off"),
    DO_NOT_APPROACH("do_not_approach", "Do Not Approach", "block");

    companion object {
        fun fromId(id: String): StatusType? = values().find { it.id == id }
    }
}
