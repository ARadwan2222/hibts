package com.yourname.habitapp

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourname.habitapp.ui.SplashActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplashActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(SplashActivity::class.java)

    @Test
    fun testSplashActivityLaunchesSuccessfully() {
        activityRule.scenario.onActivity { activity ->
            assert(!activity.isFinishing)
        }
    }
}
