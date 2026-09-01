package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(TaskNotificationHelper.EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(TaskNotificationHelper.EXTRA_TASK_TITLE) ?: "Task Reminder"
        val description = intent.getStringExtra(TaskNotificationHelper.EXTRA_TASK_DESCRIPTION) ?: ""
        val category = intent.getStringExtra(TaskNotificationHelper.EXTRA_TASK_CATEGORY) ?: "General"
        val priority = intent.getStringExtra(TaskNotificationHelper.EXTRA_TASK_PRIORITY) ?: "Medium"

        if (taskId != -1L) {
            TaskNotificationHelper.showTaskNotification(
                context = context,
                taskId = taskId,
                title = title,
                description = description,
                category = category,
                priority = priority
            )
        }
    }
}
