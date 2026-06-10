package com.yourname.habitapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.yourname.habitapp.data.models.GoalStep
import com.yourname.habitapp.data.models.YearGoal

@Dao
interface YearGoalDao {

    @Query("SELECT * FROM year_goals WHERE year = :year ORDER BY createdAt DESC")
    fun getGoalsByYear(year: Int): LiveData<List<YearGoal>>

    @Query("SELECT * FROM year_goals WHERE id = :goalId")
    suspend fun getGoalById(goalId: Int): YearGoal?

    @Query("SELECT * FROM goal_steps WHERE goalId = :goalId ORDER BY `order` ASC")
    fun getStepsForGoal(goalId: Int): LiveData<List<GoalStep>>

    @Query("SELECT * FROM goal_steps WHERE goalId = :goalId ORDER BY `order` ASC")
    suspend fun getStepsForGoalSync(goalId: Int): List<GoalStep>

    @Query("SELECT COUNT(*) FROM year_goals WHERE isCompleted = 1")
    suspend fun getCompletedGoalsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: YearGoal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: GoalStep): Long

    @Update
    suspend fun updateGoal(goal: YearGoal)

    @Update
    suspend fun updateStep(step: GoalStep)

    @Delete
    suspend fun deleteGoal(goal: YearGoal)

    @Delete
    suspend fun deleteStep(step: GoalStep)

    // تحديث تقدم الهدف تلقائياً بعد تغيير خطوة
    suspend fun recalculateProgress(goalId: Int) {
        val steps = getStepsForGoalSync(goalId)
        val goal = getGoalById(goalId) ?: return
        if (steps.isEmpty()) return

        val completed = steps.count { it.isCompleted }
        val newProgress = (completed * 100) / steps.size
        updateGoal(goal.copy(
            progress = newProgress,
            isCompleted = newProgress >= 100
        ))
    }
}

// ─── Achievement DAO ─────────────────────────────────────────────────────────

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

// إضافة import ناقص
import com.yourname.habitapp.data.models.Achievement
