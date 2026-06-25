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
    val specificDay: Int? = null,
    val isCompletedToday: Boolean = false,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val targetDate: Long? = null,
    val lastCompletedTimestamp: Long? = null,
    val isMuted: Boolean = false,            // New field to mute reminders
    val displayOrder: Int = 0,               // Field for manual sorting
    val createdAt: Long = System.currentTimeMillis()
)
