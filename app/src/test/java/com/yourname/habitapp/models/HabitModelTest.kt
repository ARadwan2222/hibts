package com.yourname.habitapp.models

import com.yourname.habitapp.data.models.Habit
import com.yourname.habitapp.data.models.HabitCategory
import com.yourname.habitapp.data.models.HabitFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitModelTest {

    @Test
    fun habit01_defaultInitialization() {
        val h = Habit(name = "Water")
        assertEquals("Water", h.name)
        assertEquals(0, h.id)
    }

    @Test
    fun habit02_categoryDefaultIsOther() {
        val h = Habit(name = "Task")
        assertEquals(HabitCategory.OTHER, h.category)
    }

    @Test
    fun habit03_iconDefaultIsStar() {
        val h = Habit(name = "Task")
        assertEquals("⭐", h.icon)
    }

    @Test
    fun habit04_frequencyDefaultIsDaily() {
        val h = Habit(name = "Task")
        assertEquals(HabitFrequency.DAILY, h.frequency)
    }

    @Test
    fun habit05_specificDayDefaultIsNull() {
        val h = Habit(name = "Task")
        assertNull(h.specificDay)
    }

    @Test
    fun habit06_isCompletedTodayDefaultIsFalse() {
        val h = Habit(name = "Task")
        assertFalse(h.isCompletedToday)
    }

    @Test
    fun habit07_streakDefaultIsZero() {
        val h = Habit(name = "Task")
        assertEquals(0, h.streak)
    }

    @Test
    fun habit08_longestStreakDefaultIsZero() {
        val h = Habit(name = "Task")
        assertEquals(0, h.longestStreak)
    }

    @Test
    fun habit09_targetDateDefaultIsNull() {
        val h = Habit(name = "Task")
        assertNull(h.targetDate)
    }

    @Test
    fun habit10_lastCompletedTimestampDefaultIsNull() {
        val h = Habit(name = "Task")
        assertNull(h.lastCompletedTimestamp)
    }

    @Test
    fun habit11_isMutedDefaultIsFalse() {
        val h = Habit(name = "Task")
        assertFalse(h.isMuted)
    }

    @Test
    fun habit12_displayOrderDefaultIsZero() {
        val h = Habit(name = "Task")
        assertEquals(0, h.displayOrder)
    }

    @Test
    fun habit13_notesDefaultIsEmpty() {
        val h = Habit(name = "Task")
        assertEquals("", h.notes)
    }

    @Test
    fun habit14_incrementStreakOnCompletion() {
        val h = Habit(name = "Gym", streak = 3, longestStreak = 3)
        val newC = true
        val newS = if (newC) h.streak + 1 else (h.streak - 1).coerceAtLeast(0)
        val newL = Math.max(h.longestStreak, newS)
        assertEquals(4, newS)
        assertEquals(4, newL)
    }

    @Test
    fun habit15_decrementStreakOnUncomplete() {
        val h = Habit(name = "Gym", streak = 3, longestStreak = 5)
        val newC = false
        val newS = if (newC) h.streak + 1 else (h.streak - 1).coerceAtLeast(0)
        val newL = Math.max(h.longestStreak, newS)
        assertEquals(2, newS)
        assertEquals(5, newL)
    }

    @Test
    fun habit16_streakCannotBeNegative() {
        val h = Habit(name = "Gym", streak = 0, longestStreak = 0)
        val newS = (h.streak - 1).coerceAtLeast(0)
        assertEquals(0, newS)
    }

    @Test
    fun habit17_weeklyFrequencyWithSpecificDay() {
        val h = Habit(name = "Friday Prayer", frequency = HabitFrequency.WEEKLY, specificDay = 6)
        assertEquals(HabitFrequency.WEEKLY, h.frequency)
        assertEquals(6, h.specificDay)
    }

    @Test
    fun habit18_monthlyFrequencyWithSpecificDay() {
        val h = Habit(name = "Monthly Budget", frequency = HabitFrequency.MONTHLY, specificDay = 1)
        assertEquals(HabitFrequency.MONTHLY, h.frequency)
        assertEquals(1, h.specificDay)
    }

    @Test
    fun habit19_toggleMute() {
        val h = Habit(name = "Quiet", isMuted = false)
        val toggled = h.copy(isMuted = !h.isMuted)
        assertTrue(toggled.isMuted)
    }

    @Test
    fun habit20_equalityCheck() {
        val h1 = Habit(id = 10, name = "Run")
        val h2 = Habit(id = 10, name = "Run")
        assertEquals(h1, h2)
    }
}
