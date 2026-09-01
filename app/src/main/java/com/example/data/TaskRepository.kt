package com.example.data

import com.example.model.TaskItem
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks()

    fun getTasksForDate(epochDay: Long): Flow<List<TaskItem>> {
        return taskDao.getTasksForDate(epochDay)
    }

    suspend fun getTaskById(id: Long): TaskItem? {
        return taskDao.getTaskById(id)
    }

    suspend fun insert(task: TaskItem): Long {
        return taskDao.insertTask(task)
    }

    suspend fun update(task: TaskItem) {
        taskDao.updateTask(task)
    }

    suspend fun delete(task: TaskItem) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteById(id: Long) {
        taskDao.deleteTaskById(id)
    }

    suspend fun setTaskCompleted(id: Long, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        taskDao.updateTaskCompletion(id, isCompleted, completedAt)
    }
}
