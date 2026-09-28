package com.yourname.habitapp.models

import com.yourname.habitapp.data.models.Habit
import com.yourname.habitapp.data.models.HabitCategory
import com.yourname.habitapp.data.models.HabitFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitTest {

    @Test
    fun testDefaultHabitValues() {
        val habit = Habit(name = "Read Books")
        
        assertEquals("Read Books", habit.name)
        assertEquals(HabitCategory.OTHER, habit.category)
        assertEquals("⭐", habit.icon)
        assertEquals(HabitFrequency.DAILY, habit.frequency)
        assertFalse(habit.isCompletedToday)
        assertEquals(0, habit.streak)
        assertEquals(0, habit.longestStreak)
        assertFalse(habit.isMuted)
        assertEquals(0, habit.displayOrder)
        assertEquals("", habit.notes)
    }

    @Test
    fun testHabitCopy() {
        val habit = Habit(name = "Exercise", streak = 5, longestStreak = 5)
        val updated = habit.copy(isCompletedToday = true, streak = 6, longestStreak = 6)

        assertEquals("Exercise", updated.name)
        assertTrue(updated.isCompletedToday)
        assertEquals(6, updated.streak)
        assertEquals(6, updated.longestStreak)
    }

    @Test
    fun testStreakCoerceAtLeastZero() {
        val habit = Habit(name = "Meditation", streak = 0)
        val newStreak = (habit.streak - 1).coerceAtLeast(0)
        assertEquals(0, newStreak)
    }

    @Test
    fun testHabitCategories() {
        val healthHabit = Habit(name = "Drink Water", category = HabitCategory.HEALTH)
        assertEquals(HabitCategory.HEALTH, healthHabit.category)

        val fitnessHabit = Habit(name = "Gym", category = HabitCategory.FITNESS)
        assertEquals(HabitCategory.FITNESS, fitnessHabit.category)
    }
}
