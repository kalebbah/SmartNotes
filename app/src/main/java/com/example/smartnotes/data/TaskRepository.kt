package com.example.smartnotes.data

import kotlinx.coroutines.flow.Flow


class TaskRepository(
    private val taskDao: TaskDao,
    private val taskGroupDao: TaskGroupDao
) {
    // Task operations
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    
    suspend fun insertTask(task: Task) {
        taskDao.insert(task)
    }
    
    suspend fun updateTask(task: Task) {
        taskDao.update(task)
    }
    
    suspend fun deleteTask(task: Task) {
        taskDao.delete(task)
    }
    
    // TaskGroup operations
    val allGroups: Flow<List<TaskGroup>> = taskGroupDao.getAllGroups()
    
    suspend fun insertGroup(group: TaskGroup) {
        taskGroupDao.insert(group)
    }
    
    suspend fun updateGroup(group: TaskGroup) {
        taskGroupDao.update(group)
    }
    
    suspend fun deleteGroup(group: TaskGroup) {
        taskGroupDao.delete(group)
    }
} 