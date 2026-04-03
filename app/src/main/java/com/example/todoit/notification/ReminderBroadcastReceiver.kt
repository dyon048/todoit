package com.example.todoit.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives AlarmManager broadcasts for task reminders and posts the notification.
 */
@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    companion object {
        const val ACTION_REMINDER   = "com.example.todoit.REMINDER"
        const val EXTRA_TASK_ID     = "task_id"
        const val EXTRA_TASK_TITLE  = "task_title"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return
        val taskId    = intent.getStringExtra(EXTRA_TASK_ID)    ?: return
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task reminder"
        notificationHelper.showReminderNotification(taskId, taskTitle)
    }
}

