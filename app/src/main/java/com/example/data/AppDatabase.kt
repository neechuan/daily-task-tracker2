package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.TaskCategory
import com.example.model.TaskItem
import com.example.model.TaskPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(entities = [TaskItem::class], version = 1, exportSchema = false)
@TypeConverters(TaskConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_tracker_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialTasks(database.taskDao())
                    }
                }
            }

            private suspend fun populateInitialTasks(dao: TaskDao) {
                val todayEpoch = LocalDate.now().toEpochDay()
                val initialTasks = listOf(
                    TaskItem(
                        title = "Morning stretch & hydration",
                        description = "15 minutes mobility exercise and drink 500ml water",
                        dueDateEpochDay = todayEpoch,
                        dueTimeHour = 8,
                        dueTimeMinute = 0,
                        priority = TaskPriority.HIGH,
                        category = TaskCategory.HEALTH,
                        isCompleted = true,
                        completedAt = System.currentTimeMillis()
                    ),
                    TaskItem(
                        title = "Review project deliverables",
                        description = "Finalize Q3 milestone review and check team backlog",
                        dueDateEpochDay = todayEpoch,
                        dueTimeHour = 10,
                        dueTimeMinute = 30,
                        priority = TaskPriority.HIGH,
                        category = TaskCategory.WORK,
                        isCompleted = false,
                        hasReminder = true
                    ),
                    TaskItem(
                        title = "Grocery run: fresh produce & coffee",
                        description = "Apples, spinach, almond milk, and espresso beans",
                        dueDateEpochDay = todayEpoch,
                        dueTimeHour = 17,
                        dueTimeMinute = 0,
                        priority = TaskPriority.MEDIUM,
                        category = TaskCategory.SHOPPING,
                        isCompleted = false,
                        hasReminder = false
                    ),
                    TaskItem(
                        title = "Read 20 pages of book",
                        description = "Chapter 4 of Atomic Habits",
                        dueDateEpochDay = todayEpoch,
                        dueTimeHour = 20,
                        dueTimeMinute = 30,
                        priority = TaskPriority.LOW,
                        category = TaskCategory.STUDY,
                        isCompleted = false,
                        hasReminder = true
                    )
                )
                dao.insertAll(initialTasks)
            }
        }
    }
}
