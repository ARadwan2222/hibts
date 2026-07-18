package com.yourname.habitapp.ui.todo

import android.app.DatePickerDialog
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
    private var selectedDate = Calendar.getInstance()
    private var editingTodoId: Int = -1

    companion object {
        fun newInstance(dateMillis: Long, todoId: Int = -1): AddTodoBottomSheet {
            val fragment = AddTodoBottomSheet()
            val args = Bundle()
            args.putLong("SELECTED_DATE", dateMillis)
            args.putInt("TODO_ID", todoId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val millis = it.getLong("SELECTED_DATE")
            if (millis > 0) selectedDate.timeInMillis = millis
            editingTodoId = it.getInt("TODO_ID", -1)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_todo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle             = view.findViewById<EditText>(R.id.etTodoTitle)
        val chipGroupPri        = view.findViewById<ChipGroup>(R.id.chipGroupPriority)
        val btnStartTime        = view.findViewById<Button>(R.id.btnStartTime)
        val btnEndTime          = view.findViewById<Button>(R.id.btnEndTime)
        val btnDate             = view.findViewById<Button>(R.id.btnDate)
        val btnSave             = view.findViewById<Button>(R.id.btnSaveTodo)
        val spinnerCat          = view.findViewById<Spinner>(R.id.spinnerCategory)
        val chipTemplates       = view.findViewById<ChipGroup>(R.id.chipGroupTemplates)
        val btnToggleSugg       = view.findViewById<ImageButton>(R.id.btnToggleSuggestions)
        val cbReminderStart     = view.findViewById<CheckBox>(R.id.switchReminderStart)
        val cbReminderEnd       = view.findViewById<CheckBox>(R.id.switchReminderEnd)

        // Suggestions Removed
        btnToggleSugg?.visibility = View.GONE
        chipTemplates?.visibility = View.GONE

        val etNotes             = view.findViewById<EditText>(R.id.etTodoNotes)

        if (editingTodoId != -1) {
            lifecycleScope.launch {
                val todo = AppDatabase.getInstance(requireContext()).todoDao().getTodoById(editingTodoId)
                todo?.let {
                    etTitle.setText(it.title)
                    etNotes.setText(it.notes)
                    selectedStartTime = it.startTime
                    selectedEndTime = it.endTime
                    selectedDate.timeInMillis = it.targetDate
                    btnSave.text = getString(R.string.update)
                    // Color is handled by ?attr/colorPrimary in XML now
                    btnDate.text = formatDate(it.targetDate)
                    if (it.startTime != null) btnStartTime.text = String.format(Locale.getDefault(), "%s: %s", getString(R.string.start), formatTime(it.startTime))
                    if (it.endTime != null) btnEndTime.text = String.format(Locale.getDefault(), "%s: %s", getString(R.string.end), formatTime(it.endTime))
                    cbReminderStart.isChecked = it.reminderStart
                    cbReminderEnd.isChecked = it.reminderEnd
                    
                    when (it.priority) {
                        Priority.HIGH -> chipGroupPri.check(R.id.chipHigh)
                        Priority.LOW -> chipGroupPri.check(R.id.chipLow)
                        else -> chipGroupPri.check(R.id.chipMedium)
                    }
                }
            }
        } else {
            btnDate.text = formatDate(selectedDate.timeInMillis)
        }

        btnDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDate.set(y, m, d)
                btnDate.text = String.format(Locale.getDefault(), "%02d/%02d/%d", d, m + 1, y)
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        val categories = TaskTemplates.ALL_CATEGORIES
        spinnerCat.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories.map { "${it.icon} ${it.name}" })
        spinnerCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                // Template loading removed
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        btnStartTime.setOnClickListener {
            showTimePicker { h, m ->
                selectedStartTime = getTimestamp(h, m)
                btnStartTime.text = String.format(Locale.getDefault(), getString(R.string.start) + ": %02d:%02d", h, m)
            }
        }

        btnEndTime.setOnClickListener {
            showTimePicker { h, m ->
                selectedEndTime = getTimestamp(h, m)
                btnEndTime.text = String.format(Locale.getDefault(), getString(R.string.end) + ": %02d:%02d", h, m)
            }
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val notes = etNotes.text.toString().trim()
            if (title.isEmpty()) { etTitle.error = getString(R.string.error_empty_field); return@setOnClickListener }

            val wordCount = title.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
            if (wordCount > 4) {
                etTitle.error = getString(R.string.error_max_words)
                return@setOnClickListener
            }

            val priority = when (chipGroupPri.checkedChipId) {
                R.id.chipHigh -> Priority.HIGH
                R.id.chipLow  -> Priority.LOW
                else          -> Priority.MEDIUM
            }

            lifecycleScope.launch {
                val db = AppDatabase.getInstance(requireContext())
                val allTodos = db.todoDao().getAllTodosSync()
                val isDuplicate = allTodos.any { it.title.trim().equals(title, ignoreCase = true) && it.id != editingTodoId }
                
                if (isDuplicate) {
                    Toast.makeText(requireContext(), "هذه المهمة موجودة بالفعل!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Calculate duration in minutes if end time is selected
                val duration = if (selectedStartTime != null && selectedEndTime != null) {
                    ((selectedEndTime!! - selectedStartTime!!) / (1000 * 60)).toInt()
                } else 0

                if (editingTodoId != -1) {
                    val original = db.todoDao().getTodoById(editingTodoId)
                    val updated = original?.copy(
                        title = title,
                        notes = notes,
                        priority = priority,
                        startTime = selectedStartTime,
                        endTime = selectedEndTime,
                        durationMinutes = if (duration > 0) duration else original.durationMinutes,
                        targetDate = selectedDate.timeInMillis,
                        reminderStart = cbReminderStart.isChecked,
                        reminderEnd = cbReminderEnd.isChecked
                    )
                    updated?.let { 
                        db.todoDao().update(it)
                        // Reschedule reminders if time changed
                        if (it.startTime != null && it.reminderStart) {
                            TodoReminderScheduler.scheduleTodoReminders(requireContext(), it)
                        }
                    }
                } else {
                    val minOrder = allTodos.minOfOrNull { it.displayOrder } ?: 0
                    val todo = TodoItem(
                        title          = title,
                        notes          = notes,
                        priority       = priority,
                        startTime      = selectedStartTime,
                        targetDate     = selectedDate.timeInMillis,
                        endTime        = selectedEndTime,
                        durationMinutes = if (duration > 0) duration else 0,
                        reminderStart  = cbReminderStart.isChecked,
                        reminderEnd    = cbReminderEnd.isChecked,
                        reminderBefore = 10,
                        displayOrder   = minOrder - 1,
                        createdAt      = System.currentTimeMillis()
                    )
                    val id = db.todoDao().insert(todo)
                    if (selectedStartTime != null && cbReminderStart.isChecked) {
                        TodoReminderScheduler.scheduleTodoReminders(requireContext(), todo.copy(id = id.toInt()))
                    }
                }
                dismiss()
            }
        }
    }

    private fun showTimePicker(onTime: (Int, Int) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, h, m -> onTime(h, m) },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    private fun getTimestamp(hour: Int, minute: Int): Long {
        val out = selectedDate.clone() as Calendar
        out.set(Calendar.HOUR_OF_DAY, hour)
        out.set(Calendar.MINUTE, minute)
        out.set(Calendar.SECOND, 0)
        return out.timeInMillis
    }

    private fun formatDate(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format(Locale.getDefault(), "%02d/%02d/%d", 
            cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }

    private fun formatTime(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format(Locale.getDefault(), "%02d:%02d", 
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }
}