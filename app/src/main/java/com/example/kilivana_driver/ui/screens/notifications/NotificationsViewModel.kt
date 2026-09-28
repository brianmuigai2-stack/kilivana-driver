package com.example.kilivana_driver.ui.screens.notifications

import androidx.lifecycle.ViewModel
import com.example.kilivana_driver.data.model.AppNotification
import com.example.kilivana_driver.data.model.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotificationsUiState(
    val items: List<AppNotification> = emptyList()
) {
    val unreadCount: Int
        get() = items.count { !it.isRead }
}

class NotificationsViewModel : ViewModel() {

    // TODO: replace sample data with real notifications (API + Firebase Cloud Messaging)
    private val _uiState = MutableStateFlow(NotificationsUiState(items = sampleNotifications()))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    fun markRead(id: String) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.id == id) it.copy(isRead = true) else it })
        }
    }

    fun markAllRead() {
        _uiState.update { state ->
            state.copy(items = state.items.map { it.copy(isRead = true) })
        }
    }
}

internal fun sampleNotifications(): List<AppNotification> = listOf(
    AppNotification(
        id = "n1",
        type = NotificationType.NEW_JOB,
        title = "New job available",
        message = "AG-4591 · Dry Maize, Machakos to Kisumu. Pays KSh 8,500.",
        timeLabel = "5 min ago",
        isEarlier = false
    ),
    AppNotification(
        id = "n2",
        type = NotificationType.JOB_UPDATE,
        title = "Pickup reminder",
        message = "AG-4589 pickup at Greenfield Farm, Thika is at 10:00 AM.",
        timeLabel = "32 min ago",
        isEarlier = false
    ),
    AppNotification(
        id = "n3",
        type = NotificationType.PAYMENT,
        title = "Payment received",
        message = "KSh 2,500 for order AG-4587 was sent to your M-Pesa.",
        timeLabel = "1 hr ago",
        isEarlier = false
    ),
    AppNotification(
        id = "n4",
        type = NotificationType.PAYMENT,
        title = "Payment received",
        message = "KSh 1,500 for order AG-4561 was sent to your M-Pesa.",
        timeLabel = "Yesterday",
        isEarlier = true,
        isRead = true
    ),
    AppNotification(
        id = "n5",
        type = NotificationType.JOB_UPDATE,
        title = "Delivery completed",
        message = "Order AG-4581 was delivered to City Market. Great work!",
        timeLabel = "Yesterday",
        isEarlier = true,
        isRead = true
    ),
    AppNotification(
        id = "n6",
        type = NotificationType.SYSTEM,
        title = "Your account is ready",
        message = "Your Kilivana driver account was created by your admin. Add your vehicle details in Profile.",
        timeLabel = "3 days ago",
        isEarlier = true,
        isRead = true
    ),
    AppNotification(
        id = "n7",
        type = NotificationType.JOB_UPDATE,
        title = "Job cancelled",
        message = "Order AG-4512 (Cabbages) was cancelled by the customer.",
        timeLabel = "6 Sep",
        isEarlier = true,
        isRead = true
    )
)
