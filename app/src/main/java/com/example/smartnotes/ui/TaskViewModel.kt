package com.example.smartnotes.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartnotes.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _filteredTasks = MutableStateFlow<List<Task>>(emptyList())
    val filteredTasks: StateFlow<List<Task>> = _filteredTasks

    private val _groups = MutableStateFlow<List<TaskGroup>>(emptyList())
    val groups: StateFlow<List<TaskGroup>> = _groups
    
    private val _timeFilter = MutableStateFlow(TimeFilter.ONE_WEEK)
    val timeFilter: StateFlow<TimeFilter> = _timeFilter

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate
    
    private val _taskCounts = MutableStateFlow(Pair(0, 0))
    val taskCounts: StateFlow<Pair<Int, Int>> = _taskCounts

    // Color list
    private val groupColorList = listOf(
        0xFF1976D2.toInt(), // Blue
        0xFFD32F2F.toInt(), // Red
        0xFF388E3C.toInt(), // Green
        0xFFF57C00.toInt(), // Orange
        0xFF7B1FA2.toInt(), // Purple
        0xFF00796B.toInt(), // Tealish
        0xFFC2185B.toInt(), // Pink
        0xFF455A64.toInt(), // Blue Grey
        0xFF689F38.toInt(), // Lighter Green
        0xFF512DA8.toInt(), // Dark Purple
        0xFF00ACC1.toInt(), // Electric Blue
        0xFFFFB300.toInt()  // Red Gold
    )

    private fun getUniqueGroupColor(): Int {
        val usedColors = _groups.value.map { it.color }
        return groupColorList.firstOrNull { it !in usedColors } ?: groupColorList.random()
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao(), database.taskGroupDao())
        
        // Load data in the background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Initialize sample data if needed
                val initialGroups = repository.allGroups.first()
                Log.d("TaskViewModel", "Initial groups: ${initialGroups.size}")
                
                if (initialGroups.isEmpty()) {
                    Log.d("TaskViewModel", "Inserting sample data")
                    insertSampleData()
                }
                
                // Collect data from repository
                repository.allTasks
                    .catch { e -> Log.e("TaskViewModel", "Error collecting tasks", e) }
                    .collect { taskList ->
                        Log.d("TaskViewModel", "Tasks updated: ${taskList.size}")
                        _tasks.value = taskList
                        updateFilteredTasks()
                    }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error in init", e)
            }
        }
        
        // Collect groups in a separate coroutine
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.allGroups
                    .catch { e -> Log.e("TaskViewModel", "Error collecting groups", e) }
                    .collect { groupList ->
                        Log.d("TaskViewModel", "Groups updated: ${groupList.size}")
                        _groups.value = groupList
                    }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error collecting groups", e)
            }
        }
    }
    
    private suspend fun insertSampleData() {
        try {
            // Create sample groups with different colors
            Log.d("TaskViewModel", "Work group color (ULong): ${groupColorList[0]}, as Int: ${groupColorList[0]}")
            val workGroup = TaskGroup(
                name = "Work",
                color = groupColorList[0]
            )
            Log.d("TaskViewModel", "Personal group color (ULong): ${groupColorList[1]}, as Int: ${groupColorList[1]}")
            val personalGroup = TaskGroup(
                name = "Personal",
                color = groupColorList[1]
            )
            Log.d("TaskViewModel", "Shopping group color (ULong): ${groupColorList[2]}, as Int: ${groupColorList[2]}")
            val shoppingGroup = TaskGroup(
                name = "Shopping",
                color = groupColorList[2]
            )
            
            // Insert groups and get their IDs
            repository.insertGroup(workGroup)
            repository.insertGroup(personalGroup)
            repository.insertGroup(shoppingGroup)
            
            // Wait for groups to be inserted and get their IDs
            val groups = repository.allGroups.first()
            Log.d("TaskViewModel", "Groups after insertion: ${groups.size}")
            
            val workGroupId = groups.find { it.name == "Work" }?.id ?: return
            val personalGroupId = groups.find { it.name == "Personal" }?.id ?: return
            val shoppingGroupId = groups.find { it.name == "Shopping" }?.id ?: return
            
            // Create sample tasks with the correct group IDs
            val task1 = Task(
                title = "Complete Project",
                description = "Finish the Android app",
                dueDate = LocalDate.now().plusDays(3),
                priority = TaskPriority.HIGH,
                status = TaskStatus.WORKING,
                groupId = workGroupId,
                isCompleted = false
            )
            
            val task2 = Task(
                title = "Buy Groceries",
                description = "Milk, eggs, bread",
                dueDate = LocalDate.now().plusDays(1),
                priority = TaskPriority.LOW,
                status = TaskStatus.TODO,
                groupId = shoppingGroupId,
                isCompleted = false
            )
            
            val task3 = Task(
                title = "Call Mom",
                description = "Weekly check-in",
                dueDate = LocalDate.now().plusDays(5),
                priority = TaskPriority.NONE,
                status = TaskStatus.TODO,
                groupId = personalGroupId,
                isCompleted = false
            )
            
            // Insert tasks
            repository.insertTask(task1)
            repository.insertTask(task2)
            repository.insertTask(task3)
            
            Log.d("TaskViewModel", "Sample data inserted successfully")
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Error inserting sample data", e)
        }
    }
    
    fun setTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
        updateFilteredTasks()
    }

    fun updateFilteredTasks() {
        val allTasks = _tasks.value
        val filter = _timeFilter.value
        val date = _selectedDate.value

        val filtered = when (filter) {
            TimeFilter.ONE_WEEK -> {
                val endDate = date.plusDays(7)
                allTasks.filter { task ->
                    !task.isCompleted && (task.dueDate.isEqual(date) || (task.dueDate.isAfter(date) && task.dueDate.isBefore(endDate)))
                }
            }
            TimeFilter.TWO_WEEKS -> {
                val endDate = date.plusDays(14)
                allTasks.filter { task ->
                    !task.isCompleted && (task.dueDate.isEqual(date) || (task.dueDate.isAfter(date) && task.dueDate.isBefore(endDate)))
                }
            }
            TimeFilter.ONE_MONTH -> {
                val endDate = date.plusMonths(1)
                allTasks.filter { task ->
                    !task.isCompleted && (task.dueDate.isEqual(date) || (task.dueDate.isAfter(date) && task.dueDate.isBefore(endDate)))
                }
            }
            TimeFilter.ALL_TIME -> allTasks
            TimeFilter.COMPLETED -> allTasks.filter { it.isCompleted }
        }

        _filteredTasks.value = filtered
        updateTaskCounts()
    }

    private fun updateTaskCounts() {
        val allTasks = _tasks.value
        val filter = _timeFilter.value
        val date = _selectedDate.value

        val (startDate, endDate) = when (filter) {
            TimeFilter.ONE_WEEK -> date to date.plusDays(7)
            TimeFilter.TWO_WEEKS -> date to date.plusDays(14)
            TimeFilter.ONE_MONTH -> date to date.plusMonths(1)
            TimeFilter.ALL_TIME, TimeFilter.COMPLETED -> null to null
        }

        val visibleTasks = if (startDate != null && endDate != null) {
            allTasks.filter { it.dueDate in startDate..endDate }
        } else {
            allTasks
        }

        val filteredCompletedTasks = when (filter) {
            TimeFilter.COMPLETED -> visibleTasks.filter { it.isCompleted }
            else -> visibleTasks
        }

        val completedCount = filteredCompletedTasks.count { it.isCompleted }
        val totalCount = filteredCompletedTasks.size

        _taskCounts.value = Pair(completedCount, totalCount)
    }

    fun addTask(title: String, description: String, dueDate: LocalDate, priority: TaskPriority, groupId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val task = Task(
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    priority = priority,
                    status = TaskStatus.TODO,
                    groupId = groupId,
                    isCompleted = false
                )
                repository.insertTask(task)
                Log.d("TaskViewModel", "Task added: $title")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error adding task", e)
            }
        }
    }
    
    fun updateTaskStatus(task: Task, newStatus: TaskStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedTask = task.copy(
                    status = newStatus,
                    isCompleted = newStatus == TaskStatus.FINISHED
                )
                repository.updateTask(updatedTask)
                Log.d("TaskViewModel", "Task status updated: ${task.title} -> $newStatus")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error updating task status", e)
            }
        }
    }
    
    fun updateTaskPriority(task: Task, newPriority: TaskPriority) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedTask = task.copy(priority = newPriority)
                repository.updateTask(updatedTask)
                Log.d("TaskViewModel", "Task priority updated: ${task.title} -> $newPriority")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error updating task priority", e)
            }
        }
    }
    
    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteTask(task)
                Log.d("TaskViewModel", "Task deleted: ${task.title}")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error deleting task", e)
            }
        }
    }
    
    fun addGroupAndTask(
        groupName: String,
        taskTitle: String,
        taskDescription: String,
        taskDueDate: LocalDate,
        taskPriority: TaskPriority
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get the next available color and convert it properly
                val nextColor = getUniqueGroupColor()
                val newGroup = TaskGroup(
                    name = groupName,
                    color = nextColor
                )
                // Insert the group
                repository.insertGroup(newGroup)
                
                // Wait for the groups to be updated and find the newly created group
                val groups = repository.allGroups.first()
                val insertedGroup = groups.find { it.name == groupName }
                
                if (insertedGroup != null) {
                    val newTask = Task(
                        title = taskTitle,
                        description = taskDescription,
                        dueDate = taskDueDate,
                        priority = taskPriority,
                        groupId = insertedGroup.id,
                        status = TaskStatus.TODO,
                        isCompleted = false
                    )
                    repository.insertTask(newTask)
                    Log.d("TaskViewModel", "Successfully added new group '$groupName' with color $nextColor and task '$taskTitle'")
                } else {
                    Log.e("TaskViewModel", "Failed to find newly created group: $groupName")
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error adding group and task: ${e.message}", e)
            }
        }
    }
    
    fun updateTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateTask(task)
                Log.d("TaskViewModel", "Task updated successfully: ${task.title}")
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error updating task", e)
            }
        }
    }
}

enum class TimeFilter {
    ONE_WEEK,
    TWO_WEEKS,
    ONE_MONTH,
    ALL_TIME,
    COMPLETED
} 
