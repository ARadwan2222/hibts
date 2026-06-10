package com.yourname.habitapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yourname.habitapp.R
import com.yourname.habitapp.ui.achievements.AchievementsFragment
import com.yourname.habitapp.ui.goals.YearGoalsFragment
import com.yourname.habitapp.ui.habits.HabitsFragment
import com.yourname.habitapp.ui.todo.TodoFragment
import com.yourname.habitapp.utils.NotificationHelper
import com.yourname.habitapp.worker.HabitReminderWorker

class MainActivity : AppCompatActivity() {

    // طلب صلاحية التنبيهات (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) HabitReminderWorker.scheduleDailyReminder(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // إنشاء قنوات التنبيه
        NotificationHelper.createNotificationChannels(this)

        // طلب صلاحية التنبيه
        requestNotificationPermission()

        // إعداد BottomNavigation
        setupBottomNavigation()

        // الشاشة الافتراضية
        if (savedInstanceState == null) {
            showFragment(HabitsFragment())
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_habits       -> { showFragment(HabitsFragment()); true }
                R.id.nav_todo         -> { showFragment(TodoFragment()); true }
                R.id.nav_goals        -> { showFragment(YearGoalsFragment()); true }
                R.id.nav_achievements -> { showFragment(AchievementsFragment()); true }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    HabitReminderWorker.scheduleDailyReminder(this)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            HabitReminderWorker.scheduleDailyReminder(this)
        }
    }
}
