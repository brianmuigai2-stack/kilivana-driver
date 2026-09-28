package com.example.kilivana_driver.data.model

enum class NotificationType { NEW_JOB, JOB_UPDATE, PAYMENT, SYSTEM }

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timeLabel: String,
    val isEarlier: Boolean,
    val isRead: Boolean = false
)
