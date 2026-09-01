package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDateEpochDay: Long = LocalDate.now().toEpochDay(),
    val dueTimeHour: Int = 9,
    val dueTimeMinute: Int = 0,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: TaskCategory = TaskCategory.PERSONAL,
    val isCompleted: Boolean = false,
    val hasReminder: Boolean = false,
    val reminderEpochMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val dueDate: LocalDate
        get() = LocalDate.ofEpochDay(dueDateEpochDay)

    val dueTime: LocalTime
        get() = LocalTime.of(dueTimeHour.coerceIn(0, 23), dueTimeMinute.coerceIn(0, 59))

    val formattedTime: String
        get() = dueTime.format(DateTimeFormatter.ofPattern("h:mm a"))

    val formattedDate: String
        get() = dueDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))

    val isToday: Boolean
        get() = dueDate == LocalDate.now()

    val isPastDue: Boolean
        get() {
            if (isCompleted) return false
            val today = LocalDate.now()
            return if (dueDate.isBefore(today)) {
                true
            } else if (dueDate == today) {
                dueTime.isBefore(LocalTime.now())
            } else {
                false
            }
        }
}
