package com.yourname.habitapp.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.yourname.habitapp.data.models.Priority
import com.yourname.habitapp.data.models.TodoItem

@Dao
interface TodoDao {

    @Query("""
        SELECT * FROM todos 
        ORDER BY 
            CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END,
            isCompleted ASC,
            createdAt DESC
    """)
    fun getAllTodos(): LiveData<List<TodoItem>>

    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getPendingTodos(): LiveData<List<TodoItem>>

    // مهام اليوم (لها startTime اليوم)
    @Query("""
        SELECT * FROM todos 
        WHERE startTime >= :startOfDay AND startTime <= :endOfDay
        ORDER BY startTime ASC
    """)
    fun getTodayTodos(startOfDay: Long, endOfDay: Long): LiveData<List<TodoItem>>

    // مهام خلال الساعة القادمة (للتنبيهات)
    @Query("""
        SELECT * FROM todos 
        WHERE startTime >= :now AND startTime <= :inOneHour AND isCompleted = 0
    """)
    suspend fun getUpcomingTodos(now: Long, inOneHour: Long): List<TodoItem>

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 1 AND startTime IS NOT NULL")
    suspend fun getTimedCompletedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoItem): Long

    @Update
    suspend fun update(todo: TodoItem)

    @Delete
    suspend fun delete(todo: TodoItem)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteById(id: Int)
}
