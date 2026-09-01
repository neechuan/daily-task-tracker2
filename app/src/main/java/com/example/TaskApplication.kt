package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.TaskRepository
import com.example.notification.TaskNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

import com.example.data.AuthRepository
import com.example.data.AuthRepositoryImpl

class TaskApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { TaskRepository(database.taskDao()) }
    val authRepository: AuthRepository by lazy { AuthRepositoryImpl(this) }

    override fun onCreate() {
        super.onCreate()
        TaskNotificationHelper.createNotificationChannel(this)
    }
}

