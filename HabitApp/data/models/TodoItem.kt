package com.yourname.habitapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority { HIGH, MEDIUM, LOW }

@Entity(tableName = "todos")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val reminderStart: Boolean = false,
    val reminderEnd: Boolean = false,
    val reminderBefore: Int = 10,           // دقائق قبل البداية
    val createdAt: Long = System.currentTimeMillis()
)
