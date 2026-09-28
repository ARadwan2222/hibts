package com.yourname.habitapp.models

import com.yourname.habitapp.data.models.Priority
import com.yourname.habitapp.data.models.TaskType
import com.yourname.habitapp.data.models.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoItemModelTest {

    @Test
    fun todo01_defaultInitialization() {
        val t = TodoItem(title = "Buy Milk")
        assertEquals("Buy Milk", t.title)
        assertEquals(0, t.id)
    }

    @Test
    fun todo02_priorityDefaultIsMedium() {
        val t = TodoItem(title = "Task")
        assertEquals(Priority.MEDIUM, t.priority)
    }

    @Test
    fun todo03_typeDefaultIsDaily() {
        val t = TodoItem(title = "Task")
        assertEquals(TaskType.DAILY, t.type)
    }

    @Test
    fun todo04_isCompletedDefaultIsFalse() {
        val t = TodoItem(title = "Task")
        assertFalse(t.isCompleted)
    }

    @Test
    fun todo05_isMissedDefaultIsFalse() {
        val t = TodoItem(title = "Task")
        assertFalse(t.isMissed)
    }

    @Test
    fun todo06_durationMinutesDefaultIsZero() {
        val t = TodoItem(title = "Task")
        assertEquals(0, t.durationMinutes)
    }

    @Test
    fun todo07_startTimeDefaultIsNull() {
        val t = TodoItem(title = "Task")
        assertNull(t.startTime)
    }

    @Test
    fun todo08_endTimeDefaultIsNull() {
        val t = TodoItem(title = "Task")
        assertNull(t.endTime)
    }

    @Test
    fun todo09_reminderStartDefaultIsFalse() {
        val t = TodoItem(title = "Task")
        assertFalse(t.reminderStart)
    }

    @Test
    fun todo10_reminderEndDefaultIsFalse() {
        val t = TodoItem(title = "Task")
        assertFalse(t.reminderEnd)
    }

    @Test
    fun todo11_reminderBeforeDefaultIsZero() {
        val t = TodoItem(title = "Task")
        assertEquals(0, t.reminderBefore)
    }

    @Test
    fun todo12_isMutedDefaultIsFalse() {
        val t = TodoItem(title = "Task")
        assertFalse(t.isMuted)
    }

    @Test
    fun todo13_displayOrderDefaultIsZero() {
        val t = TodoItem(title = "Task")
        assertEquals(0, t.displayOrder)
    }

    @Test
    fun todo14_notesDefaultIsEmpty() {
        val t = TodoItem(title = "Task")
        assertEquals("", t.notes)
    }

    @Test
    fun todo15_toggleCompleted() {
        val t = TodoItem(title = "Study", isCompleted = false)
        val updated = t.copy(isCompleted = !t.isCompleted)
        assertTrue(updated.isCompleted)
    }

    @Test
    fun todo16_toggleMute() {
        val t = TodoItem(title = "Study", isMuted = false)
        val updated = t.copy(isMuted = !t.isMuted)
        assertTrue(updated.isMuted)
    }

    @Test
    fun todo17_markMissed() {
        val t = TodoItem(title = "Missed Task", isMissed = false)
        val updated = t.copy(isMissed = true)
        assertTrue(updated.isMissed)
    }

    @Test
    fun todo18_durationMinutesCalculation() {
        val start = 1700000000000L
        val end = start + (30 * 60 * 1000L) // 30 minutes
        val duration = ((end - start) / (1000 * 60)).toInt()
        assertEquals(30, duration)
    }

    @Test
    fun todo19_priorityOrdinalComparison() {
        val h = Priority.HIGH.ordinal
        val m = Priority.MEDIUM.ordinal
        val l = Priority.LOW.ordinal
        assertEquals(0, h)
        assertEquals(1, m)
        assertEquals(2, l)
    }

    @Test
    fun todo20_equalityCheck() {
        val t1 = TodoItem(id = 5, title = "Task A")
        val t2 = TodoItem(id = 5, title = "Task A")
        assertEquals(t1, t2)
    }
}
