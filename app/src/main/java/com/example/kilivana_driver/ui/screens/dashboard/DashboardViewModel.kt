package com.example.kilivana_driver.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kilivana_driver.data.model.DeliveryJob
import com.example.kilivana_driver.data.network.DRIVER_ROLE
import com.example.kilivana_driver.data.network.JobsRepository
import com.example.kilivana_driver.data.network.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

enum class ActivityType { PICKED_UP, DELIVERED }

data class ActivityItem(
    val title: String,
    val subtitle: String,
    val type: ActivityType,
    val timestamp: String = ""
)

data class DashboardUiState(
    val greeting: String = "Good morning",
    val driverName: String = "",
    val driverId: String = "",
    val deliveriesAssigned: Int = 0,
    val estimatedTransit: String = "",
    val hasUnreadNotifications: Boolean = false,
    val recentActivity: List<ActivityItem> = emptyList(),
    /** Primary profile image URL, or blank to fall back to initials. */
    val photoUrl: String = ""
)

class DashboardViewModel(
    private val jobsRepository: JobsRepository = JobsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(greeting = currentGreeting()) }
        viewModelScope.launch {
            SessionStore.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        driverName = user?.name.orEmpty().ifBlank { "Driver" },
                        driverId = user?.let { "DRV-%04d".format(Locale.US, it.id) }.orEmpty()
                    )
                }
                // Only drivers have logistics jobs. An inspector's session
                // reaches this ViewModel because it is created at activity
                // launch, but their role must not trigger driver-only calls.
                if (user != null && user.role.equals(DRIVER_ROLE, ignoreCase = true)) {
                    refreshJobs(user.id)
                }
            }
        }
    }

    /** Fetches the driver's jobs from the backend and derives the dashboard metrics. */
    fun refreshJobs(driverId: Long? = SessionStore.userId) {
        val id = driverId ?: return
        viewModelScope.launch {
            val result = jobsRepository.getJobsByDriver(id)
            if (result.isSuccess) {
                val deliveryJobs = result.getOrNull().orEmpty()
                val acceptedJobs = deliveryJobs.filter { it.isAccepted() }
                val completedJobs = deliveryJobs.filter { it.isCompleted() }
                val totalEstimatedMinutes = acceptedJobs.sumOf { it.estimatedMinutes }

                _uiState.update {
                    it.copy(
                        deliveriesAssigned = acceptedJobs.size,
                        estimatedTransit = formatTransitMinutes(totalEstimatedMinutes),
                        recentActivity = buildRecentActivity(completedJobs)
                    )
                }
            }
        }
    }

    /** Builds delivered activity entries from the first five supplied jobs, preferring their destination addresses. */
    private fun buildRecentActivity(jobs: List<DeliveryJob>): List<ActivityItem> {
        return jobs.take(5).map { job ->
            val address = if (job.destinationAddress.isNullOrBlank()) {
                job.pickupAddress ?: ""
            } else {
                job.destinationAddress
            }
            ActivityItem(
                title = "Delivered • Order ${job.id}",
                subtitle = address,
                type = ActivityType.DELIVERED,
                timestamp = "Today"
            )
        }
    }

    /** Formats a positive transit duration as hours and minutes, or returns blank for nonpositive values. */
    private fun formatTransitMinutes(minutes: Int): String {
        if (minutes <= 0) return ""
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    /** Returns whether the backend status represents an assigned or ongoing delivery. */
    private fun DeliveryJob.isAccepted(): Boolean = when (status?.lowercase()) {
        "assigned", "accepted", "picked_up", "in_transit", "in_progress",
        "en_route_to_buyer", "en_route_to_dropoff" -> true
        else -> false
    }

    /** Returns whether the backend status represents a completed delivery. */
    private fun DeliveryJob.isCompleted(): Boolean = when (status?.lowercase()) {
        "completed", "delivered" -> true
        else -> false
    }
}

private fun currentGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

/** Sample data for Compose previews only. */
internal fun sampleDashboardState() = DashboardUiState(
    greeting = "Good morning",
    driverName = "John Kimani",
    driverId = "DRV-0042",
    deliveriesAssigned = 2,
    estimatedTransit = "4h 15m",
    hasUnreadNotifications = true,
    recentActivity = listOf(
        ActivityItem(
            title = "Picked up • Order AG-4587",
            subtitle = "Thika Kiambu",
            type = ActivityType.PICKED_UP,
            timestamp = "09:15 AM"
        ),
        ActivityItem(
            title = "Delivered • Order AG-4581",
            subtitle = "Nairobi CBD",
            type = ActivityType.DELIVERED,
            timestamp = "Yesterday"
        )
    )
)
