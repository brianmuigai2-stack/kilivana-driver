package com.example.kilivana_driver.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Wraps Android's notification system for Kilivana job alerts.
 *
 * Android notification channels are immutable once created — sound and
 * importance can't be changed in code after the fact, only by the user
 * from system settings. So instead of trying to mutate one channel's
 * sound when the driver toggles "Notification Sound", we keep two
 * channels and switch which one we post to. That's how the in-app
 * toggle can actually change whether a sound plays.
 */
object NotificationHelper {

    private const val CHANNEL_SOUND_ID = "job_alerts_sound"
    private const val CHANNEL_SILENT_ID = "job_alerts_silent"
    private const val CHANNEL_NAME = "Job alerts"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val soundUri = Settings.System.DEFAULT_NOTIFICATION_URI
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SOUND_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "New jobs, pickups and payments"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_SILENT_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "New jobs, pickups and payments (silent)"
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    fun hasPermission(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Posts a job-alert notification, respecting the driver's Push / Sound settings. */
    fun postJobAlert(
        context: Context,
        title: String,
        message: String,
        pushEnabled: Boolean,
        soundEnabled: Boolean
    ) {
        if (!pushEnabled || !hasPermission(context)) return

        val channelId = if (soundEnabled) CHANNEL_SOUND_ID else CHANNEL_SILENT_ID
        val notification = NotificationCompat.Builder(context, channelId)
            // TODO: replace with a proper monochrome notification icon asset
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
