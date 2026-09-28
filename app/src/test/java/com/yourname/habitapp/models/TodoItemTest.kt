package com.yourname.habitapp.models

import com.yourname.habitapp.data.models.Priority
import com.yourname.habitapp.data.models.TaskType
import com.yourname.habitapp.data.models.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoItemTest {

    @Test
    fun testDefaultTodoItemValues() {
        val todo = TodoItem(title = "Finish Report")

        assertEquals("Finish Report", todo.title)
        assertEquals(Priority.MEDIUM, todo.priority)
        assertEquals(TaskType.DAILY, todo.type)
        assertFalse(todo.isCompleted)
        assertFalse(todo.isMissed)
        assertEquals(0, todo.durationMinutes)
        assertFalse(todo.reminderStart)
        assertFalse(todo.reminderEnd)
        assertFalse(todo.isMuted)
        assertEquals(0, todo.displayOrder)
        assertEquals("", todo.notes)
    }

    @Test
    fun testTodoPrioritySorting() {
        val high = TodoItem(title = "High Task", priority = Priority.HIGH)
        val medium = TodoItem(title = "Medium Task", priority = Priority.MEDIUM)
        val low = TodoItem(title = "Low Task", priority = Priority.LOW)

        val list = listOf(medium, low, high)
        val sorted = list.sortedBy { it.priority.ordinal }

        assertEquals(Priority.HIGH, sorted[0].priority)
        assertEquals(Priority.MEDIUM, sorted[1].priority)
        assertEquals(Priority.LOW, sorted[2].priority)
    }

    @Test
    fun testDurationCalculation() {
        val start = 1000000L
        val end = start + (45 * 60 * 1000L) // 45 minutes later
        val duration = ((end - start) / (1000 * 60)).toInt()

        assertEquals(45, duration)
    }

    @Test
    fun testTodoCompletionToggle() {
        val todo = TodoItem(title = "Call Client", isCompleted = false)
        val toggled = todo.copy(isCompleted = !todo.isCompleted)

        assertTrue(toggled.isCompleted)
    }
}
