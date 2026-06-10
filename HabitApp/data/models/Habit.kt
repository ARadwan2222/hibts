package com.yourname.habitapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HabitFrequency { DAILY, WEEKLY, MONTHLY }

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String = "⭐",
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val weekDays: String = "1111111",       // "1010100" = السبت والاثنين والأربعاء
    val monthDay: Int = 1,                   // يوم الشهر للعادات الشهرية
    val isCompletedToday: Boolean = false,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
