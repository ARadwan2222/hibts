package com.yourname.habitapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GoalFrequency { DAILY, WEEKLY, MONTHLY, YEARLY }

@Entity(tableName = "year_goals")
data class YearGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val icon: String = "🎯",
    val frequency: GoalFrequency = GoalFrequency.YEARLY,
    val year: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val quarter: Int? = null,                // 1, 2, 3, 4 (3-month frames)
    val progress: Int = 0,
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
