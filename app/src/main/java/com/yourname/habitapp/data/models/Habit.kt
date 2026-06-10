package com.yourname.habitapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

enum class HabitFrequency { DAILY, WEEKLY, MONTHLY, YEARLY }
enum class HabitCategory { 
    HEALTH, FITNESS, PRODUCTIVITY, LEARNING, SPIRITUAL, SOCIAL, FINANCE, HOBBY, HOME, SELF_IMPROVEMENT, OTHER 
}

@Entity(tableName = "habits", indices = [Index(value = ["name"], unique = true)])
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: HabitCategory = HabitCategory.OTHER,
    val icon: String = "⭐",
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val specificDay: Int? = null,           // 1-7 for Weekly, 1-31 for Monthly
    val isCompletedToday: Boolean = false,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val targetDate: Long? = null,
    val lastCompletedTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
