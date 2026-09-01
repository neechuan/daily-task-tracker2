package com.example.ui

enum class TaskFilterTab(val title: String) {
    TODAY("Today"),
    UPCOMING("Upcoming"),
    ALL("All Tasks"),
    COMPLETED("Completed")
}

data class DailyTaskStats(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val completionPercentage: Float = 0f,
    val highPriorityPending: Int = 0
)
