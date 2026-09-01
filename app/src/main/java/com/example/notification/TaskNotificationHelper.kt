package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.model.TaskItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object TaskNotificationHelper {
    const val CHANNEL_ID = "task_reminders_channel"
    const val CHANNEL_NAME = "Task Reminders"
    const val CHANNEL_DESCRIPTION = "Notifications for scheduled task reminders and deadlines"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
    const val EXTRA_TASK_CATEGORY = "extra_task_category"
    const val EXTRA_TASK_PRIORITY = "extra_task_priority"

    const val ACTION_MARK_DONE = "com.example.notification.ACTION_MARK_DONE"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleTaskReminder(context: Context, task: TaskItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_DESCRIPTION, task.description)
            putExtra(EXTRA_TASK_CATEGORY, task.category.displayName)
            putExtra(EXTRA_TASK_PRIORITY, task.priority.displayName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate epoch millis for target date and time
        val targetDateTime = LocalDateTime.of(
            task.dueDate,
            task.dueTime
        )
        val triggerEpochMillis = targetDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Only schedule if in future
        if (triggerEpochMillis > System.currentTimeMillis()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // Fallback for exact alarm permission
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showTaskNotification(
        context: Context,
        taskId: Long,
        title: String,
        description: String,
        category: String,
        priority: String
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt() + 100000,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (description.isNotBlank()) {
            "[$category] $description"
        } else {
            "[$category • $priority Priority] Due now"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\nDon't forget to complete your task!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Mark Done",
                donePendingIntent
            )
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(taskId.toInt(), notification)
        } catch (e: SecurityException) {
            // Notification permission might not be granted yet
        }
    }

    fun triggerTestNotification(context: Context) {
        val testId = (System.currentTimeMillis() % 10000).toLong()
        showTaskNotification(
            context = context,
            taskId = testId,
            title = "Task Reminder: Review daily goals",
            description = "Stay on track! Check off completed tasks for today.",
            category = "Personal",
            priority = "High"
        )
    }
}
