package com.example.smartnotes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insert(group: TaskGroup)

    @Update
     fun update(group: TaskGroup)

    @Delete
     fun delete(group: TaskGroup)

    @Query("SELECT * FROM Groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<TaskGroup>>
}