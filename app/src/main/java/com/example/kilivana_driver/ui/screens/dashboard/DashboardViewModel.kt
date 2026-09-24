package com.example.kilivana_driver.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

enum class ActivityType { PICKED_UP, DELIVERED }

data class ActivityItem(
    val title: String,
    val subtitle: String,
    val type: ActivityType
)

data class DashboardUiState(
    val greeting: String = "Good morning",
    val driverName: String = "",
    val driverId: String = "",
    val deliveriesAssigned: Int = 0,
    val estimatedTransit: String = "",
    val hasUnreadNotifications: Boolean = false,
    val recentActivity: List<ActivityItem> = emptyList()
)

class DashboardViewModel : ViewModel() {

    // TODO: replace sample data with the logged-in driver and real jobs from the API
    private val _uiState = MutableStateFlow(sampleDashboardState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
}

private fun currentGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

internal fun sampleDashboardState() = DashboardUiState(
    greeting = currentGreeting(),
    driverName = "James",
    driverId = "DRI-0042",
    deliveriesAssigned = 2,
    estimatedTransit = "4h 15m",
    hasUnreadNotifications = true,
    recentActivity = listOf(
        ActivityItem(
            title = "Picked up • Order AG-4587",
            subtitle = "09:15 AM • Thika Kiambu",
            type = ActivityType.PICKED_UP
        ),
        ActivityItem(
            title = "Delivered • Order AG-4581",
            subtitle = "Yesterday • Nairobi",
            type = ActivityType.DELIVERED
        )
    )
)
