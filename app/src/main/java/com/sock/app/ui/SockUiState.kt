package com.sock.app.ui

import com.sock.app.model.AvailabilityStatus
import com.sock.app.model.CustomStatus
import com.sock.app.model.CustomStatusTone
import com.sock.app.model.GroupMemberSummary
import com.sock.app.model.GroupSummary
import com.sock.app.model.InvitationSummary
import java.time.Instant

data class SockUiState(
    val globalStatus: AvailabilityStatus = AvailabilityStatus.OPEN_TO_HANGOUT,
    val groupsManagedByUser: List<GroupSummary> = emptyList(),
    val groupsMemberOf: List<GroupSummary> = emptyList(),
    val invitations: List<InvitationSummary> = emptyList(),
    val selectedGroupMembers: List<GroupMemberSummary> = emptyList(),
    val customStatuses: List<CustomStatus> = emptyList()
) {
    val dashboardGroups: List<GroupSummary>
        get() = (groupsManagedByUser + groupsMemberOf)
}

fun previewSockUiState(): SockUiState {
    val roommates = GroupSummary(
        id = "roommates",
        name = "Roommates",
        memberCount = 4,
        primaryColor = 0xFF6750A4,
        secondaryColor = 0xFF24005A,
        status = AvailabilityStatus.OPEN_TO_HANGOUT
    )

    val bookClub = GroupSummary(
        id = "book_club",
        name = "Book Club",
        memberCount = 8,
        primaryColor = 0xFF386A20,
        secondaryColor = 0xFF1B370C,
        status = AvailabilityStatus.WORKING
    )

    val climbingCrew = GroupSummary(
        id = "climbing",
        name = "Climbing Crew",
        memberCount = 6,
        primaryColor = 0xFF625B71,
        secondaryColor = 0xFF312E38,
        status = AvailabilityStatus.BUSY
    )

    return SockUiState(
        globalStatus = AvailabilityStatus.OPEN_TO_HANGOUT,
        groupsManagedByUser = listOf(roommates, climbingCrew),
        groupsMemberOf = listOf(bookClub),
        invitations = listOf(
            InvitationSummary(
                id = "invite-1",
                groupName = "Game Night",
                inviterName = "Jordan",
                sentAt = Instant.now()
            )
        ),
        selectedGroupMembers = listOf(
            GroupMemberSummary(
                id = "1",
                displayName = "Alex Chen",
                username = "alex",
                status = AvailabilityStatus.OPEN_TO_HANGOUT,
                isAdmin = true
            ),
            GroupMemberSummary(
                id = "2",
                displayName = "Sam Patel",
                username = "sam",
                status = AvailabilityStatus.WORKING
            )
        ),
        customStatuses = listOf(
            CustomStatus(
                text = "Take a breather",
                tone = CustomStatusTone.RELAXED,
                note = "Happy for a quick walk"
            ),
            CustomStatus(
                text = "Heads down sprint",
                tone = CustomStatusTone.FOCUSED
            ),
            CustomStatus(
                text = "Need quiet space",
                tone = CustomStatusTone.BOUNDARY,
                note = "Ping later tonight"
            )
        )
    )
}
