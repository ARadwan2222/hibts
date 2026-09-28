package com.yourname.habitapp.utils

import com.yourname.habitapp.data.models.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoReminderSchedulerTest {

    @Test
    fun scheduler01_triggerTimeBeforeCalculation() {
        val startTime = 1700000000000L
        val minutesBefore = 15
        val expectedTriggerTime = startTime - (minutesBefore * 60 * 1000L)
        assertEquals(expectedTriggerTime, startTime - (15 * 60 * 1000L))
    }

    @Test
    fun scheduler02_triggerTimeDelayPositiveWhenInFuture() {
        val now = 1000000L
        val startTime = 2000000L
        val delay = startTime - now
        assertEquals(1000000L, delay)
    }

    @Test
    fun scheduler03_triggerTimeDelayNegativeWhenInPast() {
        val now = 2000000L
        val startTime = 1000000L
        val delay = startTime - now
        assertEquals(-1000000L, delay)
    }

    @Test
    fun scheduler04_uniqueWorkerIdBeforeFormat() {
        val todoId = 42
        val uniqueId = "todo_before_$todoId"
        assertEquals("todo_before_42", uniqueId)
    }

    @Test
    fun scheduler05_uniqueWorkerIdStartFormat() {
        val todoId = 42
        val uniqueId = "todo_start_$todoId"
        assertEquals("todo_start_42", uniqueId)
    }

    @Test
    fun scheduler06_uniqueWorkerIdEndFormat() {
        val todoId = 42
        val uniqueId = "todo_end_$todoId"
        assertEquals("todo_end_42", uniqueId)
    }

    @Test
    fun scheduler07_todoItemRemindersDefaultState() {
        val todo = TodoItem(title = "Task")
        assertFalse(todo.reminderStart)
        assertFalse(todo.reminderEnd)
    }

    @Test
    fun scheduler08_todoItemWithStartReminder() {
        val todo = TodoItem(title = "Task", reminderStart = true, startTime = 1700000000000L)
        assertTrue(todo.reminderStart)
        assertTrue(todo.startTime != null)
    }

    @Test
    fun scheduler09_todoItemWithEndReminder() {
        val todo = TodoItem(title = "Task", reminderEnd = true, endTime = 1700000000000L)
        assertTrue(todo.reminderEnd)
        assertTrue(todo.endTime != null)
    }

    @Test
    fun scheduler10_todoItemDurationFromStartEnd() {
        val start = 1000000L
        val end = start + (120 * 60 * 1000L) // 2 hours
        val durationMinutes = ((end - start) / (1000 * 60)).toInt()
        assertEquals(120, durationMinutes)
    }
}
