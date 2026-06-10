package com.yourname.habitapp.ui.todo

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.Priority
import com.yourname.habitapp.data.models.TodoItem
import com.yourname.habitapp.utils.TaskTemplates
import com.yourname.habitapp.utils.TodoReminderScheduler
import kotlinx.coroutines.launch
import java.util.*

class AddTodoBottomSheet : BottomSheetDialogFragment() {

    private var selectedStartTime: Long? = null
    private var selectedEndTime: Long? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_todo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle       = view.findViewById<EditText>(R.id.etTodoTitle)
        val chipGroupPri  = view.findViewById<ChipGroup>(R.id.chipGroupPriority)
        val btnStartTime  = view.findViewById<Button>(R.id.btnStartTime)
        val btnEndTime    = view.findViewById<Button>(R.id.btnEndTime)
        val switchStart   = view.findViewById<Switch>(R.id.switchReminderStart)
        val switchEnd     = view.findViewById<Switch>(R.id.switchReminderEnd)
        val spinnerBefore = view.findViewById<Spinner>(R.id.spinnerReminderBefore)
        val btnSave       = view.findViewById<Button>(R.id.btnSaveTodo)
        val spinnerCat    = view.findViewById<Spinner>(R.id.spinnerCategory)
        val chipTemplates = view.findViewById<ChipGroup>(R.id.chipGroupTemplates)

        // Spinner الفئات
        val categories = TaskTemplates.ALL_CATEGORIES
        spinnerCat.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item,
            categories.map { "${it.icon} ${it.name}" })

        spinnerCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                loadTemplates(chipTemplates, etTitle, categories[pos].tasks.map { it.title })
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Spinner التذكير المسبق
        val beforeOptions = listOf("5 دقائق", "10 دقائق", "15 دقائق", "30 دقائق")
        spinnerBefore.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, beforeOptions)
        spinnerBefore.setSelection(1)

        // اختيار وقت البداية
        btnStartTime.setOnClickListener {
            showTimePicker { h, m ->
                selectedStartTime = getTimestamp(h, m)
                btnStartTime.text = String.format("البداية: %02d:%02d", h, m)
            }
        }

        // اختيار وقت النهاية
        btnEndTime.setOnClickListener {
            showTimePicker { h, m ->
                selectedEndTime = getTimestamp(h, m)
                btnEndTime.text = String.format("النهاية: %02d:%02d", h, m)
            }
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            if (title.isEmpty()) { etTitle.error = "أدخل عنوان المهمة"; return@setOnClickListener }

            val priority = when (chipGroupPri.checkedChipId) {
                R.id.chipHigh -> Priority.HIGH
                R.id.chipLow  -> Priority.LOW
                else          -> Priority.MEDIUM
            }

            val reminderBefore = when (spinnerBefore.selectedItemPosition) {
                0 -> 5; 2 -> 15; 3 -> 30; else -> 10
            }

            val todo = TodoItem(
                title         = title,
                priority      = priority,
                startTime     = selectedStartTime,
                endTime       = selectedEndTime,
                reminderStart = switchStart.isChecked,
                reminderEnd   = switchEnd.isChecked,
                reminderBefore = reminderBefore
            )

            lifecycleScope.launch {
                val db = AppDatabase.getInstance(requireContext())
                val id = db.todoDao().insert(todo)
                // جدولة التنبيهات
                TodoReminderScheduler.scheduleTodoReminders(
                    requireContext(), todo.copy(id = id.toInt())
                )
                dismiss()
            }
        }
    }

    private fun loadTemplates(chipGroup: ChipGroup, etTitle: EditText, templates: List<String>) {
        chipGroup.removeAllViews()
        templates.forEach { template ->
            val chip = Chip(requireContext()).apply {
                text = template
                isClickable = true
                setOnClickListener { etTitle.setText(template) }
            }
            chipGroup.addView(chip)
        }
    }

    private fun showTimePicker(onTime: (Int, Int) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, h, m -> onTime(h, m) },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    private fun getTimestamp(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }
}
