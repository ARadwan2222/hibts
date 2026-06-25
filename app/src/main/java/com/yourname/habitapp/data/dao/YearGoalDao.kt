package com.yourname.habitapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.yourname.habitapp.data.models.GoalStep
import com.yourname.habitapp.data.models.YearGoal

@Dao
abstract class YearGoalDao {

    @Query("SELECT * FROM year_goals WHERE year = :year ORDER BY isCompleted ASC, displayOrder ASC, targetDate ASC, createdAt DESC")
    abstract fun getGoalsByYear(year: Int): LiveData<List<YearGoal>>

    @Query("SELECT * FROM year_goals WHERE isCompleted = 0 ORDER BY targetDate ASC")
    abstract suspend fun getAllCurrentGoalsSync(): List<YearGoal>

    @Query("SELECT * FROM year_goals WHERE id = :goalId")
    abstract suspend fun getGoalById(goalId: Int): YearGoal?

    @Query("SELECT * FROM goal_steps WHERE goalId = :goalId ORDER BY `order` ASC")
    abstract fun getStepsForGoal(goalId: Int): LiveData<List<GoalStep>>

    @Query("SELECT * FROM goal_steps WHERE goalId = :goalId ORDER BY `order` ASC")
    abstract suspend fun getStepsForGoalSync(goalId: Int): List<GoalStep>

    @Query("SELECT COUNT(*) FROM year_goals WHERE isCompleted = 1")
    abstract suspend fun getCompletedGoalsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertGoal(goal: YearGoal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertStep(step: GoalStep): Long

    @Update
    abstract suspend fun updateGoal(goal: YearGoal)

    @Update
    abstract suspend fun updateAllGoals(goals: List<YearGoal>)

    @Update
    abstract suspend fun updateStep(step: GoalStep)

    @Delete
    abstract suspend fun deleteGoal(goal: YearGoal)

    @Delete
    abstract suspend fun deleteStep(step: GoalStep)

    @Transaction
    open suspend fun recalculateProgress(goalId: Int) {
        val steps = getStepsForGoalSync(goalId)
        val goal = getGoalById(goalId) ?: return
        
        val newProgress = if (steps.isEmpty()) 0 else (steps.count { it.isCompleted } * 100) / steps.size

        updateGoal(goal.copy(
            progress = newProgress,
            isCompleted = newProgress >= 100
        ))
    }
}
