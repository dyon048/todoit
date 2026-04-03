package com.example.todoit.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.todoit.domain.model.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels exact-time AlarmManager alarms for task reminder notifications.
 * On API 31+ respects SCHEDULE_EXACT_ALARM permission; falls back to inexact alarm if denied.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleReminder(taskId: String, taskTitle: String, reminderAt: Long) {
        val pendingIntent = buildPendingIntent(taskId, taskTitle, PendingIntent.FLAG_UPDATE_CURRENT)
            ?: return  // should never be null with FLAG_UPDATE_CURRENT, but guard defensively

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, pendingIntent)
        }
    }

    fun cancelReminder(taskId: String) {
        val pendingIntent = buildPendingIntent(
            taskId, "", PendingIntent.FLAG_NO_CREATE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /** Re-registers alarms for all tasks with a future reminderAt. Called after device boot. */
    fun rescheduleAll(tasks: List<Task>) {
        val now = System.currentTimeMillis()
        tasks.forEach { task ->
            val reminderAt = task.reminderAt ?: return@forEach
            if (reminderAt > now) scheduleReminder(task.id, task.title, reminderAt)
        }
    }

    private fun buildPendingIntent(taskId: String, taskTitle: String, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderBroadcastReceiver.EXTRA_TASK_TITLE, taskTitle)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

