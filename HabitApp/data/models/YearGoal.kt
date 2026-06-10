package com.yourname.habitapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "year_goals")
data class YearGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val icon: String = "🎯",
    val year: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val progress: Int = 0,                  // 0-100
    val isCompleted: Boolean = false,
    val targetDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "goal_steps")
data class GoalStep(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,                        // ارتباط بالهدف الأب
    val title: String,
    val isCompleted: Boolean = false,
    val order: Int = 0
)
