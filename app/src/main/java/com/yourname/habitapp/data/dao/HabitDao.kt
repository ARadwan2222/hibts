package com.yourname.habitapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.yourname.habitapp.data.models.Habit

@Dao
interface HabitDao {

    // ─── قراءة ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): LiveData<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    suspend fun getAllHabitsSync(): List<Habit>       // للاستخدام في Workers

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Int): Habit?

    @Query("SELECT * FROM habits ORDER BY longestStreak DESC LIMIT 1")
    suspend fun getBestHabit(): Habit?

    @Query("SELECT MAX(longestStreak) FROM habits")
    suspend fun getMaxStreak(): Int?

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun getHabitCount(): Int

    // نسبة إتمام اليوم (عدد المكتملة / الكل * 100)
    @Query("""
        SELECT CASE WHEN COUNT(*) = 0 THEN 0 
        ELSE (SUM(CASE WHEN isCompletedToday = 1 THEN 1 ELSE 0 END) * 100 / COUNT(*))
        END FROM habits
    """)
    suspend fun getTodayCompletionRate(): Int

    // ─── كتابة ──────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    // إعادة تعيين الإتمام بناءً على التكرار
    @Query("UPDATE habits SET isCompletedToday = 0 WHERE frequency = 'DAILY'")
    suspend fun resetDailyHabits()

    @Query("UPDATE habits SET isCompletedToday = 0 WHERE frequency = 'WEEKLY' AND specificDay = :dayOfWeek")
    suspend fun resetWeeklyHabits(dayOfWeek: Int)

    @Query("UPDATE habits SET isCompletedToday = 0 WHERE frequency = 'MONTHLY' AND specificDay = :dayOfMonth")
    suspend fun resetMonthlyHabits(dayOfMonth: Int)

    @Query("UPDATE habits SET isCompletedToday = 0 WHERE frequency = 'YEARLY'")
    suspend fun resetYearlyHabits()

    @Query("UPDATE habits SET isCompletedToday = :completed, streak = :streak, longestStreak = :longest, lastCompletedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateHabitStreak(id: Int, completed: Boolean, streak: Int, longest: Int, timestamp: Long?)
}
