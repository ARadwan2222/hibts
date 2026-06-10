package com.yourname.habitapp.ui.habits

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.Habit
import com.yourname.habitapp.data.models.HabitFrequency
import com.yourname.habitapp.utils.AchievementEngine
import kotlinx.coroutines.launch

class AddHabitActivity : AppCompatActivity() {

    private val icons = listOf("⭐","💧","📚","🏃","🧘","💪","🎯","🌱","💰","🎵","🍎","☀️","🌙","✍️","🧹")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_habit)

        val etName        = findViewById<EditText>(R.id.etHabitName)
        val spinnerFreq   = findViewById<Spinner>(R.id.spinnerFrequency)
        val spinnerIcon   = findViewById<Spinner>(R.id.spinnerIcon)
        val btnSave       = findViewById<Button>(R.id.btnSaveHabit)
        val btnBack       = findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        // إعداد Spinner التكرار
        val freqOptions = listOf("يومي", "أسبوعي", "شهري")
        spinnerFreq.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, freqOptions)

        // إعداد Spinner الأيقونة
        spinnerIcon.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, icons)

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                etName.error = "أدخل اسم العادة"
                return@setOnClickListener
            }

            val frequency = when (spinnerFreq.selectedItemPosition) {
                1 -> HabitFrequency.WEEKLY
                2 -> HabitFrequency.MONTHLY
                else -> HabitFrequency.DAILY
            }

            val habit = Habit(
                name = name,
                icon = icons[spinnerIcon.selectedItemPosition],
                frequency = frequency
            )

            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@AddHabitActivity)
                db.habitDao().insertHabit(habit)

                // فحص إنجاز أول عادة
                val count = db.habitDao().getAllHabitsSync().size
                AchievementEngine.checkAndUnlock(this@AddHabitActivity, "HABIT_ADDED", count)

                finish()
            }
        }
    }
}
