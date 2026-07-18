package com.yourname.habitapp.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.yourname.habitapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yourname.habitapp.ui.todo.AddTodoBottomSheet
import com.yourname.habitapp.ui.habits.AddHabitBottomSheet
import com.yourname.habitapp.ui.goals.AddGoalBottomSheet
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

        // Apply Dark Mode correctly
        val isDarkMode = settingsPrefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

        // Apply Custom Theme
        val themeName = settingsPrefs.getString("app_theme", "Male")
        val themeId = when(themeName) {
            "Female" -> R.style.Theme_HabitApp_Female
            "Cats"   -> R.style.Theme_HabitApp_Cats
            "Dogs"   -> R.style.Theme_HabitApp_Dogs
            "Travel" -> R.style.Theme_HabitApp_Travel
            "Nature" -> R.style.Theme_HabitApp_Nature
            "Ocean"  -> R.style.Theme_HabitApp_Ocean
            "Sunset" -> R.style.Theme_HabitApp_Sunset
            "Space"  -> R.style.Theme_HabitApp_Space
            "Coffee" -> R.style.Theme_HabitApp_Coffee
            "Tech"   -> R.style.Theme_HabitApp_Tech
            "Minimal"-> R.style.Theme_HabitApp_Minimal
            "Pastel" -> R.style.Theme_HabitApp_Pastel
            "Vintage"-> R.style.Theme_HabitApp_Vintage
            "Gold"   -> R.style.Theme_HabitApp_Gold
            "Classic"-> R.style.Theme_HabitApp_Classic
            else     -> {
                val gender = prefs.getString("user_gender", "Male")
                if (gender == "Female") R.style.Theme_HabitApp_Female else R.style.Theme_HabitApp_Male
            }
        }
        setTheme(themeId)

        super.onCreate(savedInstanceState)
        
        // Stop any notification sounds when app is opened
        com.yourname.habitapp.utils.NotificationHelper.stopAllSounds(this)

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
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            when (currentFragment) {
                is YearGoalsFragment -> {
                    AddGoalBottomSheet.newInstance(-1, -1).show(supportFragmentManager, "AddGoal")
                }
                is TodoFragment -> {
                    val dateMillis = (currentFragment as TodoFragment).getSelectedDateMillis()
                    val options = arrayOf(getString(R.string.new_task), getString(R.string.new_habit))
                    AlertDialog.Builder(this, R.style.PurpleAlertDialog)
                        .setTitle(getString(R.string.add_options_title))
                        .setItems(options) { _, which ->
                            if (which == 0) {
                                AddTodoBottomSheet.newInstance(dateMillis).show(supportFragmentManager, "AddTodo")
                            } else {
                                AddHabitBottomSheet.newInstance(-1, null, false, dateMillis).show(supportFragmentManager, "AddHabit")
                            }
                        }.show()
                }
                else -> {
                    val dateMillis = System.currentTimeMillis()
                    val options = arrayOf(getString(R.string.new_task), getString(R.string.new_habit))
                    AlertDialog.Builder(this, R.style.PurpleAlertDialog)
                        .setTitle(getString(R.string.add_options_title))
                        .setItems(options) { _, which ->
                            if (which == 0) {
                                AddTodoBottomSheet.newInstance(dateMillis).show(supportFragmentManager, "AddTodo")
                            } else {
                                AddHabitBottomSheet.newInstance(-1, null, false, dateMillis).show(supportFragmentManager, "AddHabit")
                            }
                        }.show()
                }
            }
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

        // Apply Bottom Nav Tint Programmatically to prevent crashes
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
        val colorPrimary = typedValue.data
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))
        val colorList = android.content.res.ColorStateList(states, intArrayOf(colorPrimary, 0xFF9E9E9E.toInt()))
        binding.bottomNavigation.itemIconTintList = colorList
        binding.bottomNavigation.itemTextColor = colorList
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
