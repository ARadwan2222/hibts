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
import android.view.animation.ScaleAnimation
import com.yourname.habitapp.utils.AchievementEngine
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.Habit
import com.yourname.habitapp.data.models.HabitFrequency
import com.yourname.habitapp.data.models.TodoItem
import com.yourname.habitapp.ui.todo.AddTodoBottomSheet
import com.yourname.habitapp.ui.todo.TodoAdapter
import kotlinx.coroutines.launch
import java.util.*

class HabitsFragment : Fragment() {

    private lateinit var db: AppDatabase
    private var countDownTimer: CountDownTimer? = null
    private val flashHandler = Handler(Looper.getMainLooper())
    private var flashRunnable: Runnable? = null

    private lateinit var taskAdapter: TodoAdapter
    private lateinit var dailyAdapter: HabitAdapter
    private lateinit var weeklyAdapter: HabitAdapter
    private lateinit var monthlyAdapter: HabitAdapter

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
            onMuteToggle = { todo -> toggleTodoMute(todo) }
        )
        dailyAdapter = HabitAdapter(
            onCompleteClick = { habit -> onHabitCompleted(habit) },
            onEditClick = { habit -> openEditHabit(habit) },
            onDeleteClick = { habit -> showDeleteConfirmation(habit) },
            onMuteToggle = { habit -> toggleHabitMute(habit) }
        )
        weeklyAdapter = HabitAdapter(
            onCompleteClick = { habit -> onHabitCompleted(habit) },
            onEditClick = { habit -> openEditHabit(habit) },
            onDeleteClick = { habit -> showDeleteConfirmation(habit) },
            onMuteToggle = { habit -> toggleHabitMute(habit) }
        )
        monthlyAdapter = HabitAdapter(
            onCompleteClick = { habit -> onHabitCompleted(habit) },
            onEditClick = { habit -> openEditHabit(habit) },
            onDeleteClick = { habit -> showDeleteConfirmation(habit) },
            onMuteToggle = { habit -> toggleHabitMute(habit) }
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

        val combinedData = MediatorLiveData<Pair<List<TodoItem>, List<Habit>>>()
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
            val dailyHabits = habits.filter { it.frequency == HabitFrequency.DAILY }
            
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
        view.findViewById<View>(R.id.btnAddTaskPlus).setOnClickListener { AddTodoBottomSheet().show(parentFragmentManager, "AddTodo") }
        view.findViewById<View>(R.id.btnAddHabitPlus).setOnClickListener { openAddHabit(HabitFrequency.DAILY) }
        view.findViewById<View>(R.id.btnAddWeeklyPlus).setOnClickListener { openAddHabit(HabitFrequency.WEEKLY) }
        view.findViewById<View>(R.id.btnAddMonthlyPlus).setOnClickListener { openAddHabit(HabitFrequency.MONTHLY) }
        view.findViewById<View>(R.id.btnNotifications).setOnClickListener {
            Toast.makeText(requireContext(), "تم إرسال التنبيهات بنجاح ✅", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), com.yourname.habitapp.ui.achievements.AchievementsActivity::class.java))
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
        val pendingHabits = habits.filter { !it.isCompletedToday }

        if (tasks.isEmpty()) {
            tvInspo.text = "★ " + getString(R.string.start_timer) + " ★"; tvTitle.text = getString(R.string.add_first_task); timer.text = "--:--:--"; countDownTimer?.cancel()
        } else if (pendingTasks.isNotEmpty()) {
            val focusTask = pendingTasks.sortedWith(compareBy({ it.priority.ordinal }, { it.startTime ?: Long.MAX_VALUE })).first()
            tvTitle.text = focusTask.title
            if (focusTask.startTime != null) {
                val now = System.currentTimeMillis()
                val effectiveEndTime = focusTask.endTime ?: (focusTask.startTime + (focusTask.durationMinutes * 60 * 1000L))
                
                if (now < focusTask.startTime) {
                    tvInspo.text = "✨ " + getString(R.string.focus_next_goal) + " ✨"
                    // Changed logic: Timer ONLY works at the start of the task, not when added
                    timer.text = "--:--:--"; countDownTimer?.cancel()
                } else if (now < effectiveEndTime) {
                    tvInspo.text = "🔥 " + getString(R.string.focus_in_progress) + " 🔥"
                    startFocusTimer(timer, effectiveEndTime)
                } else {
                    tvInspo.text = "🎯 " + getString(R.string.focus_time_up) + " 🎯"
                    timer.text = getString(R.string.focus_time_finished); countDownTimer?.cancel()
                }
            } else { 
                tvInspo.text = "✨ " + getString(R.string.focus_next_goal) + " ✨"
                timer.text = getString(R.string.focus_all_day); countDownTimer?.cancel() 
            }
        } else {
            tvInspo.text = getString(R.string.focus_hero); tvTitle.text = getString(R.string.focus_completed_all); timer.text = "00:00:00"; countDownTimer?.cancel()
        }
    }

    private fun startFocusTimer(timer: TextView, targetTime: Long) {
        countDownTimer?.cancel()
        val duration = targetTime - System.currentTimeMillis()
        if (duration <= 0) { timer.text = "00:00:00"; return }
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(ms: Long) {
                val h = ms / 3600000; val m = (ms % 3600000) / 60000; val s = (ms % 60000) / 1000
                timer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
            }
            override fun onFinish() { timer.text = "00:00:00" }
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
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]
                adapter.notifyItemChanged(position)
                showDeleteConfirmation(item)
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun toggleTodoMute(todo: TodoItem) {
        lifecycleScope.launch {
            val nextMuted = !todo.isMuted
            db.todoDao().update(todo.copy(isMuted = nextMuted))
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