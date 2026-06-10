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

    @Query("SELECT * FROM todos ORDER BY createdAt DESC")
    suspend fun getAllTodosSync(): List<TodoItem>

    @Query("""
        SELECT * FROM todos 
        WHERE targetDate >= :startOfDay AND targetDate <= :endOfDay
        ORDER BY 
            CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END,
            isCompleted ASC
    """)
    fun getTodosByDate(startOfDay: Long, endOfDay: Long): LiveData<List<TodoItem>>

    @Query("SELECT * FROM todos WHERE (reminderStart = 1 OR reminderEnd = 1) AND isCompleted = 0 AND startTime > :now")
    suspend fun getReminders(now: Long): List<TodoItem>

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

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: Int): TodoItem?

    @Query("UPDATE todos SET isMissed = 1 WHERE isCompleted = 0 AND endTime < :now")
    suspend fun markMissedTasks(now: Long)

    @Query("SELECT COUNT(*) FROM todos WHERE isMissed = 1")
    suspend fun getMissedCount(): Int
}
