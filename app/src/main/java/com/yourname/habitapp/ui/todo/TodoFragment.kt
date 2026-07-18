package com.yourname.habitapp.ui.todo

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
import androidx.recyclerview.widget.ItemTouchHelper
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

    fun getSelectedDateMillis(): Long = selectedDate.timeInMillis

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_todo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getInstance(requireContext())

        val btnSelectDate = view.findViewById<ImageButton>(R.id.btnSelectDate)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTodos)

        val recyclerDayStrip = view.findViewById<RecyclerView>(R.id.recyclerDayStrip)
        val chipGroupFilter = view.findViewById<ChipGroup>(R.id.chipGroupFilter)
        val chipGroupCategory = view.findViewById<ChipGroup>(R.id.chipGroupCategory)
        val btnNotifications = view.findViewById<View>(R.id.btnNotificationsTodo)
        val settingsPrefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

        btnNotifications?.setOnClickListener {
            val isMuted = settingsPrefs.getBoolean("mute_notifications", false)
            val nextMute = !isMuted
            settingsPrefs.edit().putBoolean("mute_notifications", nextMute).apply()
            
            if (nextMute) com.yourname.habitapp.utils.NotificationHelper.stopAllSounds(requireContext())
            
            val msg = if (nextMute) "تم كتم التنبيهات 🔇" else "تم تفعيل التنبيهات 🔔"
            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
            
            val bellIcon = if (nextMute) R.drawable.ic_notification_off else R.drawable.ic_notification
            (it as? ImageButton)?.setImageResource(bellIcon)
        }

        val isMutedInitial = settingsPrefs.getBoolean("mute_notifications", false)
        (btnNotifications as? ImageButton)?.setImageResource(if (isMutedInitial) R.drawable.ic_notification_off else R.drawable.ic_notification)

        combinedData.addSource(db.todoDao().getAllTodos()) { tasks ->
            combinedData.value = Pair(tasks ?: emptyList(), combinedData.value?.second ?: emptyList())
        }
        combinedData.addSource(db.habitDao().getAllHabits()) { habits ->
            combinedData.value = Pair(combinedData.value?.first ?: emptyList(), habits ?: emptyList())
        }

        combinedData.observe(viewLifecycleOwner) { (tasks, habits) ->
            loadFilteredItems(tasks, habits)
        }

        recyclerDayStrip.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        recyclerDayStrip.adapter = DayStripAdapter { date ->
            selectedDate = date
            updateUI()
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MyTasksAdapter(
            onTodoToggle = { todo -> onTodoToggle(todo) },
            onHabitToggle = { habit -> onHabitToggle(habit) },
            onEdit = { item -> editItem(item) },
            onDelete = { item -> confirmDelete(item) },
            onMuteToggle = { item -> toggleMute(item) },
            onNotesClick = { item -> showNotes(item) }
        )
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT) {
            private var fromPosition: Int = -1
            private var toPosition: Int = -1

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.let {
                        it.animate().scaleX(1.06f).scaleY(1.06f).setDuration(150).start()
                        it.elevation = 40f
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            it.foreground = android.graphics.drawable.ColorDrawable(0x4D000000) // 30% Darker
                        }
                    }
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.let {
                    it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    it.elevation = 2f
                    it.isPressed = false
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        it.foreground = null
                    }
                }

                if (fromPosition != -1 && toPosition != -1 && fromPosition != toPosition) {
                    val list = adapter.currentList
                    lifecycleScope.launch {
                        val tasks = list.filterIsInstance<TodoItem>().mapIndexed { index, item -> item.copy(displayOrder = index) }
                        val habits = list.filterIsInstance<Habit>().mapIndexed { index, item -> item.copy(displayOrder = index) }
                        if (tasks.isNotEmpty()) db.todoDao().updateAll(tasks)
                        if (habits.isNotEmpty()) db.habitDao().updateAllHabits(habits)
                        Toast.makeText(requireContext(), "تم تحديث الترتيب بنجاح ✅", Toast.LENGTH_SHORT).show()
                    }
                }
                fromPosition = -1
                toPosition = -1
            }

            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean {
                val from = vh.adapterPosition
                val to = t.adapterPosition
                if (fromPosition == -1) fromPosition = from
                toPosition = to
                
                val list = adapter.currentList.toMutableList()
                Collections.swap(list, from, to)
                adapter.submitList(list)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                val item = adapter.currentList[pos]
                adapter.notifyItemChanged(pos)
                confirmDelete(item)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

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

        applyThemeDecorations(view)

        updateUI()
    }

    private fun applyThemeDecorations(view: View) {
        val settingsPrefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val themeName = settingsPrefs.getString("app_theme", "Male")

        val emojis = when(themeName) {
            "Cats" -> "🐱🐾🐈😻"
            "Dogs" -> "🐶🦴🐕🐕‍🦺"
            "Travel" -> "✈️🌍🏨🗼"
            "Nature" -> "🌿🌻🌲🌳"
            "Ocean" -> "🌊🐬🐙⛵"
            "Sunset" -> "🌅🌇🌙⭐"
            "Space" -> "🚀⭐🪐🛸"
            "Coffee" -> "☕🥐🍩🍪"
            "Tech" -> "💻📱⌨️🖱️"
            "Vintage" -> "🕰️🎞️📻⏳"
            "Gold" -> "👑💰💎✨"
            else -> ""
        }

        view.findViewById<TextView>(R.id.tvThemeDecorationTodo)?.text = emojis
        view.findViewById<TextView>(R.id.tvThemeDecorationTodoBottom)?.text = emojis
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

        // Sort: Incomplete items first, then complete items.
        // Within each group: respect the Manual displayOrder first, then priority, then time.
        // Grouping: Tasks first, then Habits.
        val sortedCombined = combined.sortedWith(compareBy<Any> {
            when (it) {
                is TodoItem -> it.isCompleted
                is Habit -> it.isCompletedToday
                else -> false
            }
        }.thenBy {
            // Grouping: Tasks (0) then Habits (1)
            if (it is TodoItem) 0 else 1
        }.thenBy {
            when (it) {
                is TodoItem -> it.displayOrder
                is Habit -> it.displayOrder
                else -> 0
            }
        }.thenBy {
            when (it) {
                is TodoItem -> it.priority.ordinal
                is Habit -> 1
                else -> 2
            }
        }.thenBy {
            when (it) {
                is TodoItem -> it.startTime ?: Long.MAX_VALUE
                else -> Long.MAX_VALUE
            }
        })

        adapter.submitList(sortedCombined)
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

    private fun showNotes(item: Any) {
        val (title, notes) = when(item) {
            is TodoItem -> Pair(item.title, item.notes)
            is Habit -> Pair(item.name, item.notes)
            else -> return
        }
        AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
            .setTitle(title)
            .setMessage(notes)
            .setPositiveButton(getString(R.string.close), null)
            .show()
    }

    private fun toggleMute(item: Any) {
        lifecycleScope.launch {
            if (item is TodoItem) {
                // If reminders were never enabled, show toast and don't toggle
                if (!item.reminderStart && !item.reminderEnd) {
                    Toast.makeText(requireContext(), "التنبيه غير مفعل لهذه المهمة", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val nextMuted = !item.isMuted
                db.todoDao().update(item.copy(isMuted = nextMuted))
                if (nextMuted) com.yourname.habitapp.utils.NotificationHelper.stopAllSounds(requireContext())
                val msg = if (nextMuted) "تم كتم المهمة 🔇" else "تنبيهات المهمة مفعلة 🔔"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            } else if (item is Habit) {
                val nextMuted = !item.isMuted
                db.habitDao().updateHabit(item.copy(isMuted = nextMuted))
                if (nextMuted) com.yourname.habitapp.utils.NotificationHelper.stopAllSounds(requireContext())
                val msg = if (nextMuted) "تم كتم العادة 🔇" else "تنبيهات العادة مفعلة 🔔"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onTodoToggle(todo: TodoItem) {
        lifecycleScope.launch {
            db.todoDao().update(todo.copy(isCompleted = !todo.isCompleted))
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
