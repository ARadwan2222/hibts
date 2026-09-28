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
class AddTodoBottomSheetUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testOpenAddTodoAndEnterTitle() {
        // Click Tasks tab
        onView(withId(R.id.nav_todo)).perform(click())

        // Click Add Task FAB or Plus button
        onView(withId(R.id.fabAddMain)).perform(click())

        // Check if etTodoTitle is displayed
        onView(withId(R.id.etTodoTitle)).check(matches(isDisplayed()))

        // Type task title
        onView(withId(R.id.etTodoTitle)).perform(typeText("Test Task UI"), closeSoftKeyboard())

        // Check priority chips and buttons exist
        onView(withId(R.id.chipGroupPriority)).check(matches(isDisplayed()))
        onView(withId(R.id.btnDate)).check(matches(isDisplayed()))
        onView(withId(R.id.btnStartTime)).check(matches(isDisplayed()))
        onView(withId(R.id.btnEndTime)).check(matches(isDisplayed()))
        onView(withId(R.id.btnSaveTodo)).check(matches(isDisplayed()))
    }
}
