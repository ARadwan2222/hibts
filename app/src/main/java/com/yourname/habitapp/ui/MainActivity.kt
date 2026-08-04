package com.yourname.habitapp.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.yourname.habitapp.R
import com.yourname.habitapp.ui.todo.AddTodoBottomSheet
import com.yourname.habitapp.ui.habits.AddHabitBottomSheet
import com.yourname.habitapp.ui.goals.AddGoalBottomSheet
import androidx.appcompat.app.AlertDialog
import com.yourname.habitapp.databinding.ActivityMainBinding
import com.yourname.habitapp.ui.habits.HabitsFragment
import com.yourname.habitapp.ui.todo.TodoFragment
import com.yourname.habitapp.ui.goals.YearGoalsFragment
import androidx.fragment.app.Fragment
import com.yourname.habitapp.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            com.yourname.habitapp.utils.NotificationHelper.createNotificationChannels(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsPrefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)
        val userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val isDarkMode = settingsPrefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

        val themeName = settingsPrefs.getString("app_theme", "Male")
        val themeId = when(themeName) {
            "Female" -> R.style.Theme_HabitApp_Female
            "Cats" -> R.style.Theme_HabitApp_Cats
            "Dogs" -> R.style.Theme_HabitApp_Dogs
            "Travel" -> R.style.Theme_HabitApp_Travel
            "Nature" -> R.style.Theme_HabitApp_Nature
            "Ocean" -> R.style.Theme_HabitApp_Ocean
            "Sunset" -> R.style.Theme_HabitApp_Sunset
            "Space" -> R.style.Theme_HabitApp_Space
            "Coffee" -> R.style.Theme_HabitApp_Coffee
            "Tech" -> R.style.Theme_HabitApp_Tech
            "Minimal" -> R.style.Theme_HabitApp_Minimal
            "Pastel" -> R.style.Theme_HabitApp_Pastel
            "Vintage" -> R.style.Theme_HabitApp_Vintage
            "Gold" -> R.style.Theme_HabitApp_Gold
            "Classic" -> R.style.Theme_HabitApp_Classic
            else -> {
                val gender = userPrefs.getString("user_gender", "Male")
                if (gender == "Female") R.style.Theme_HabitApp_Female else R.style.Theme_HabitApp_Male
            }
        }
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        com.yourname.habitapp.utils.NotificationHelper.stopAllSounds(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        com.yourname.habitapp.utils.NotificationHelper.createNotificationChannels(this)
        com.yourname.habitapp.worker.HabitReminderWorker.scheduleDailyReminder(this)
        com.yourname.habitapp.worker.DayTransitionWorker.schedule(this)
        com.yourname.habitapp.worker.GoalReminderWorker.schedule(this)

        if (savedInstanceState == null) {
            replaceFragment(HabitsFragment())
        }

        binding.fabAddMain.setOnClickListener {
            onFabAddClicked()
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

        binding.bottomNavigation.itemIconTintList = android.content.res.ColorStateList.valueOf(0xFF6C5CE7.toInt())
        binding.bottomNavigation.itemTextColor = android.content.res.ColorStateList.valueOf(0xFF9E9E9E.toInt())
    }

    private fun onFabAddClicked() {
        if (isFinishing || isDestroyed || supportFragmentManager.isStateSaved) return
        
        val currentFrag = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        
        if (currentFrag is YearGoalsFragment) {
            AddGoalBottomSheet.newInstance().show(supportFragmentManager, "AddGoal")
        } else {
            val dateMillis = (currentFrag as? TodoFragment)?.getSelectedDateMillis() ?: System.currentTimeMillis()
            showAddTaskHabitDialog(dateMillis)
        }
    }

    private fun showAddTaskHabitDialog(dateMillis: Long) {
        val options = arrayOf(getString(R.string.new_task), getString(R.string.new_habit))
        AlertDialog.Builder(this, R.style.PurpleAlertDialog)
            .setTitle(getString(R.string.add_options_title))
            .setItems(options) { _, which ->
                if (supportFragmentManager.isStateSaved) return@setItems
                if (which == 0) {
                    AddTodoBottomSheet.newInstance(dateMillis).show(supportFragmentManager, "AddTodo")
                } else {
                    AddHabitBottomSheet.newInstance(targetDate = dateMillis).show(supportFragmentManager, "AddHabit")
                }
            }.show()
    }

    private fun replaceFragment(fragment: Fragment) {
        if (isFinishing || isDestroyed || supportFragmentManager.isStateSaved) return
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
