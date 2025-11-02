package com.sock.app

import com.sock.app.model.AvailabilityStatus
import com.sock.app.ui.previewSockUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SockUiStateTest {

    @Test
    fun previewState_hasExpectedCounts() {
        val state = previewSockUiState()

        assertEquals(2, state.groupsManagedByUser.size)
        assertEquals(1, state.groupsMemberOf.size)
        assertEquals(3, state.customStatuses.size)
    }

    @Test
    fun dashboardGroups_mergesManagedAndMemberLists() {
        val state = previewSockUiState()

        val ids = state.dashboardGroups.map { it.id }

        assertEquals(3, ids.size)
        assertTrue(ids.containsAll(listOf("roommates", "climbing", "book_club")))
    }

    @Test
    fun globalStatus_defaultsToOpenToHangout() {
        val state = previewSockUiState()

        assertEquals(AvailabilityStatus.OPEN_TO_HANGOUT, state.globalStatus)
    }
}
