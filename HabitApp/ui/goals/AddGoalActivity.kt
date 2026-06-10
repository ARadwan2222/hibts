package com.yourname.habitapp.ui.goals

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.GoalStep
import com.yourname.habitapp.data.models.YearGoal
import kotlinx.coroutines.launch

class AddGoalActivity : AppCompatActivity() {

    private val icons = listOf("🎯","📚","💪","💰","🌱","🏆","✈️","🎨","💻","🏠","❤️","🧘","🎵","⭐")
    private val steps = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_goal)

        val etTitle      = findViewById<EditText>(R.id.etGoalTitle)
        val etDesc       = findViewById<EditText>(R.id.etGoalDescription)
        val spinnerIcon  = findViewById<Spinner>(R.id.spinnerGoalIcon)
        val etStep       = findViewById<EditText>(R.id.etNewStep)
        val btnAddStep   = findViewById<Button>(R.id.btnAddStep)
        val tvSteps      = findViewById<TextView>(R.id.tvStepsList)
        val btnSave      = findViewById<Button>(R.id.btnSaveGoal)

        spinnerIcon.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, icons)

        btnAddStep.setOnClickListener {
            val step = etStep.text.toString().trim()
            if (step.isNotEmpty()) {
                steps.add(step)
                etStep.text.clear()
                tvSteps.text = steps.mapIndexed { i, s -> "${i+1}. $s" }.joinToString("\n")
            }
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) { etTitle.error = "أدخل عنوان الهدف"; return@setOnClickListener }

            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@AddGoalActivity)
                val goal = YearGoal(
                    title = title,
                    description = etDesc.text.toString().trim(),
                    icon = icons[spinnerIcon.selectedItemPosition]
                )
                val goalId = db.yearGoalDao().insertGoal(goal)

                // إضافة الخطوات
                steps.forEachIndexed { index, stepTitle ->
                    db.yearGoalDao().insertStep(GoalStep(
                        goalId = goalId.toInt(),
                        title = stepTitle,
                        order = index
                    ))
                }
                finish()
            }
        }
    }
}
