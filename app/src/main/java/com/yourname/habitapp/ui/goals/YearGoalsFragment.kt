package com.yourname.habitapp.ui.goals

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.GoalStep
import com.yourname.habitapp.data.models.YearGoal
import com.yourname.habitapp.utils.AchievementEngine
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Collections

class YearGoalsFragment : Fragment() {

    private lateinit var adapter: YearGoalAdapter
    private lateinit var db: AppDatabase
    private val years = mutableListOf<Int>()
    private var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_year_goals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getInstance(requireContext())

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerGoals)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = YearGoalAdapter(
            context = requireContext(),
            onGoalClick = { goal -> showGoalDetails(goal) },
            onEdit = { goal -> editGoal(goal) },
            onDelete = { goal -> confirmDelete(goal) },
            onToggleComplete = { goal -> toggleGoalComplete(goal) }
        )
        recyclerView.adapter = adapter

        // Setup Drag & Drop and Swipe for Goals
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
                            it.foreground = android.graphics.drawable.ColorDrawable(0x4D000000)
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
                        val updatedList = list.mapIndexed { index, item ->
                            item.copy(displayOrder = index)
                        }
                        db.yearGoalDao().updateAllGoals(updatedList)
                        Toast.makeText(requireContext(), "تم تحديث الترتيب بنجاح ✅", Toast.LENGTH_SHORT).show()
                    }
                }
                fromPosition = -1
                toPosition = -1
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
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

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutGoals)
        setupYearTabs(tabLayout)

        applyThemeDecorations(view)

        observeGoals()
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

        val bgDecorations = view.findViewById<TextView>(R.id.tvBgDecorationsGoals)
        if (bgDecorations != null && emojis.isNotEmpty()) {
            val repeated = (1..100).joinToString(" ") { emojis }
            bgDecorations.text = repeated
        } else {
            bgDecorations?.text = ""
        }
    }

    private fun setupYearTabs(tabLayout: TabLayout) {
        tabLayout.removeAllTabs()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        years.clear()
        for (i in -2..3) {
            val year = currentYear + i
            years.add(year)
            val tab = tabLayout.newTab().setText(year.toString())
            tabLayout.addTab(tab)
            if (year == currentYear) tab.select()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedYear = years[tab?.position ?: 0]
                observeGoals()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeGoals() {
        db.yearGoalDao().getGoalsByYear(selectedYear).observe(viewLifecycleOwner) { goals ->
            adapter.submitList(goals)
            updateHeader(goals)
        }
    }

    private fun updateHeader(goals: List<YearGoal>) {
        val achieved = goals.count { it.isCompleted }
        val total = goals.size
        
        val achievedPercent = if (total > 0) (achieved * 100 / total) else 0

        view?.findViewById<TextView>(R.id.tvAchievedPercent)?.text = "$achievedPercent%"

        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        
        if (selectedYear < currentYear) {
            view?.findViewById<TextView>(R.id.tvDaysLeft)?.text = getString(R.string.focus_time_finished)
            view?.findViewById<TextView>(R.id.tvDaysLeft)?.setTextColor(0xFF888888.toInt())
        } else {
            val endOfYear = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, Calendar.DECEMBER)
                set(Calendar.DAY_OF_MONTH, 31)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
            }
            val diff = endOfYear.timeInMillis - now.timeInMillis
            val daysLeft = if (diff > 0) (diff / (1000 * 60 * 60 * 24)) else 0
            view?.findViewById<TextView>(R.id.tvDaysLeft)?.text = daysLeft.toString()
            view?.findViewById<TextView>(R.id.tvDaysLeft)?.setTextColor(0xFF2196F3.toInt())
        }
    }

    private fun showGoalDetails(goal: YearGoal) {
        lifecycleScope.launch {
            val steps = db.yearGoalDao().getStepsForGoalSync(goal.id)
            val stepsText = if (steps.isNotEmpty()) {
                steps.joinToString("\n") { step -> 
                    val status = if (step.isCompleted) "✅" else "⬜"
                    "$status ${step.title}"
                }
            } else {
                getString(R.string.no_steps_yet)
            }

            AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
                .setTitle(goal.title)
                .setMessage(getString(R.string.goal_progress_msg).format(goal.progress, stepsText))
                .setPositiveButton(getString(R.string.close), null)
                .show()
        }
    }

    private fun editGoal(goal: YearGoal) {
        AddGoalBottomSheet.newInstance(goalId = goal.id)
            .show(parentFragmentManager, "EditGoal")
    }

    private fun toggleGoalComplete(goal: YearGoal) {
        lifecycleScope.launch {
            val isComp = !goal.isCompleted
            db.yearGoalDao().updateGoal(goal.copy(
                isCompleted = isComp,
                progress = if (isComp) 100 else goal.progress
            ))
            if (isComp) {
                AchievementEngine.addXP(requireContext(), 100)
                val count = db.yearGoalDao().getCompletedGoalsCount()
                AchievementEngine.checkAndUnlock(requireContext(), "GOAL_COMPLETED", count)
            }
        }
    }

    private fun confirmDelete(goal: YearGoal) {
        AlertDialog.Builder(requireContext(), R.style.PurpleAlertDialog)
            .setTitle(getString(R.string.delete_goal))
            .setMessage(getString(R.string.delete_goal_msg))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                lifecycleScope.launch {
                    db.yearGoalDao().deleteGoal(goal)
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }
}
