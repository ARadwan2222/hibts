package com.yourname.habitapp.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val achievementId: String,              // معرف فريد مثل "WEEK_STREAK"
    val title: String,
    val description: String,
    val icon: String,
    val xpReward: Int = 50,
    val unlockedAt: Long = System.currentTimeMillis()
)
