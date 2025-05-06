package com.example.smartnotes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insert(task: Task)

    @Update
     fun update(task: Task)

    @Delete
     fun delete(task: Task)

    @Query("SELECT * FROM Tasks ORDER BY dueDate ASC")
    fun getAllTasks(): Flow<List<Task>>
}