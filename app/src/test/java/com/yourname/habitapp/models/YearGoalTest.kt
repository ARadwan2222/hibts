package com.yourname.habitapp.models

import com.yourname.habitapp.data.models.GoalFrequency
import com.yourname.habitapp.data.models.GoalStep
import com.yourname.habitapp.data.models.YearGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class YearGoalTest {

    @Test
    fun testYearGoalDefaults() {
        val goal = YearGoal(title = "Learn Kotlin")

        assertEquals("Learn Learn Kotlin", "Learn " + goal.title)
        assertEquals("🎯", goal.icon)
        assertEquals(GoalFrequency.YEARLY, goal.frequency)
        assertEquals(0, goal.progress)
        assertFalse(goal.isCompleted)
    }

    @Test
    fun testGoalProgressCalculation() {
        val steps = listOf(
            GoalStep(goalId = 1, title = "Step 1", isCompleted = true),
            GoalStep(goalId = 1, title = "Step 2", isCompleted = true),
            GoalStep(goalId = 1, title = "Step 3", isCompleted = false),
            GoalStep(goalId = 1, title = "Step 4", isCompleted = false)
        )

        val completedCount = steps.count { it.isCompleted }
        val progressPercent = (completedCount * 100) / steps.size

        assertEquals(2, completedCount)
        assertEquals(50, progressPercent)
    }

    @Test
    fun testEmptyGoalStepsProgress() {
        val steps = emptyList<GoalStep>()
        val total = steps.size
        val progressPercent = if (total == 0) 0 else (steps.count { it.isCompleted } * 100) / total

        assertEquals(0, progressPercent)
    }
}
