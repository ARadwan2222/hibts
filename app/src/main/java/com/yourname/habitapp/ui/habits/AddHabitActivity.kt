package com.yourname.habitapp.ui.habits

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.Habit
import com.yourname.habitapp.data.models.HabitCategory
import com.yourname.habitapp.data.models.HabitFrequency
import com.yourname.habitapp.utils.AchievementEngine
import com.yourname.habitapp.utils.HabitTemplates
import kotlinx.coroutines.launch
import java.util.*

class AddHabitActivity : AppCompatActivity() {

    private var selectedCategory: HabitCategory = HabitCategory.OTHER
    private var selectedFrequency: HabitFrequency = HabitFrequency.DAILY
    private var selectedSpecificDay: Int? = null
    private var editingHabit: Habit? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val gender = prefs.getString("user_gender", "Male")
        if (gender == "Female") setTheme(R.style.Theme_HabitApp_Female)
        else setTheme(R.style.Theme_HabitApp_Male)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)

        val etName = findViewById<EditText>(R.id.etHabitName)
        val btnCat = findViewById<Button>(R.id.btnSelectCategory)
        val btnFreq = findViewById<Button>(R.id.btnSelectFrequency)
        val tvIcon = findViewById<TextView>(R.id.tvSelectedIcon)
        val btnSave = findViewById<Button>(R.id.btnSaveHabit)
        val layoutSpec = findViewById<LinearLayout>(R.id.layoutSpecificDay)
        val tvSpecTitle = findViewById<TextView>(R.id.tvSpecificDayTitle)
        val btnSpecDay = findViewById<Button>(R.id.btnSelectSpecificDay)
        val chipGroupSugg = findViewById<ChipGroup>(R.id.chipGroupSuggestions)

        // Load Initial Suggestions
        loadSuggestions(chipGroupSugg, etName, tvIcon, HabitCategory.OTHER)

        val habitId = intent.getIntExtra("HABIT_ID", -1)
        if (habitId != -1) {
            lifecycleScope.launch {
                editingHabit = AppDatabase.getInstance(this@AddHabitActivity).habitDao().getHabitById(habitId)
                editingHabit?.let {
                    etName.setText(it.name)
                    selectedCategory = it.category
                    selectedFrequency = it.frequency
                    selectedSpecificDay = it.specificDay
                    tvIcon.text = it.icon
                    btnCat.text = HabitTemplates.CATEGORY_NAMES[it.category]
                    btnFreq.text = getFrequencyText(it.frequency)
                    updateSpecificDayUI(layoutSpec, tvSpecTitle, btnSpecDay)
                    loadSuggestions(chipGroupSugg, etName, tvIcon, it.category)
                    btnSave.text = "تحديث"
                }
            }
        }

        btnCat.setOnClickListener {
            val categories = HabitCategory.entries.toTypedArray()
            val names = categories.map { HabitTemplates.CATEGORY_NAMES[it] ?: it.name }.toTypedArray()
            AlertDialog.Builder(this, R.style.PurpleAlertDialog)
                .setTitle("اختر الفئة")
                .setItems(names) { _, which ->
                    selectedCategory = categories[which]
                    btnCat.text = names[which]
                    updateIcon(tvIcon, selectedCategory)
                    loadSuggestions(chipGroupSugg, etName, tvIcon, selectedCategory)
                }.show()
        }

        btnFreq.setOnClickListener {
            val freqs = arrayOf("يومي", "أسبوعي", "شهري")
            AlertDialog.Builder(this, R.style.PurpleAlertDialog)
                .setTitle("تكرار العادة")
                .setItems(freqs) { _, which ->
                    selectedFrequency = when(which) {
                        0 -> HabitFrequency.DAILY
                        1 -> HabitFrequency.WEEKLY
                        else -> HabitFrequency.MONTHLY
                    }
                    btnFreq.text = freqs[which]
                    selectedSpecificDay = null
                    updateSpecificDayUI(layoutSpec, tvSpecTitle, btnSpecDay)
                }.show()
        }

        btnSpecDay.setOnClickListener {
            if (selectedFrequency == HabitFrequency.WEEKLY) {
                val days = arrayOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
                AlertDialog.Builder(this, R.style.PurpleAlertDialog)
                    .setTitle("اختر يوم الأسبوع")
                    .setItems(days) { _, which ->
                        selectedSpecificDay = which + 1
                        btnSpecDay.text = days[which]
                    }.show()
            } else if (selectedFrequency == HabitFrequency.MONTHLY) {
                val days = (1..31).map { it.toString() }.toTypedArray()
                AlertDialog.Builder(this, R.style.PurpleAlertDialog)
                    .setTitle("اختر يوم الشهر")
                    .setItems(days) { _, which ->
                        selectedSpecificDay = which + 1
                        btnSpecDay.text = "يوم ${which + 1} في الشهر"
                    }.show()
            }
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) { etName.error = "أدخل اسم العادة"; return@setOnClickListener }

            if (selectedFrequency != HabitFrequency.DAILY && selectedSpecificDay == null) {
                Toast.makeText(this, "يرجى اختيار يوم التكرار", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val habit = editingHabit?.copy(
                name = name,
                category = selectedCategory,
                frequency = selectedFrequency,
                specificDay = selectedSpecificDay,
                icon = tvIcon.text.toString()
            ) ?: Habit(
                name = name,
                category = selectedCategory,
                frequency = selectedFrequency,
                specificDay = selectedSpecificDay,
                icon = tvIcon.text.toString()
            )

            lifecycleScope.launch {
                val dao = AppDatabase.getInstance(this@AddHabitActivity).habitDao()
                
                // Check for duplicate name (case insensitive)
                val allHabits = dao.getAllHabitsSync()
                val isDuplicate = allHabits.any { it.name.trim().equals(name, ignoreCase = true) && it.id != (editingHabit?.id ?: -1) }
                
                if (isDuplicate) {
                    Toast.makeText(this@AddHabitActivity, "هذه العادة موجودة بالفعل!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (editingHabit != null) dao.updateHabit(habit) else dao.insertHabit(habit)
                
                if (editingHabit == null) {
                    val count = dao.getHabitCount()
                    AchievementEngine.checkAndUnlock(this@AddHabitActivity, "HABIT_ADDED", count)
                }
                showConfirmationPopup(habit)
            }
        }
    }

    private fun getFrequencyText(freq: HabitFrequency): String = when(freq) {
        HabitFrequency.DAILY -> "يومي"
        HabitFrequency.WEEKLY -> "أسبوعي"
        HabitFrequency.MONTHLY -> "شهري"
        else -> ""
    }

    private fun updateSpecificDayUI(layout: View, title: TextView, button: Button) {
        when (selectedFrequency) {
            HabitFrequency.DAILY -> {
                layout.visibility = View.GONE
            }
            HabitFrequency.WEEKLY -> {
                layout.visibility = View.VISIBLE
                title.text = "يوم التكرار الأسبوعي"
                button.text = selectedSpecificDay?.let { getDayName(it) } ?: "اختر اليوم"
            }
            HabitFrequency.MONTHLY -> {
                layout.visibility = View.VISIBLE
                title.text = "يوم التكرار الشهري"
                button.text = selectedSpecificDay?.let { "يوم $it في الشهر" } ?: "اختر اليوم"
            }
            else -> layout.visibility = View.GONE
        }
    }

    private fun getDayName(day: Int): String {
        val days = arrayOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        return if (day in 1..7) days[day - 1] else "اختر اليوم"
    }

    private fun showConfirmationPopup(habit: Habit) {
        val msg = when (habit.frequency) {
            HabitFrequency.DAILY -> "تم إضافة عادة يومية بنجاح"
            HabitFrequency.WEEKLY -> "تم إضافة عادة أسبوعية كل يوم ${getDayName(habit.specificDay ?: 1)}"
            HabitFrequency.MONTHLY -> "تم إضافة عادة شهرية كل يوم ${habit.specificDay} في الشهر"
            else -> "تم الإضافة بنجاح"
        }

        AlertDialog.Builder(this, R.style.PurpleAlertDialog)
            .setTitle("تم الحفظ ✨")
            .setMessage(msg)
            .setPositiveButton("حسناً") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun loadSuggestions(group: ChipGroup, et: EditText, tvIcon: TextView, category: HabitCategory) {
        group.removeAllViews()
        val suggestions = if (category == HabitCategory.OTHER) {
            HabitTemplates.ALL_SUGGESTIONS.take(8)
        } else {
            HabitTemplates.ALL_SUGGESTIONS.filter { it.category == category }
        }
        
        suggestions.forEach { suggestion ->
            val chip = Chip(this).apply {
                text = "${suggestion.icon} ${suggestion.name}"
                setOnClickListener {
                    et.setText(suggestion.name)
                    tvIcon.text = suggestion.icon
                    selectedCategory = suggestion.category
                }
            }
            group.addView(chip)
        }
    }

    private fun updateIcon(tvIcon: TextView, category: HabitCategory) {
        tvIcon.text = when (category) {
            HabitCategory.HEALTH -> "🍎"
            HabitCategory.FITNESS -> "🏋️"
            HabitCategory.PRODUCTIVITY -> "💻"
            HabitCategory.LEARNING -> "📚"
            HabitCategory.SPIRITUAL -> "🕌"
            HabitCategory.SOCIAL -> "🤝"
            HabitCategory.FINANCE -> "💰"
            HabitCategory.HOBBY -> "🎨"
            HabitCategory.HOME -> "🧹"
            HabitCategory.SELF_IMPROVEMENT -> "🧘"
            else -> "⭐"
        }
    }
}