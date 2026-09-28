package com.yourname.habitapp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
class MainActivityUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testBottomNavigationTabsDisplayed() {
        onView(withId(R.id.bottomNavigation)).check(matches(isDisplayed()))
    }

    @Test
    fun testSwitchingNavigationTabs() {
        // Click Habits Tab
        onView(withId(R.id.nav_habits)).perform(click())
        
        // Click Goals Tab
        onView(withId(R.id.nav_goals)).perform(click())

        // Click Profile Tab
        onView(withId(R.id.nav_profile)).perform(click())

        // Click Tasks Tab
        onView(withId(R.id.nav_todo)).perform(click())
    }
}
