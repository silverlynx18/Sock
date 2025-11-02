package com.sock.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MoodBad
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.sock.app.model.AvailabilityStatus
import com.sock.app.ui.theme.StatusAmber
import com.sock.app.ui.theme.StatusGreen
import com.sock.app.ui.theme.StatusPurple
import com.sock.app.ui.theme.StatusRed

fun AvailabilityStatus.statusColor(): Color = when (this) {
    AvailabilityStatus.OPEN_TO_HANGOUT -> StatusGreen
    AvailabilityStatus.BUSY -> StatusAmber
    AvailabilityStatus.GOING_THROUGH_IT -> StatusPurple
    AvailabilityStatus.BUSY_ANYONE_CAN_JOIN -> StatusGreen
    AvailabilityStatus.WORKING -> StatusAmber
    AvailabilityStatus.DO_NOT_DISTURB -> StatusRed
    AvailabilityStatus.DO_NOT_APPROACH -> StatusRed
}

fun AvailabilityStatus.statusIcon(): ImageVector = when (this) {
    AvailabilityStatus.OPEN_TO_HANGOUT -> Icons.Outlined.CheckCircle
    AvailabilityStatus.BUSY -> Icons.Outlined.Work
    AvailabilityStatus.GOING_THROUGH_IT -> Icons.Outlined.MoodBad
    AvailabilityStatus.BUSY_ANYONE_CAN_JOIN -> Icons.Outlined.Group
    AvailabilityStatus.WORKING -> Icons.Outlined.Work
    AvailabilityStatus.DO_NOT_DISTURB -> Icons.Outlined.Bedtime
    AvailabilityStatus.DO_NOT_APPROACH -> Icons.Outlined.Block
}
