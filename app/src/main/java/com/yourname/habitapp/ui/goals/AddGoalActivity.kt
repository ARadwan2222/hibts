package com.yourname.habitapp.ui.goals

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.GoalFrequency
import com.yourname.habitapp.data.models.GoalStep
import com.yourname.habitapp.data.models.YearGoal
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddGoalActivity : AppCompatActivity() {

    private val steps = mutableListOf<String>()
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)
    private var selectedTargetDate: Long? = null
    private var editingGoal: YearGoal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_goal)

        val etTitle      = findViewById<EditText>(R.id.etGoalTitle)
        val btnYear      = findViewById<Button>(R.id.btnSelectYear)
        val btnDate      = findViewById<Button>(R.id.btnSelectTargetDate)
        val etStep       = findViewById<EditText>(R.id.etNewStep)
        val btnAddStep   = findViewById<Button>(R.id.btnAddStep)
        val tvSteps      = findViewById<TextView>(R.id.tvStepsList)
        val btnSave      = findViewById<Button>(R.id.btnSaveGoal)

        val goalId = intent.getIntExtra("GOAL_ID", -1)
        if (goalId != -1) {
            lifecycleScope.launch {
                editingGoal = AppDatabase.getInstance(this@AddGoalActivity).yearGoalDao().getGoalById(goalId)
                editingGoal?.let {
                    etTitle.setText(it.title)
                    selectedYear = it.year
                    selectedTargetDate = it.targetDate
                    btnYear.text = "السنة: $selectedYear"
                    selectedTargetDate?.let { date ->
                        btnDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(date))
                    }
                    btnSave.text = "تحديث الهدف"
                }
            }
        }

        val intentYear = intent.getIntExtra("SELECTED_YEAR", -1)
        if (intentYear != -1 && goalId == -1) {
            selectedYear = intentYear
        }
        btnYear.text = "السنة: $selectedYear"

        btnYear.setOnClickListener {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val years = (-2..3).map { (currentYear + it).toString() }.toTypedArray()
            AlertDialog.Builder(this, R.style.PurpleAlertDialog)
                .setTitle("اختر السنة")
                .setItems(years) { _, which ->
                    selectedYear = years[which].toInt()
                    btnYear.text = "السنة: $selectedYear"
                }.show()
        }

        btnDate.setOnClickListener {
            val cal = Calendar.getInstance()
            selectedTargetDate?.let { cal.timeInMillis = it }
            DatePickerDialog(this, R.style.PurpleAlertDialog, { _, y, m, d ->
                val selected = Calendar.getInstance().apply { set(y, m, d) }
                selectedTargetDate = selected.timeInMillis
                btnDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selected.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

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
                val goal = editingGoal?.copy(
                    title = title,
                    year = selectedYear,
                    targetDate = selectedTargetDate
                ) ?: YearGoal(
                    title = title,
                    year = selectedYear,
                    targetDate = selectedTargetDate,
                    frequency = GoalFrequency.YEARLY
                )
                
                if (editingGoal != null) {
                    db.yearGoalDao().updateGoal(goal)
                } else {
                    val newGoalId = db.yearGoalDao().insertGoal(goal)
                    steps.forEachIndexed { index, stepTitle ->
                        db.yearGoalDao().insertStep(GoalStep(
                            goalId = newGoalId.toInt(),
                            title = stepTitle,
                            order = index
                        ))
                    }
                }
                finish()
            }
        }
    }
}
