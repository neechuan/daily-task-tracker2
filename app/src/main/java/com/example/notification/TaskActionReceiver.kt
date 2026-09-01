package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.TaskApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TaskNotificationHelper.ACTION_MARK_DONE) {
            val taskId = intent.getLongExtra(TaskNotificationHelper.EXTRA_TASK_ID, -1L)
            if (taskId != -1L) {
                // Dismiss notification
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.cancel(taskId.toInt())

                // Mark task complete in Database
                val app = context.applicationContext as? TaskApplication
                app?.let { application ->
                    CoroutineScope(Dispatchers.IO).launch {
                        application.repository.setTaskCompleted(taskId, true)
                    }
                }
            }
        }
    }
}
