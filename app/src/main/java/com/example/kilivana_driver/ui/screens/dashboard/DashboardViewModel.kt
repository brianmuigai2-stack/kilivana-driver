package com.example.kilivana_driver.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.network.SessionStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.Locale

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

    /**
     * The name shown in the greeting comes from the signed-in user, so it
     * matches whoever actually logged in. Everything else still falls back to
     * sample data until its endpoint exists (see [sampleDashboardState]).
     */
    val uiState: StateFlow<DashboardUiState> = SessionStore.currentUser
        .map { user ->
            val sample = sampleDashboardState()
            sample.copy(
                greeting = currentGreeting(),
                driverName = user?.name.orEmpty().ifBlank { "Driver" },
                driverId = user?.let { "DRV-%04d".format(Locale.US, it.id) }.orEmpty()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sampleDashboardState()
        )
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
    // Name/id are overridden per signed-in user by the ViewModel; the rest is
    // still placeholder until the jobs endpoints exist.
    driverName = "",
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
