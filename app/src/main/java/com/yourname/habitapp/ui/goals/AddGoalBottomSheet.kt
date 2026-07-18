package com.yourname.habitapp.ui.goals

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.GoalFrequency
import com.yourname.habitapp.data.models.GoalStep
import com.yourname.habitapp.data.models.YearGoal
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddGoalBottomSheet : BottomSheetDialogFragment() {

    private val steps = mutableListOf<GoalStep>()
    private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)
    private var selectedQuarter: Int? = null
    private var selectedTargetDate: Long? = null
    private var editingGoal: YearGoal? = null
    private var editingStepIndex: Int = -1

    companion object {
        private const val ARG_GOAL_ID = "goal_id"
        private const val ARG_SELECTED_YEAR = "selected_year"

        fun newInstance(goalId: Int = -1, selectedYear: Int = -1): AddGoalBottomSheet {
            return AddGoalBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_GOAL_ID, goalId)
                    putInt(ARG_SELECTED_YEAR, selectedYear)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_goal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val goalId = arguments?.getInt(ARG_GOAL_ID) ?: -1
        val intentYear = arguments?.getInt(ARG_SELECTED_YEAR) ?: -1

        val etTitle             = view.findViewById<EditText>(R.id.etGoalTitle)
        val etNotes             = view.findViewById<EditText>(R.id.etGoalNotes)
        val btnYear             = view.findViewById<Button>(R.id.btnSelectYear)
        val btnQuarter          = view.findViewById<Button>(R.id.btnSelectQuarter)
        val btnDate             = view.findViewById<Button>(R.id.btnSelectTargetDate)
        val etStep              = view.findViewById<EditText>(R.id.etNewStep)
        val btnAddStep          = view.findViewById<Button>(R.id.btnAddStep)
        val recyclerStepsEdit   = view.findViewById<RecyclerView>(R.id.recyclerStepsEdit)
        val btnSave             = view.findViewById<Button>(R.id.btnSaveGoal)
        val tvSheetTitle        = view.findViewById<TextView>(R.id.tvSheetTitle)

        val stepsAdapter = EditStepsAdapter(
            onEdit = { index, step ->
                etStep.setText(step.title)
                etStep.requestFocus()
                editingStepIndex = index
                btnAddStep.text = "✓"
            },
            onDelete = { index ->
                steps.removeAt(index)
                updateStepsUI(recyclerStepsEdit)
            }
        )
        recyclerStepsEdit.layoutManager = LinearLayoutManager(requireContext())
        recyclerStepsEdit.adapter = stepsAdapter

        if (goalId != -1) {
            lifecycleScope.launch {
                val dao = AppDatabase.getInstance(requireContext()).yearGoalDao()
                editingGoal = dao.getGoalById(goalId)
                editingGoal?.let {
                    tvSheetTitle.text = getString(R.string.edit_goal)
                    etTitle.setText(it.title)
                    etNotes.setText(it.notes)
                    selectedYear = it.year
                    selectedQuarter = it.quarter
                    selectedTargetDate = it.targetDate
                    btnYear.text = getString(R.string.year_label).format(selectedYear)
                    btnQuarter.text = if (it.quarter != null) {
                        val qNames = arrayOf("", getString(R.string.quarter_q1), getString(R.string.quarter_q2), getString(R.string.quarter_q3), getString(R.string.quarter_q4))
                        getString(R.string.quarter_of_year).format(qNames[it.quarter], it.year)
                    } else getString(R.string.full_year).format(it.year)
                    selectedTargetDate?.let { date ->
                        btnDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(date))
                    }
                    btnSave.text = getString(R.string.update)

                    // Load existing steps
                    val existingSteps = dao.getStepsForGoalSync(it.id)
                    steps.clear()
                    steps.addAll(existingSteps)
                    updateStepsUI(recyclerStepsEdit)
                }
            }
        } else {
            if (intentYear != -1) selectedYear = intentYear
            tvSheetTitle.text = getString(R.string.add_new_goal)
            btnYear.text = getString(R.string.year_label).format(selectedYear)
            btnQuarter.text = getString(R.string.full_year).format(selectedYear)
        }

        btnYear.setOnClickListener {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val yearsArr = (-2..3).map { (currentYear + it).toString() }.toTypedArray()
            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(getString(R.string.select_year))
                .setItems(yearsArr) { _, which ->
                    selectedYear = yearsArr[which].toInt()
                    btnYear.text = getString(R.string.year_label).format(selectedYear)
                    updateQuarterText(btnQuarter)
                }.show()
        }

        btnQuarter.setOnClickListener {
            val qNames = arrayOf("", getString(R.string.quarter_q1), getString(R.string.quarter_q2), getString(R.string.quarter_q3), getString(R.string.quarter_q4))
            val quarters = arrayOf(
                getString(R.string.full_year).format(selectedYear),
                getString(R.string.quarter_of_year).format(qNames[1], selectedYear),
                getString(R.string.quarter_of_year).format(qNames[2], selectedYear),
                getString(R.string.quarter_of_year).format(qNames[3], selectedYear),
                getString(R.string.quarter_of_year).format(qNames[4], selectedYear)
            )
            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(getString(R.string.select_time))
                .setItems(quarters) { _, which ->
                    selectedQuarter = if (which == 0) null else which
                    btnQuarter.text = quarters[which]
                }.show()
        }

        btnDate.setOnClickListener {
            val cal = Calendar.getInstance()
            selectedTargetDate?.let { cal.timeInMillis = it }
            DatePickerDialog(requireContext(), R.style.PurpleAlertDialog, { _, y, m, d ->
                val selected = Calendar.getInstance().apply { set(y, m, d) }
                selectedTargetDate = selected.timeInMillis
                btnDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selected.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnAddStep.setOnClickListener {
            val stepTitle = etStep.text.toString().trim()
            if (stepTitle.isNotEmpty()) {
                if (editingStepIndex != -1) {
                    // Update only the edited step, maintain position
                    steps[editingStepIndex] = steps[editingStepIndex].copy(title = stepTitle)
                    editingStepIndex = -1
                    btnAddStep.text = "+"
                } else {
                    steps.add(GoalStep(goalId = editingGoal?.id ?: 0, title = stepTitle))
                }
                etStep.text.clear()
                updateStepsUI(recyclerStepsEdit)
            }
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val notes = etNotes.text.toString().trim()
            if (title.isEmpty()) { etTitle.error = getString(R.string.error_empty_field); return@setOnClickListener }

            lifecycleScope.launch {
                val db = AppDatabase.getInstance(requireContext())
                val dao = db.yearGoalDao()
                val goal = editingGoal?.copy(
                    title = title,
                    notes = notes,
                    year = selectedYear,
                    quarter = selectedQuarter,
                    targetDate = selectedTargetDate
                ) ?: YearGoal(
                    title = title,
                    notes = notes,
                    year = selectedYear,
                    quarter = selectedQuarter,
                    targetDate = selectedTargetDate,
                    frequency = GoalFrequency.YEARLY
                )
                
                val finalGoalId: Int
                if (editingGoal != null) {
                    dao.updateGoal(goal)
                    finalGoalId = goal.id
                    val originalSteps = dao.getStepsForGoalSync(finalGoalId)
                    val currentStepIds = steps.map { it.id }.filter { it != 0 }
                    originalSteps.filter { it.id !in currentStepIds }.forEach { dao.deleteStep(it) }
                } else {
                    finalGoalId = dao.insertGoal(goal).toInt()
                }

                steps.forEachIndexed { index, step ->
                    val finalStep = step.copy(goalId = finalGoalId, order = index)
                    if (finalStep.id == 0) {
                        dao.insertStep(finalStep)
                    } else {
                        dao.updateStep(finalStep)
                    }
                }
                
                dao.recalculateProgress(finalGoalId)
                dismiss()
            }
        }
    }

    private fun updateQuarterText(btn: Button) {
        if (selectedQuarter == null) {
            btn.text = getString(R.string.full_year).format(selectedYear)
        } else {
            val qNames = arrayOf("", getString(R.string.quarter_q1), getString(R.string.quarter_q2), getString(R.string.quarter_q3), getString(R.string.quarter_q4))
            btn.text = getString(R.string.quarter_of_year).format(qNames[selectedQuarter!!], selectedYear)
        }
    }

    private fun updateStepsUI(recycler: RecyclerView) {
        val adapter = recycler.adapter as? EditStepsAdapter
        adapter?.submitList(steps.toList())
    }
}

class EditStepsAdapter(
    private val onEdit: (Int, GoalStep) -> Unit,
    private val onDelete: (Int) -> Unit
) : ListAdapter<GoalStep, EditStepsAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvNum: TextView = v.findViewById(R.id.tvStepNumber)
        val tvTitle: TextView = v.findViewById(R.id.tvStepTitle)
        val btnEdit: View = v.findViewById(R.id.btnEditStep)
        val btnDelete: View = v.findViewById(R.id.btnDeleteStep)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_goal_step_edit, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val step = getItem(position)
        holder.tvNum.text = "${position + 1}."
        holder.tvTitle.text = step.title
        holder.btnEdit.setOnClickListener { onEdit(holder.adapterPosition, step) }
        holder.btnDelete.setOnClickListener { onDelete(holder.adapterPosition) }
    }

    class DiffCallback : DiffUtil.ItemCallback<GoalStep>() {
        override fun areItemsTheSame(a: GoalStep, b: GoalStep) = a.id == b.id && a.id != 0
        override fun areContentsTheSame(a: GoalStep, b: GoalStep) = a == b
    }
}
