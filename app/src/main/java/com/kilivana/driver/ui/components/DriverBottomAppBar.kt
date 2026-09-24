package com.kilivana.driver.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kilivana_driver.ui.theme.Green700
import com.example.kilivana_driver.ui.theme.Green900

/**
 * Bottom navigation bar for the driver app.
 *
 * Four destinations: Dashboard, Maps, Delivery history, Profile.
 * The tab screens themselves are not built here - only the navigation shell.
 */
enum class DriverBottomNavDestination {
    Dashboard,
    Maps,
    History,
    Profile
}

private data class NavDestination(
    val destination: DriverBottomNavDestination,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun DriverBottomAppBar(
    currentDestination: DriverBottomNavDestination,
    onDestinationSelected: (DriverBottomNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavDestination(DriverBottomNavDestination.Dashboard, "Dashboard", Icons.Outlined.Dashboard),
        NavDestination(DriverBottomNavDestination.Maps, "Maps", Icons.Outlined.Map),
        NavDestination(DriverBottomNavDestination.History, "History", Icons.Outlined.History),
        NavDestination(DriverBottomNavDestination.Profile, "Profile", Icons.Outlined.Person2)
    )

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        containerColor = Color.White,
        contentColor = Green900,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val selected = item.destination == currentDestination
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(item.destination) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (selected) Green700 else Color(0xFF8A8A8A),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}