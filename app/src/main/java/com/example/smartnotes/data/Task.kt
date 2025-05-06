package com.example.smartnotes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "Tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "dueDate")
    val dueDate: LocalDate,
    
    @ColumnInfo(name = "priority")
    val priority: TaskPriority,
    
    @ColumnInfo(name = "status")
    val status: TaskStatus,
    
    @ColumnInfo(name = "groupId")
    val groupId: Int,
    
    @ColumnInfo(name = "isCompleted")
    val isCompleted: Boolean = false
)

// Enums for task status and priority
enum class TaskStatus {
    TODO, WORKING, HOLD, FINISHED
}

enum class TaskPriority {
    LOW, HIGH, NONE
}