package com.yourname.habitapp.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.yourname.habitapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yourname.habitapp.ui.todo.AddTodoBottomSheet
import com.yourname.habitapp.ui.habits.AddHabitBottomSheet
import androidx.appcompat.app.AlertDialog
import com.yourname.habitapp.databinding.ActivityMainBinding
import com.yourname.habitapp.ui.habits.HabitsFragment
import com.yourname.habitapp.ui.todo.TodoFragment
import com.yourname.habitapp.ui.goals.YearGoalsFragment
import androidx.fragment.app.Fragment
import com.yourname.habitapp.ui.achievements.AchievementsFragment
import com.yourname.habitapp.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
            com.yourname.habitapp.utils.NotificationHelper.createNotificationChannels(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val settingsPrefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // Apply Dark Mode
        if (settingsPrefs.getBoolean("dark_mode", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        val gender = prefs.getString("user_gender", "Male")
        if (gender == "Female") {
            setTheme(R.style.Theme_HabitApp_Female)
        } else {
            setTheme(R.style.Theme_HabitApp_Male)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // إعداد التنبيهات
        com.yourname.habitapp.utils.NotificationHelper.createNotificationChannels(this)
        com.yourname.habitapp.worker.HabitReminderWorker.scheduleDailyReminder(this)
        com.yourname.habitapp.worker.DayTransitionWorker.schedule(this)
        com.yourname.habitapp.worker.GoalReminderWorker.schedule(this)

        // تعيين الشاشة الافتراضية
        if (savedInstanceState == null) {
            replaceFragment(HabitsFragment())
        }

        binding.fabAddMain.setOnClickListener {
            val options = arrayOf(getString(R.string.new_task), getString(R.string.new_habit))
            AlertDialog.Builder(this, R.style.PurpleAlertDialog)
                .setTitle(getString(R.string.add_options_title))
                .setItems(options) { _, which ->
                    if (which == 0) {
                        AddTodoBottomSheet.newInstance(System.currentTimeMillis()).show(supportFragmentManager, "AddTodo")
                    } else {
                        AddHabitBottomSheet.newInstance(-1, null, false).show(supportFragmentManager, "AddHabit")
                    }
                }.show()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_habits -> replaceFragment(HabitsFragment())
                R.id.nav_todo -> replaceFragment(TodoFragment())
                R.id.nav_goals -> replaceFragment(YearGoalsFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
