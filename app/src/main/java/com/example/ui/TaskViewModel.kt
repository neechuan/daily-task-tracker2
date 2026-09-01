package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TaskRepository
import com.example.model.TaskCategory
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.notification.TaskNotificationHelper
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    val allTasks: StateFlow<List<TaskItem>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedFilterTab = MutableStateFlow(TaskFilterTab.TODAY)
    val selectedFilterTab: StateFlow<TaskFilterTab> = _selectedFilterTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow<TaskCategory?>(null)
    val selectedCategory: StateFlow<TaskCategory?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _showAddEditSheet = MutableStateFlow(false)
    val showAddEditSheet: StateFlow<Boolean> = _showAddEditSheet.asStateFlow()

    private val _editingTask = MutableStateFlow<TaskItem?>(null)
    val editingTask: StateFlow<TaskItem?> = _editingTask.asStateFlow()

    private val _showThemeDialog = MutableStateFlow(false)
    val showThemeDialog: StateFlow<Boolean> = _showThemeDialog.asStateFlow()

    private val _showTestReminderSnackbar = MutableStateFlow<String?>(null)
    val showTestReminderSnackbar: StateFlow<String?> = _showTestReminderSnackbar.asStateFlow()

    val filteredTasks: StateFlow<List<TaskItem>> = combine(
        allTasks,
        _selectedDate,
        _selectedFilterTab,
        _selectedCategory,
        _searchQuery
    ) { tasks, date, tab, category, query ->
        tasks.filter { task ->
            // Tab filtering
            val matchesTab = when (tab) {
                TaskFilterTab.TODAY -> task.dueDate == date
                TaskFilterTab.UPCOMING -> task.dueDate.isAfter(LocalDate.now()) && !task.isCompleted
                TaskFilterTab.ALL -> true
                TaskFilterTab.COMPLETED -> task.isCompleted
            }

            // Category filtering
            val matchesCategory = category == null || task.category == category

            // Search query
            val matchesSearch = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)

            matchesTab && matchesCategory && matchesSearch
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dailyStats: StateFlow<DailyTaskStats> = combine(
        allTasks,
        _selectedDate
    ) { tasks, date ->
        val dailyTasks = tasks.filter { it.dueDate == date }
        val total = dailyTasks.size
        val completed = dailyTasks.count { it.isCompleted }
        val pending = total - completed
        val percentage = if (total > 0) (completed.toFloat() / total.toFloat()) else 0f
        val highPriority = dailyTasks.count { !it.isCompleted && it.priority == TaskPriority.HIGH }

        DailyTaskStats(
            totalCount = total,
            completedCount = completed,
            pendingCount = pending,
            completionPercentage = percentage,
            highPriorityPending = highPriority
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyTaskStats()
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        if (_selectedFilterTab.value != TaskFilterTab.TODAY) {
            _selectedFilterTab.value = TaskFilterTab.TODAY
        }
    }

    fun setFilterTab(tab: TaskFilterTab) {
        _selectedFilterTab.value = tab
    }

    fun setCategory(category: TaskCategory?) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun openAddTask(presetDate: LocalDate? = null) {
        _editingTask.value = null
        if (presetDate != null) {
            _selectedDate.value = presetDate
        }
        _showAddEditSheet.value = true
    }

    fun openEditTask(task: TaskItem) {
        _editingTask.value = task
        _showAddEditSheet.value = true
    }

    fun closeAddEditSheet() {
        _showAddEditSheet.value = false
        _editingTask.value = null
    }

    fun setShowThemeDialog(show: Boolean) {
        _showThemeDialog.value = show
    }

    fun clearSnackbar() {
        _showTestReminderSnackbar.value = null
    }

    fun toggleTaskCompletion(task: TaskItem, context: Context) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            repository.setTaskCompleted(task.id, newCompleted)
            if (newCompleted && task.hasReminder) {
                TaskNotificationHelper.cancelTaskReminder(context, task.id)
            } else if (!newCompleted && task.hasReminder) {
                TaskNotificationHelper.scheduleTaskReminder(context, task.copy(isCompleted = false))
            }
        }
    }

    fun saveTask(
        title: String,
        description: String,
        dueDate: LocalDate,
        dueHour: Int,
        dueMinute: Int,
        priority: TaskPriority,
        category: TaskCategory,
        hasReminder: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            val currentEditing = _editingTask.value
            if (currentEditing == null) {
                // New Task
                val newTask = TaskItem(
                    title = title.trim(),
                    description = description.trim(),
                    dueDateEpochDay = dueDate.toEpochDay(),
                    dueTimeHour = dueHour,
                    dueTimeMinute = dueMinute,
                    priority = priority,
                    category = category,
                    isCompleted = false,
                    hasReminder = hasReminder
                )
                val newId = repository.insert(newTask)
                if (hasReminder) {
                    TaskNotificationHelper.scheduleTaskReminder(context, newTask.copy(id = newId))
                }
            } else {
                // Update Existing Task
                val updatedTask = currentEditing.copy(
                    title = title.trim(),
                    description = description.trim(),
                    dueDateEpochDay = dueDate.toEpochDay(),
                    dueTimeHour = dueHour,
                    dueTimeMinute = dueMinute,
                    priority = priority,
                    category = category,
                    hasReminder = hasReminder
                )
                repository.update(updatedTask)
                if (hasReminder && !updatedTask.isCompleted) {
                    TaskNotificationHelper.scheduleTaskReminder(context, updatedTask)
                } else {
                    TaskNotificationHelper.cancelTaskReminder(context, updatedTask.id)
                }
            }
            closeAddEditSheet()
        }
    }

    fun deleteTask(task: TaskItem, context: Context) {
        viewModelScope.launch {
            repository.delete(task)
            TaskNotificationHelper.cancelTaskReminder(context, task.id)
        }
    }

    fun testReminderNotification(context: Context) {
        TaskNotificationHelper.triggerTestNotification(context)
        _showTestReminderSnackbar.value = "Test reminder notification sent!"
    }
}

class TaskViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
