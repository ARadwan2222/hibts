package com.yourname.habitapp.models

import com.yourname.habitapp.data.models.GoalFrequency
import com.yourname.habitapp.data.models.GoalStep
import com.yourname.habitapp.data.models.YearGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YearGoalModelTest {

    @Test
    fun goal01_defaultTitleAndIcon() {
        val g = YearGoal(title = "Save $1000")
        assertEquals("Save $1000", g.title)
        assertEquals("🎯", g.icon)
    }

    @Test
    fun goal02_defaultDescriptionIsEmpty() {
        val g = YearGoal(title = "Goal")
        assertEquals("", g.description)
    }

    @Test
    fun goal03_defaultFrequencyIsYearly() {
        val g = YearGoal(title = "Goal")
        assertEquals(GoalFrequency.YEARLY, g.frequency)
    }

    @Test
    fun goal04_defaultQuarterIsNull() {
        val g = YearGoal(title = "Goal")
        assertNull(g.quarter)
    }

    @Test
    fun goal05_defaultProgressIsZero() {
        val g = YearGoal(title = "Goal")
        assertEquals(0, g.progress)
    }

    @Test
    fun goal06_defaultIsCompletedIsFalse() {
        val g = YearGoal(title = "Goal")
        assertFalse(g.isCompleted)
    }

    @Test
    fun goal07_defaultTargetDateIsNull() {
        val g = YearGoal(title = "Goal")
        assertNull(g.targetDate)
    }

    @Test
    fun goal08_defaultNotesIsEmpty() {
        val g = YearGoal(title = "Goal")
        assertEquals("", g.notes)
    }

    @Test
    fun goal09_goalStepDefaultIsCompletedIsFalse() {
        val step = GoalStep(goalId = 1, title = "Step 1")
        assertFalse(step.isCompleted)
        assertEquals(0, step.order)
    }

    @Test
    fun goal10_calculateProgressWithTwoCompletedStepsOutOfFour() {
        val steps = listOf(
            GoalStep(goalId = 1, title = "Step 1", isCompleted = true),
            GoalStep(goalId = 1, title = "Step 2", isCompleted = true),
            GoalStep(goalId = 1, title = "Step 3", isCompleted = false),
            GoalStep(goalId = 1, title = "Step 4", isCompleted = false)
        )
        val completed = steps.count { it.isCompleted }
        val percent = (completed * 100) / steps.size
        assertEquals(50, percent)
    }

    @Test
    fun goal11_calculateProgressAllStepsCompleted() {
        val steps = listOf(
            GoalStep(goalId = 1, title = "Step 1", isCompleted = true),
            GoalStep(goalId = 1, title = "Step 2", isCompleted = true)
        )
        val percent = (steps.count { it.isCompleted } * 100) / steps.size
        assertEquals(100, percent)
    }

    @Test
    fun goal12_calculateProgressNoStepsCompleted() {
        val steps = listOf(
            GoalStep(goalId = 1, title = "Step 1", isCompleted = false)
        )
        val percent = (steps.count { it.isCompleted } * 100) / steps.size
        assertEquals(0, percent)
    }

    @Test
    fun goal13_quarterAssignment() {
        val g = YearGoal(title = "Q1 Goal", quarter = 1)
        assertEquals(1, g.quarter)
    }

    @Test
    fun goal14_toggleGoalCompletion() {
        val g = YearGoal(title = "Run Marathon", isCompleted = false)
        val updated = g.copy(isCompleted = true, progress = 100)
        assertTrue(updated.isCompleted)
        assertEquals(100, updated.progress)
    }

    @Test
    fun goal15_goalStepOrderSorting() {
        val step1 = GoalStep(goalId = 1, title = "First", order = 1)
        val step2 = GoalStep(goalId = 1, title = "Second", order = 2)
        val list = listOf(step2, step1)
        val sorted = list.sortedBy { it.order }
        assertEquals(1, sorted[0].order)
        assertEquals(2, sorted[1].order)
    }
}
