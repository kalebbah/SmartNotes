package com.example.smartnotes

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartnotes.data.Task
import com.example.smartnotes.data.TaskGroup
import com.example.smartnotes.data.TaskPriority
import com.example.smartnotes.data.TaskStatus
import com.example.smartnotes.ui.TaskViewModel
import com.example.smartnotes.ui.TimeFilter
import com.example.smartnotes.ui.theme.SmartNotesTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartNotesTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmartNotesApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun SmartNotesApp(viewModel: TaskViewModel) {
    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val timeFilter by viewModel.timeFilter.collectAsState()
    val taskCounts by viewModel.taskCounts.collectAsState()
    
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Smart Notes",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Filter and task count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterButton(
                    currentFilter = timeFilter,
                    onFilterSelected = viewModel::setTimeFilter
                )

                Text(
                    text = "${taskCounts.first}/${taskCounts.second}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Task list
            TaskList(
                tasks = filteredTasks,
                onStatusChange = { task, status -> viewModel.updateTaskStatus(task, status) },
                onPriorityChange = { task, priority -> viewModel.updateTaskPriority(task, priority) },
                onDelete = viewModel::deleteTask,
                viewModel = viewModel
            )
        }

        // Add task button
        FloatingActionButton(
            onClick = { showAddTaskDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            interactionSource = null,
            content = {
                Icon(Icons.Default.Add, "Add Task")
            }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAddTask = { title, description, dueDate, priority, groupId, newGroupName ->
                if (groupId != null) {
                    // Add task to existing group
                    viewModel.addTask(title, description, dueDate, priority, groupId)
                } else if (newGroupName != null) {
                    // Create new group and add task
                    viewModel.addGroupAndTask(
                        groupName = newGroupName,
                        taskTitle = title,
                        taskDescription = description,
                        taskDueDate = dueDate,
                        taskPriority = priority
                    )
                }
                showAddTaskDialog = false
            },
            groups = groups
        )
    }
}

@Composable
fun FilterButton(
    currentFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = when (currentFilter) {
                        TimeFilter.ONE_WEEK -> "1 Week"
                        TimeFilter.TWO_WEEKS -> "2 Weeks"
                        TimeFilter.ONE_MONTH -> "1 Month"
                        TimeFilter.ALL_TIME -> "All Time"
                        TimeFilter.COMPLETED -> "Completed"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.primary
            )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            TimeFilter.entries.forEach { filter ->
            DropdownMenuItem(
                text = {
                    Text(
                            text = when (filter) {
                                TimeFilter.ONE_WEEK -> "1 Week"
                                TimeFilter.TWO_WEEKS -> "2 Weeks"
                                TimeFilter.ONE_MONTH -> "1 Month"
                                TimeFilter.ALL_TIME -> "All Time"
                                TimeFilter.COMPLETED -> "Completed"
                            }
                    )
                },
                onClick = {
                        onFilterSelected(filter)
                    expanded = false
                },
                leadingIcon = {
                        if (currentFilter == filter) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    },
                    interactionSource = remember { MutableInteractionSource() }
                )
            }
        }
    }
}

@Composable
fun TaskList(
    tasks: List<Task>,
    onStatusChange: (Task, TaskStatus) -> Unit,
    onPriorityChange: (Task, TaskPriority) -> Unit,
    onDelete: (Task) -> Unit,
    viewModel: TaskViewModel
) {
    var expandedTaskIds by remember { mutableStateOf(setOf<Int>()) }
    val tasksByGroup = tasks.groupBy { it.groupId }
    val groups by viewModel.groups.collectAsState()

    // Calculate group priorities based on their tasks
    val groupPriorities = tasksByGroup.mapValues { (_, tasks) ->
        tasks.maxOfOrNull { task ->
            when (task.priority) {
                TaskPriority.HIGH -> 2
                TaskPriority.LOW -> 1
                TaskPriority.NONE -> 0
            }
        } ?: 0
    }

    // Sort groups by their priority
    val sortedGroups = groups.sortedByDescending { group ->
        groupPriorities[group.id] ?: 0
    }

    val activeGroupIds = tasks.map { it.groupId }.toSet()
    val visibleGroups = sortedGroups.filter { it.id in activeGroupIds }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        // For each visible group
        visibleGroups.forEach { group ->
            val groupTasks = tasksByGroup[group.id] ?: emptyList()
            if (groupTasks.isNotEmpty()) {
                item {
                    // Group box with color
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(
                                color = getGroupDisplayColor(group).copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Column {
                            // Group header with name and priority
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = getGroupDisplayColor(group),
                                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    )
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                
                                Text(
                                    text = when (groupPriorities[group.id]) {
                                        2 -> "🔴"
                                        1 -> "🟡"
                                        else -> "⚪"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            // Tasks in this group
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                // Sort tasks by priority and due date
                                val sortedTasks = groupTasks.sortedWith(
                                    compareBy<Task> { 
                                        when (it.priority) {
                                            TaskPriority.HIGH -> 0
                                            TaskPriority.LOW -> 1
                                            TaskPriority.NONE -> 2
                                        }
                                    }.thenBy { it.dueDate }
                                )

                                sortedTasks.forEach { task ->
                                    TaskItem(
                                        task = task,
                                        onStatusChange = { status -> onStatusChange(task, status) },
                                        onPriorityChange = { priority -> onPriorityChange(task, priority) },
                                        onDelete = { onDelete(task) },
                                        viewModel = viewModel,
                                        isExpanded = expandedTaskIds.contains(task.id),
                                        onExpandedChange = { expanded -> 
                                            expandedTaskIds = if (expanded) {
                                                expandedTaskIds + task.id
                                            } else {
                                                expandedTaskIds - task.id
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        
        // Add a footer item with fixed height to ensure the LazyColumn has a finite height
        item {
            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: Task,
    onStatusChange: (TaskStatus) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onDelete: () -> Unit,
    viewModel: TaskViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    var isEditingDescription by remember { mutableStateOf(false) }
    var editedDescription by remember { mutableStateOf(task.description) }
    val groups by viewModel.groups.collectAsState()
    val group = groups.find { it.id == task.groupId }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onExpandedChange(!isExpanded) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(group?.let { getGroupDisplayColor(it) } ?: MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Due: ${task.dueDate.format(DateTimeFormatter.ofPattern("MMM dd"))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when (task.priority) {
                            TaskPriority.HIGH -> "🔴"
                            TaskPriority.LOW -> "🟡"
                            TaskPriority.NONE -> "⚪"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when (task.status) {
                            TaskStatus.TODO -> "⏳"
                            TaskStatus.WORKING -> "⚙️"
                            TaskStatus.HOLD -> "⏸️"
                            TaskStatus.FINISHED -> "✅"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Description section with edit capability
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                            text = "Description:",
                            style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Edit button
                        TextButton(
                            onClick = { 
                                isEditingDescription = !isEditingDescription
                                if (isEditingDescription) {
                                    editedDescription = task.description
                                }
                            },
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            Text(
                                text = if (isEditingDescription) "Cancel" else "Edit",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (isEditingDescription) {
                        // Edit mode
                        OutlinedTextField(
                            value = editedDescription,
                            onValueChange = { editedDescription = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            maxLines = 3
                        )
                        
                        // Save button
                        Button(
                            onClick = {
                                // Update the task description
                                val updatedTask = task.copy(description = editedDescription)
                                viewModel.updateTask(updatedTask)
                                isEditingDescription = false
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            Text("Save")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        // Display mode
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                            Text(
                                text = "Status:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                        modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                        TaskStatus.entries.forEach { status ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (task.status == status)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                            MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { onStatusChange(status) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (status) {
                                        TaskStatus.TODO -> "⏳"
                                        TaskStatus.WORKING -> "⚙️"
                                        TaskStatus.HOLD -> "⏸️"
                                        TaskStatus.FINISHED -> "✅"
                                    },
                                    color = if (task.status == status)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Priority:",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Row(
                        modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                        TaskPriority.entries.forEach { priority ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (task.priority == priority)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                            MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { onPriorityChange(priority) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (priority) {
                                                TaskPriority.HIGH -> "🔴 High"
                                        TaskPriority.LOW -> "🟡 Low"
                                                TaskPriority.NONE -> "⚪ None"
                                    },
                                    color = if (task.priority == priority)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable { showDeleteDialog = true }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete") },
                            text = { Text("Are you sure you want to delete this task?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteDialog = false
                                    onDelete()
                                }) {
                                    Text("Yes")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("No")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (title: String, description: String, dueDate: LocalDate, priority: TaskPriority, groupId: Int?, newGroupName: String?) -> Unit,
    groups: List<TaskGroup>
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedPriority by remember { mutableStateOf(TaskPriority.NONE) }
    var selectedGroupId by remember { mutableStateOf<Int?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var isCreatingNewGroup by remember { mutableStateOf(false) }
    var groupFieldFocused by remember { mutableStateOf(false) }

    // Date picker state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate.atStartOfDay()
            .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
    )

    // Filtered group list for dropdown
    val filteredGroups = groups.filter {
        it.name.contains(newGroupName, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add New",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { showDatePicker = true }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Due Date: ${dueDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
        confirmButton = {
            TextButton(
                onClick = {
                            // Update the due date when user confirms
                            datePickerState.selectedDateMillis?.let { millis ->
                                // Using UTC to avoid time zone issues
                                val instant = java.time.Instant.ofEpochMilli(millis)
                                val utcDate = instant.atZone(java.time.ZoneOffset.UTC).toLocalDate()
                                dueDate = utcDate
                            }
                            showDatePicker = false
                        },
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Text("OK")
                }
            }
                    ) {
                        DatePicker(
                            state = datePickerState
                        )
                    }
                }

                Text(
                    "Priority:", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.entries.forEach { priority ->
        Box(
            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                .background(
                                    if (selectedPriority == priority)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                                .clickable { selectedPriority = priority }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (priority) {
                                    TaskPriority.HIGH -> "🔴 High"
                                    TaskPriority.LOW -> "🟡 Low"
                                    TaskPriority.NONE -> "⚪ None"
                                },
                                color = if (selectedPriority == priority)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Group selection
                Text(
                    "Group:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val focusRequester = remember { FocusRequester() }
                // Dropdown above the text field
                if (groupFieldFocused && newGroupName.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        if (filteredGroups.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("No matches")
                            }
                        } else {
                            LazyColumn {
                                items(filteredGroups) { group ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedGroupId = group.id
                                                newGroupName = group.name
                                                isCreatingNewGroup = false
                                                // Remove focus to close dropdown
                                                groupFieldFocused = false
                                            }
                                            .padding(16.dp)
                                    ) {
                                        Text(group.name)
                                    }
                                }
                                item {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                isCreatingNewGroup = true
                                                // Remove focus to close dropdown
                                                groupFieldFocused = false
                                            }
                                            .padding(16.dp)
                                    ) {
                                        Text("Create New Group")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = {
                        newGroupName = it
                    },
                    label = { Text("Group Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            groupFieldFocused = focusState.isFocused
                        },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        if (isCreatingNewGroup && newGroupName.isNotBlank()) {
                            // Create new group and add task
                            onAddTask(title, description, dueDate, selectedPriority, null, newGroupName)
                        } else if (selectedGroupId != null) {
                            // Add task to existing group
                            onAddTask(title, description, dueDate, selectedPriority, selectedGroupId, null)
                        }
                    }
                },
                enabled = title.isNotBlank() && (selectedGroupId != null || (isCreatingNewGroup && newGroupName.isNotBlank())),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    )
}

@Preview(showBackground = true)
@Composable
fun TaskScreenPreview() {
    val context = LocalContext.current
    val viewModel = TaskViewModel(context.applicationContext as Application)

    SmartNotesTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
        SmartNotesApp(viewModel)
    }
}

@Preview(showBackground = true)
@Composable
fun TaskItemPreview() {
    val sampleTask = Task(
        title = "Sample Task",
        description = "This is a sample task description",
                        dueDate = LocalDate.now().plusDays(3),
                        priority = TaskPriority.HIGH,
                        status = TaskStatus.WORKING,
        groupId = 0,
        isCompleted = false
    )

    SmartNotesTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
        TaskItem(
            task = sampleTask,
                    onStatusChange = {},
                    onPriorityChange = {},
                    onDelete = {},
            viewModel = TaskViewModel(LocalContext.current.applicationContext as Application),
            isExpanded = false,
            onExpandedChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FilterButtonPreview() {
    SmartNotesTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
        FilterButton(
            currentFilter = TimeFilter.ONE_WEEK,
            onFilterSelected = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
fun AddTaskDialogPreview() {
    val sampleGroups = listOf(
        TaskGroup(
            name = "Work",
            color = 0xFF4CAF50.toInt()
        ),
        TaskGroup(
            name = "Personal",
            color = 0xFF2196F3.toInt()
        )
    )

    SmartNotesTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
        AddTaskDialog(
            onDismiss = {},
            onAddTask = { _, _, _, _, _, _ -> },
            groups = sampleGroups
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyTaskListPreview() {
    SmartNotesTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
                TaskList(
            tasks = emptyList(),
                    onStatusChange = { _, _ -> },
                    onPriorityChange = { _, _ -> },
            onDelete = { },
            viewModel = TaskViewModel(LocalContext.current.applicationContext as Application)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PopulatedTaskListPreview() {
    val sampleTasks = listOf(
        Task(
            title = "Complete Project",
            description = "Finish the Android app",
            dueDate = LocalDate.now().plusDays(3),
            priority = TaskPriority.HIGH,
            status = TaskStatus.WORKING,
            groupId = 0,
            isCompleted = false
        ),
        Task(
            title = "Buy Groceries",
            description = "Milk, eggs, bread",
            dueDate = LocalDate.now().plusDays(1),
            priority = TaskPriority.LOW,
            status = TaskStatus.TODO,
            groupId = 1,
            isCompleted = false
        )
    )

    SmartNotesTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = false) {
                    TaskList(
                        tasks = sampleTasks,
                        onStatusChange = { _, _ -> },
                        onPriorityChange = { _, _ -> },
            onDelete = { },
            viewModel = TaskViewModel(LocalContext.current.applicationContext as Application)
        )
    }
}

fun getGroupDisplayColor(group: TaskGroup): Color {
    return if (group.color == 0) {
        // Picks a color from the palette or the default
        Color(0xFF1976D2) // Blue as fallback
    } else {
        Color(group.color)
    }
}