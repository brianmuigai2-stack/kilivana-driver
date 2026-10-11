package com.example.kilivana_driver.data.model

/** Which icon/colour the notification row shows. */
enum class NotificationType { NEW_JOB, JOB_UPDATE, PAYMENT, SYSTEM }

/**
 * One inbox item on the Notifications screen.
 *
 * Still sample data (see [NotificationsViewModel]) — real push
 * delivery (FCM) is not implemented yet. [timeLabel] is a
 * display-ready string ("5 min ago"), and [isEarlier] groups older
 * items under an "Earlier" heading.
 */
data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timeLabel: String,
    val isEarlier: Boolean,
    val isRead: Boolean = false
)
