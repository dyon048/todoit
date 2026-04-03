package com.example.todoit.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates notification channels and posts notifications for reminders and proximity alerts.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_PROXIMITY = "proximity"
    }

    /** Must be called once on app start (idempotent on repeated calls). */
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Exact-time alerts for task reminders" }

        val proximity = NotificationChannel(
            CHANNEL_PROXIMITY,
            "Location Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Alerts when near a task location" }

        manager.createNotificationChannels(listOf(reminders, proximity))
    }

    fun showReminderNotification(taskId: String, title: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Task Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            @Suppress("MissingPermission")
            NotificationManagerCompat.from(context).notify(taskId.hashCode(), notification)
        }
    }

    fun showProximityNotification(taskId: String, title: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_PROXIMITY)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("You're near a task location")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            @Suppress("MissingPermission")
            NotificationManagerCompat.from(context).notify(taskId.hashCode() + 1, notification)
        }
    }
}

