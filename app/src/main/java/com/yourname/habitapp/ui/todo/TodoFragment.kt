package com.yourname.habitapp.ui.todo

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.Habit
import com.yourname.habitapp.data.models.HabitFrequency
import com.yourname.habitapp.data.models.TodoItem
import com.yourname.habitapp.ui.habits.AddHabitBottomSheet
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TodoFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: MyTasksAdapter
    private var selectedDate = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
    private var currentFilter = "DAY"
    private var currentCategory = "ALL"

    private val combinedData = MediatorLiveData<Pair<List<TodoItem>, List<Habit>>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_todo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getInstance(requireContext())

        val btnSelectDate = view.findViewById<ImageButton>(R.id.btnSelectDate)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTodos)
        val btnTopAdd = view.findViewById<View>(R.id.btnTopAdd)
        val recyclerDayStrip = view.findViewById<RecyclerView>(R.id.recyclerDayStrip)
        val chipGroupFilter = view.findViewById<ChipGroup>(R.id.chipGroupFilter)
        val chipGroupCategory = view.findViewById<ChipGroup>(R.id.chipGroupCategory)
        val btnNotifications = view.findViewById<View>(R.id.btnNotificationsTodo)
        val settingsPrefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

        btnNotifications?.setOnClickListener {
            val isMuted = settingsPrefs.getBoolean("mute_notifications", false)
            val nextMute = !isMuted
            settingsPrefs.edit().putBoolean("mute_notifications", nextMute).apply()
            
            val msg = if (nextMute) "تم كتم التنبيهات 🔇" else "تم تفعيل التنبيهات 🔔"
            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
            
            // Visual feedback for the button if needed (optional)
            val bellIcon = if (nextMute) R.drawable.ic_notification_off else R.drawable.ic_notification
            (it as? ImageButton)?.setImageResource(bellIcon)
        }

        // Initial icon state
        val isMuted = settingsPrefs.getBoolean("mute_notifications", false)
        (btnNotifications as? ImageButton)?.setImageResource(if (isMuted) R.drawable.ic_notification_off else R.drawable.ic_notification)

        // Setup MediatorLiveData sources once
        combinedData.addSource(db.todoDao().getAllTodos()) { tasks ->
            combinedData.value = Pair(tasks ?: emptyList(), combinedData.value?.second ?: emptyList())
        }
        combinedData.addSource(db.habitDao().getAllHabits()) { habits ->
            combinedData.value = Pair(combinedData.value?.first ?: emptyList(), habits ?: emptyList())
        }

        combinedData.observe(viewLifecycleOwner) { (tasks, habits) ->
            loadFilteredItems(tasks, habits)
        }

        // 1. Setup Day Ribbon
        recyclerDayStrip.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        recyclerDayStrip.adapter = DayStripAdapter { date ->
            selectedDate = date
            updateUI()
        }

        // 2. Setup Tasks List
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MyTasksAdapter(
            onTodoToggle = { todo -> onTodoToggle(todo) },
            onHabitToggle = { habit -> onHabitToggle(habit) },
            onEdit = { item -> editItem(item) },
            onDelete = { item -> confirmDelete(item) }
        )
        recyclerView.adapter = adapter

        btnSelectDate.setOnClickListener {
            DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDate.set(y, m, d)
                updateUI()
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }

        chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipWeek -> "WEEK"
                R.id.chipMonth -> "MONTH"
                else -> "DAY"
            }
            updateUI()
        }

        chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            currentCategory = when (checkedIds.firstOrNull()) {
                R.id.chipOnlyTasks -> "TASKS"
                R.id.chipOnlyHabits -> "HABITS"
                else -> "ALL"
            }
            updateUI()
        }

        val onAddClick = View.OnClickListener {
            val options = arrayOf(getString(R.string.new_task), getString(R.string.new_habit))
            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(getString(R.string.add_options_title))
                .setItems(options) { _, which ->
                    if (which == 0) {
                        AddTodoBottomSheet.newInstance(selectedDate.timeInMillis)
                            .show(parentFragmentManager, "AddTodo")
                    } else {
                        val isFuture = selectedDate.timeInMillis > Calendar.getInstance().apply { 
                            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) 
                        }.timeInMillis
                        AddHabitBottomSheet.newInstance(showFreqBtn = isFuture, targetDate = selectedDate.timeInMillis)
                            .show(parentFragmentManager, "AddHabit")
                    }
                }.show()
        }

        btnTopAdd.setOnClickListener(onAddClick)

        updateUI()
    }

    private fun updateUI() {
        val now = Calendar.getInstance()
        val isToday = now.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                      now.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
        
        val dateText = if (isToday) getString(R.string.today) + "، " + dateFormatter.format(selectedDate.time)
                       else dateFormatter.format(selectedDate.time)
        
        view?.findViewById<TextView>(R.id.tvDisplayDate)?.text = dateText
        combinedData.value?.let { (tasks, habits) -> loadFilteredItems(tasks, habits) }
    }

    private fun loadFilteredItems(tasks: List<TodoItem>, habits: List<Habit>) {
        val start = selectedDate.clone() as Calendar
        val end = selectedDate.clone() as Calendar

        when (currentFilter) {
            "DAY" -> {
                start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0)
                end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59)
            }
            "WEEK" -> {
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.DAY_OF_YEAR, 7)
            }
            "MONTH" -> {
                start.set(Calendar.DAY_OF_MONTH, 1)
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            }
        }

        val filteredTasks = tasks.filter { it.targetDate in start.timeInMillis..end.timeInMillis }
        val filteredHabits = if (currentFilter == "DAY") {
            habits.filter { habit ->
                if (habit.targetDate != null) {
                    habit.targetDate >= start.timeInMillis && habit.targetDate <= end.timeInMillis
                } else {
                    habit.frequency == HabitFrequency.DAILY
                }
            }
        } else {
            habits.filter { habit ->
                habit.targetDate == null || (habit.targetDate >= start.timeInMillis && habit.targetDate <= end.timeInMillis)
            }
        }

        val combined = mutableListOf<Any>()
        if (currentCategory == "ALL" || currentCategory == "TASKS") combined.addAll(filteredTasks)
        if (currentCategory == "ALL" || currentCategory == "HABITS") combined.addAll(filteredHabits)

        adapter.submitList(combined)
    }

    private fun loadItems() {
        // Redundant, removed
    }

    private fun editItem(item: Any) {
        if (item is TodoItem) {
            AddTodoBottomSheet.newInstance(selectedDate.timeInMillis, item.id)
                .show(parentFragmentManager, "EditTodo")
        } else if (item is Habit) {
            AddHabitBottomSheet.newInstance(item.id, item.frequency, item.frequency != HabitFrequency.DAILY)
                .show(parentFragmentManager, "EditHabit")
        }
    }

    private fun onTodoToggle(todo: TodoItem) {
        lifecycleScope.launch {
            db.todoDao().update(todo.copy(isCompleted = !todo.isCompleted))
            // Silence notifications
            com.yourname.habitapp.utils.NotificationHelper.cancelNotification(requireContext(), todo.id * 10 + 1)
            com.yourname.habitapp.utils.NotificationHelper.cancelNotification(requireContext(), todo.id * 10 + 2)
            com.yourname.habitapp.utils.NotificationHelper.cancelNotification(requireContext(), todo.id * 10 + 3)
        }
    }

    private fun onHabitToggle(habit: Habit) {
        lifecycleScope.launch {
            val newCompleted = !habit.isCompletedToday
            val newStreak = if (newCompleted) habit.streak + 1 else Math.max(0, habit.streak - 1)
            val timestamp = if (newCompleted) System.currentTimeMillis() else null
            db.habitDao().updateHabitStreak(habit.id, newCompleted, newStreak, Math.max(habit.longestStreak, newStreak), timestamp)
            
            if (newCompleted) {
                com.yourname.habitapp.utils.NotificationHelper.cancelNotification(requireContext(), habit.name.hashCode())
            }
        }
    }

    private fun confirmDelete(item: Any) {
        AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
            .setTitle(getString(R.string.delete_confirm_title))
            .setMessage(getString(R.string.delete_confirm_msg))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                lifecycleScope.launch {
                    if (item is TodoItem) db.todoDao().delete(item)
                    else if (item is Habit) db.habitDao().deleteHabit(item)
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
}