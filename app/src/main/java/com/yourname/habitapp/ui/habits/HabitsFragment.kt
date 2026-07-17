package com.yourname.habitapp.ui.habits

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.chip.ChipGroup
import com.yourname.habitapp.utils.AchievementEngine
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.Habit
import com.yourname.habitapp.data.models.HabitFrequency
import com.yourname.habitapp.data.models.TodoItem
import com.yourname.habitapp.ui.todo.AddTodoBottomSheet
import com.yourname.habitapp.ui.todo.TodoAdapter
import kotlinx.coroutines.launch

class HabitsFragment : Fragment() {

    private lateinit var db: AppDatabase
    private var countDownTimer: CountDownTimer? = null
    private val flashHandler = Handler(Looper.getMainLooper())
    private var flashRunnable: Runnable? = null

    private lateinit var taskAdapter: TodoAdapter
    private lateinit var dailyAdapter: HabitAdapter
    private lateinit var weeklyAdapter: HabitAdapter
    private lateinit var monthlyAdapter: HabitAdapter
    
    private val combinedData = MediatorLiveData<Pair<List<TodoItem>, List<Habit>>>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_habits, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getInstance(requireContext())

        val prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "User")
        view.findViewById<TextView>(R.id.tvUserNameMain)?.text = userName

        val ivTopProfile = view.findViewById<ImageView>(R.id.ivProfilePicTop)
        val imageUri = prefs.getString("user_image", null)
        if (imageUri != null) {
            ivTopProfile?.setImageURI(Uri.parse(imageUri))
        }

        val tvDateHeader = view.findViewById<TextView>(R.id.tvDateHeader)
        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        tvDateHeader?.text = sdf.format(Date())

        setupAdapters()
        setupRecyclers(view)
        setupObservers(view)
        setupClickListeners(view)
        setupFilters(view)
        setupFlashing(view.findViewById(R.id.cardFocus))
        
        setupSwipeDelete(view.findViewById(R.id.recyclerCurrentTasks), taskAdapter)
        setupSwipeDelete(view.findViewById(R.id.recyclerDailyHabits), dailyAdapter)
        setupSwipeDelete(view.findViewById(R.id.recyclerWeeklyHabits), weeklyAdapter)
        setupSwipeDelete(view.findViewById(R.id.recyclerMonthlyHabits), monthlyAdapter)
    }

    private fun setupAdapters() {
        taskAdapter = TodoAdapter(
            onCompleteClick = { todo -> onTodoToggle(todo) },
            onEditClick = { todo -> openEditTodo(todo) },
            onDeleteClick = { todo -> showDeleteConfirmation(todo) },
            onMuteToggle = { todo -> toggleTodoMute(todo) },
            onNotesClick = { todo -> showNotes(todo) }
        )
        dailyAdapter = HabitAdapter(
            onCompleteClick = { habit -> onHabitCompleted(habit) },
            onEditClick = { habit -> openEditHabit(habit) },
            onDeleteClick = { habit -> showDeleteConfirmation(habit) },
            onNotesClick = { habit -> showNotes(habit) }
        )
        weeklyAdapter = HabitAdapter(
            onCompleteClick = { habit -> onHabitCompleted(habit) },
            onEditClick = { habit -> openEditHabit(habit) },
            onDeleteClick = { habit -> showDeleteConfirmation(habit) },
            onNotesClick = { habit -> showNotes(habit) }
        )
        monthlyAdapter = HabitAdapter(
            onCompleteClick = { habit -> onHabitCompleted(habit) },
            onEditClick = { habit -> openEditHabit(habit) },
            onDeleteClick = { habit -> showDeleteConfirmation(habit) },
            onNotesClick = { habit -> showNotes(habit) }
        )
    }

    private fun setupRecyclers(view: View) {
        view.findViewById<RecyclerView>(R.id.recyclerCurrentTasks).apply { layoutManager = LinearLayoutManager(requireContext()); adapter = taskAdapter }
        view.findViewById<RecyclerView>(R.id.recyclerDailyHabits).apply { layoutManager = LinearLayoutManager(requireContext()); adapter = dailyAdapter }
        view.findViewById<RecyclerView>(R.id.recyclerWeeklyHabits).apply { layoutManager = LinearLayoutManager(requireContext()); adapter = weeklyAdapter }
        view.findViewById<RecyclerView>(R.id.recyclerMonthlyHabits).apply { layoutManager = LinearLayoutManager(requireContext()); adapter = monthlyAdapter }
    }

    private fun setupObservers(view: View) {
        val tvProgress = view.findViewById<TextView>(R.id.tvProgress)
        val tvFocusTitle = view.findViewById<TextView>(R.id.tvFocusTitle)
        val tvFocusInspiration = view.findViewById<TextView>(R.id.tvFocusInspiration)
        val tvFocusTimer = view.findViewById<TextView>(R.id.tvFocusTimer)
        val tvTasksTitle = view.findViewById<TextView>(R.id.tvTodayTasksTitle)
        val tvHabitsTitle = view.findViewById<TextView>(R.id.tvDailyHabitsTitle)
        val tvPoints = view.findViewById<TextView>(R.id.tvPointsCount)

        combinedData.addSource(db.todoDao().getAllTodos()) { tasks -> combinedData.value = Pair(tasks ?: emptyList(), combinedData.value?.second ?: emptyList()) }
        combinedData.addSource(db.habitDao().getAllHabits()) { habits -> combinedData.value = Pair(combinedData.value?.first ?: emptyList(), habits ?: emptyList()) }

        combinedData.observe(viewLifecycleOwner) { (tasks, habits) ->
            // Update Points and Animate (Life effect)
            val xp = AchievementEngine.getTotalXP(requireContext())
            val currentPointsStr = tvPoints.text.toString().replace(",", "")
            val currentPoints = if (currentPointsStr.isEmpty() || currentPointsStr == "pts") 0 else currentPointsStr.toIntOrNull() ?: 0
            
            if (xp > currentPoints) {
                tvPoints.text = String.format(Locale.getDefault(), "%,d", xp)
                animatePoints(tvPoints)
            } else {
                tvPoints.text = String.format(Locale.getDefault(), "%,d", xp)
            }

            val now = Calendar.getInstance()
            val startOfDay = now.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
            val endOfDay = now.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
            
            val todayTasks = tasks.filter { it.targetDate in startOfDay..endOfDay }
                .sortedWith(compareBy<TodoItem> { it.isCompleted }
                .thenBy { it.displayOrder }
                .thenBy { it.priority.ordinal }
                .thenBy { it.startTime ?: Long.MAX_VALUE })

            val dailyHabits = habits.filter { it.frequency == HabitFrequency.DAILY }
                .sortedWith(compareBy<Habit> { it.isCompletedToday }
                .thenBy { it.displayOrder }
                .thenBy { it.createdAt })
            
            taskAdapter.submitList(todayTasks)
            dailyAdapter.submitList(dailyHabits)
            weeklyAdapter.submitList(habits.filter { it.frequency == HabitFrequency.WEEKLY })
            monthlyAdapter.submitList(habits.filter { it.frequency == HabitFrequency.MONTHLY })

            updateDashboard(todayTasks, dailyHabits, tvProgress, tvFocusTitle, tvFocusInspiration, tvFocusTimer, tvTasksTitle, tvHabitsTitle)
        }
    }

    private fun animatePoints(view: View) {
        view.animate()
            .scaleX(1.4f)
            .scaleY(1.4f)
            .setDuration(150)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<View>(R.id.layoutTodayTasksHeader).setOnClickListener { toggleList(view, R.id.recyclerCurrentTasks, R.id.ivTodayTasksArrow) }
        view.findViewById<View>(R.id.layoutDailyHabitsHeader).setOnClickListener { toggleList(view, R.id.recyclerDailyHabits, R.id.ivDailyHabitsArrow) }
        view.findViewById<View>(R.id.layoutWeeklyHabitsHeader).setOnClickListener { toggleList(view, R.id.recyclerWeeklyHabits, R.id.ivWeeklyHabitsArrow) }
        view.findViewById<View>(R.id.layoutMonthlyHabitsHeader).setOnClickListener { toggleList(view, R.id.recyclerMonthlyHabits, R.id.ivMonthlyHabitsArrow) }
        
        val btnNotify = view.findViewById<ImageButton>(R.id.btnNotifications)
        val settingsPrefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        
        fun updateNotifyIcon(isMuted: Boolean) {
            btnNotify?.setImageResource(if (isMuted) R.drawable.ic_notification_off else R.drawable.ic_notification)
        }
        
        updateNotifyIcon(settingsPrefs.getBoolean("mute_notifications", false))
        
        btnNotify?.setOnClickListener {
            val isMuted = settingsPrefs.getBoolean("mute_notifications", false)
            val nextMute = !isMuted
            settingsPrefs.edit().putBoolean("mute_notifications", nextMute).apply()
            if (nextMute) com.yourname.habitapp.utils.NotificationHelper.stopAllSounds(requireContext())
            updateNotifyIcon(nextMute)
            val msg = if (nextMute) "تم كتم التنبيهات 🔇" else "تم تفعيل التنبيهات 🔔"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFilters(view: View) {
        val chipGroup = view.findViewById<ChipGroup>(R.id.chipGroupFilter)
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            val isWeek = checkedIds.contains(R.id.chipWeek)
            view.findViewById<View>(R.id.layoutWeeklyHabitsHeader).visibility = if (isWeek) View.VISIBLE else View.GONE
            view.findViewById<View>(R.id.layoutMonthlyHabitsHeader).visibility = if (isWeek) View.GONE else View.VISIBLE
        }
    }

    private fun toggleList(view: View, recyclerId: Int, arrowId: Int) {
        val recycler = view.findViewById<RecyclerView>(recyclerId)
        val arrow = view.findViewById<ImageView>(arrowId)
        val isVisible = recycler.visibility == View.VISIBLE
        recycler.visibility = if (isVisible) View.GONE else View.VISIBLE
        arrow.animate().rotation(if (isVisible) 180f else 0f).setDuration(200).start()
    }

    private fun updateDashboard(tasks: List<TodoItem>, habits: List<Habit>, tvProgress: TextView, tvTitle: TextView, tvInspo: TextView, timer: TextView, tvTTitle: TextView, tvHTitle: TextView) {
        val completedTasks = tasks.count { it.isCompleted }
        tvTTitle.text = getString(R.string.today_tasks_label) + " $completedTasks/${tasks.size}"
        tvHTitle.text = getString(R.string.daily_habits_label)
        tvProgress.text = "$completedTasks / ${tasks.size} " + getString(R.string.achieved)

        val pendingTasks = tasks.filter { !it.isCompleted }

        if (tasks.isEmpty()) {
            tvInspo.text = getString(R.string.start_timer); tvTitle.text = getString(R.string.add_first_task); timer.text = "--:--:--"; countDownTimer?.cancel()
        } else if (pendingTasks.isNotEmpty()) {
            val now = System.currentTimeMillis()
            
            val activeTask = pendingTasks.firstOrNull { 
                val startTime = it.startTime ?: return@firstOrNull false
                val effectiveEndTime = it.endTime ?: (startTime + (it.durationMinutes.coerceAtLeast(1) * 60 * 1000L))
                now >= startTime && now < effectiveEndTime
            }
            
            val soonTask = pendingTasks.filter { it.startTime != null && it.startTime!! > now }
                .minByOrNull { it.startTime!! }
                
            val manualFirstTask = pendingTasks.minByOrNull { it.displayOrder }

            val focusTask = activeTask ?: soonTask ?: manualFirstTask!!
            
            tvTitle.text = focusTask.title
            if (focusTask.startTime != null) {
                val effectiveEndTime = focusTask.endTime ?: (focusTask.startTime!! + (focusTask.durationMinutes * 60 * 1000L))
                
                if (now < focusTask.startTime!!) {
                    tvInspo.text = getString(R.string.focus_next_goal)
                    timer.text = "--:--:--"; countDownTimer?.cancel()
                } else if (now < effectiveEndTime) {
                    tvInspo.text = getString(R.string.focus_in_progress)
                    startFocusTimer(timer, effectiveEndTime)
                } else {
                    tvInspo.text = getString(R.string.focus_time_up)
                    timer.text = getString(R.string.focus_time_finished); countDownTimer?.cancel()
                }
            } else { 
                tvInspo.text = getString(R.string.focus_next_goal)
                timer.text = getString(R.string.focus_all_day); countDownTimer?.cancel() 
            }
        } else {
            tvInspo.text = getString(R.string.focus_hero); tvTitle.text = getString(R.string.focus_completed_all); timer.text = "00:00:00"; countDownTimer?.cancel()
        }
    }

    private fun startFocusTimer(timer: TextView, targetTime: Long) {
        countDownTimer?.cancel()
        val duration = targetTime - System.currentTimeMillis()
        if (duration <= 0) { 
            timer.text = "00:00:00"
            return 
        }
        
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(ms: Long) {
                val h = ms / 3600000
                val m = (ms % 3600000) / 60000
                val s = (ms % 60000) / 1000
                timer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
            }
            override fun onFinish() { 
                timer.text = "00:00:00" 
                // Refresh dashboard to show next task by forcing a refresh of the current data
                combinedData.value = combinedData.value
            }
        }.start()
    }

    private fun setupFlashing(view: View) {
        flashRunnable = object : Runnable {
            override fun run() {
                val anim = AlphaAnimation(1.0f, 0.4f); anim.duration = 600; anim.repeatMode = Animation.REVERSE; anim.repeatCount = 3
                view.startAnimation(anim); flashHandler.postDelayed(this, 10 * 60 * 1000)
            }
        }; flashHandler.post(flashRunnable!!)
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

    private fun showDeleteConfirmation(item: Any) {
        val title = if (item is TodoItem) getString(R.string.delete_task_title) else getString(R.string.delete_habit_title)
        val message = if (item is TodoItem) getString(R.string.delete_task_msg) else getString(R.string.delete_habit_msg)
        
        AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.delete_confirm_title)) { _, _ ->
                lifecycleScope.launch {
                    if (item is TodoItem) db.todoDao().delete(item)
                    else if (item is Habit) db.habitDao().deleteHabit(item)
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun setupSwipeDelete(recyclerView: RecyclerView, adapter: ListAdapter<out Any, *>) {
        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT) {
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
                        if (list.all { it is TodoItem }) {
                            val updated = list.filterIsInstance<TodoItem>().mapIndexed { index, item -> item.copy(displayOrder = index) }
                            db.todoDao().updateAll(updated)
                        } else if (list.all { it is Habit }) {
                            val updated = list.filterIsInstance<Habit>().mapIndexed { index, item -> item.copy(displayOrder = index) }
                            db.habitDao().updateAllHabits(updated)
                        }
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

                val list = adapter.currentList.toMutableList() as MutableList<Any>
                Collections.swap(list, from, to)
                
                when (adapter) {
                    is TodoAdapter -> adapter.submitList(list.filterIsInstance<TodoItem>())
                    is HabitAdapter -> adapter.submitList(list.filterIsInstance<Habit>())
                    else -> (adapter as? ListAdapter<Any, *>)?.submitList(list)
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]
                adapter.notifyItemChanged(position)
                showDeleteConfirmation(item)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun toggleTodoMute(todo: TodoItem) {
        lifecycleScope.launch {
            if (!todo.reminderStart && !todo.reminderEnd) {
                Toast.makeText(requireContext(), "التنبيه غير مفعل لهذه المهمة", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val nextMuted = !todo.isMuted
            db.todoDao().update(todo.copy(isMuted = nextMuted))
            if (nextMuted) com.yourname.habitapp.utils.NotificationHelper.stopAllSounds(requireContext(), todo.id)
            val msg = if (nextMuted) "تم كتم المهمة 🔇" else "تنبيهات المهمة مفعلة 🔔"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleHabitMute(habit: Habit) {
        lifecycleScope.launch {
            val nextMuted = !habit.isMuted
            db.habitDao().updateHabit(habit.copy(isMuted = nextMuted))
            val msg = if (nextMuted) "تم كتم العادة 🔇" else "تنبيهات العادة مفعلة 🔔"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEditTodo(todo: TodoItem) { AddTodoBottomSheet.newInstance(todo.targetDate, todo.id).show(parentFragmentManager, "EditTodo") }
    private fun openEditHabit(habit: Habit) { AddHabitBottomSheet.newInstance(habit.id, habit.frequency, false).show(parentFragmentManager, "EditHabit") }
    private fun onTodoToggle(todo: TodoItem) {
        lifecycleScope.launch {
            db.todoDao().update(todo.copy(isCompleted = !todo.isCompleted))
            com.yourname.habitapp.utils.NotificationHelper.cancelNotification(requireContext(), todo.id * 10 + 1)
            com.yourname.habitapp.utils.NotificationHelper.cancelNotification(requireContext(), todo.id * 10 + 2)
            com.yourname.habitapp.utils.NotificationHelper.cancelNotification(requireContext(), todo.id * 10 + 3)
        }
    }

    private fun onHabitCompleted(habit: Habit) {
        lifecycleScope.launch {
            val newC = !habit.isCompletedToday
            val newS = if (newC) habit.streak + 1 else Math.max(0, habit.streak - 1)
            val timestamp = if (newC) System.currentTimeMillis() else null
            db.habitDao().updateHabitStreak(habit.id, newC, newS, Math.max(habit.longestStreak, newS), timestamp)
            if (newC) {
                com.yourname.habitapp.utils.NotificationHelper.cancelNotification(requireContext(), habit.name.hashCode())
            }
        }
    }
    private fun openAddHabit(freq: HabitFrequency?) { AddHabitBottomSheet.newInstance(-1, freq, false).show(parentFragmentManager, "AddHabit") }
    override fun onDestroyView() { super.onDestroyView(); countDownTimer?.cancel(); flashRunnable?.let { flashHandler.removeCallbacks(it) } }
}