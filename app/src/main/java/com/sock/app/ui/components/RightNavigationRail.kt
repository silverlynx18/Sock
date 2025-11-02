package com.sock.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sock.app.navigation.SockTopLevelDestination

@Composable
fun RightNavigationRail(
    destinations: List<SockTopLevelDestination>,
    currentDestination: SockTopLevelDestination?,
    expanded: Boolean,
    onDestinationSelected: (SockTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        destinations.forEach { destination ->
            NavigationRailItem(
                selected = destination == currentDestination,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = if (expanded) {
                    { Text(text = destination.label) }
                } else {
                    null
                }
            )
        }
    }
}
