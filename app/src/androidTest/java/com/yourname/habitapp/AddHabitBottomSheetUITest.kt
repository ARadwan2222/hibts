package com.yourname.habitapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourname.habitapp.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddHabitBottomSheetUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testOpenAddHabitAndInteract() {
        // Navigate to Habits tab
        onView(withId(R.id.nav_habits)).perform(click())

        // Click Add Habit Plus button
        onView(withId(R.id.btnAddHabitPlus)).perform(click())

        // Verify habit name input field is displayed
        onView(withId(R.id.etHabitName)).check(matches(isDisplayed()))

        // Type habit name
        onView(withId(R.id.etHabitName)).perform(typeText("Daily Workout"), closeSoftKeyboard())

        // Verify Category and Save buttons are displayed
        onView(withId(R.id.btnSelectCategory)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSaveHabit)).check(matches(isDisplayed()))
    }
}
