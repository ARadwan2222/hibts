package com.yourname.habitapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

enum class Priority { HIGH, MEDIUM, LOW }
enum class TaskType { DAILY, WEEKLY, MONTHLY, YEARLY }

@Entity(tableName = "todos", indices = [Index(value = ["title"], unique = true)])
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val priority: Priority = Priority.MEDIUM,
    val type: TaskType = TaskType.DAILY,
    val isCompleted: Boolean = false,
    val isMissed: Boolean = false,           // New field for tasks not done on time
    val durationMinutes: Int = 0,
    val startTime: Long? = null,
    val targetDate: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val reminderStart: Boolean = false,
    val reminderEnd: Boolean = false,
    val reminderBefore: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
