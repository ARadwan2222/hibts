package com.yourname.habitapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.yourname.habitapp.data.models.Achievement

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun getAllAchievements(): LiveData<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE achievementId = :achievementId LIMIT 1")
    suspend fun getByAchievementId(achievementId: String): Achievement?

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getUnlockedCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: Achievement): Long

    @Delete
    suspend fun delete(achievement: Achievement)
}
