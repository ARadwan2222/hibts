package com.yourname.habitapp.ui.goals

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.YearGoal
import com.yourname.habitapp.utils.AchievementEngine
import kotlinx.coroutines.launch
import java.util.*

class YearGoalAdapter(
    private val context: android.content.Context,
    private val onGoalClick: (YearGoal) -> Unit,
    private val onEdit: (YearGoal) -> Unit,
    private val onDelete: (YearGoal) -> Unit,
    private val onToggleComplete: (YearGoal) -> Unit
) : ListAdapter<YearGoal, YearGoalAdapter.ViewHolder>(DiffCallback()) {

    private var expandedGoalId: Int? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container    : View         = view.findViewById(R.id.layoutGoalContainer)
        val tvIcon       : TextView     = view.findViewById(R.id.tvGoalIcon)
        val tvTitle      : TextView     = view.findViewById(R.id.tvGoalTitle)
        val tvCountdown  : TextView     = view.findViewById(R.id.tvGoalCountdown)
        val progressBar  : ProgressBar  = view.findViewById(R.id.progressBarGoal)
        val checkGoal    : CheckBox     = view.findViewById(R.id.checkGoal)
        val btnEdit      : View         = view.findViewById(R.id.btnEditGoal)
        val btnDelete    : View         = view.findViewById(R.id.btnDeleteGoalAction)
        val stepsLayout  : View         = view.findViewById(R.id.layoutStepsContainer)
        val recyclerSteps: RecyclerView = view.findViewById(R.id.recyclerSteps)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_year_goal, parent, false)
        // Ensure full width by removing horizontal margins in code if layout doesn't suffice
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val goal = getItem(position)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val now = System.currentTimeMillis()

        holder.tvIcon.text = "🎯"
        holder.tvTitle.text = goal.title
        
        holder.container.setBackgroundColor(0) // Reset background
        
        // Progress Bar Logic
        holder.progressBar.progress = goal.progress
        holder.progressBar.visibility = if (goal.progress > 0) View.VISIBLE else View.GONE
        if (goal.progress >= 100) {
            holder.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(0xFF1B5E20.toInt()) // Dark Green
        } else {
            holder.progressBar.progressTintList = android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt()) // Regular Green
        }

        // Background logic: Show progress bar only if there are steps
        val db = AppDatabase.getInstance(holder.itemView.context)

        // Dynamic Background Color Logic with Translucency
        when {
            goal.isCompleted -> {
                holder.container.setBackgroundColor(0x204CAF50) // More transparent Green
            }
            getTargetMillis(goal) < now -> {
                holder.container.setBackgroundColor(0x20F44336) // More transparent Red
            }
            else -> {
                // Keep default or set to surface
            }
        }

        // Checkbox logic - Essential for completing goals
        holder.checkGoal.setOnCheckedChangeListener(null)
        holder.checkGoal.isChecked = goal.isCompleted
        holder.checkGoal.visibility = if (goal.year >= currentYear) View.VISIBLE else View.GONE
        
        holder.itemView.alpha = if (goal.year < currentYear) 0.8f else 1.0f

        // Countdown Logic
        val targetMillis = getTargetMillis(goal)
        val diff = targetMillis - now
        if (diff > 0) {
            val days = diff / (1000 * 60 * 60 * 24)
            holder.tvCountdown.text = context.getString(R.string.days_left_msg).format(days)
            
            // Resolve primary color from theme
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)
            val primaryColor = typedValue.data
            
            holder.tvCountdown.setTextColor(if (days < 7) 0xFFD32F2F.toInt() else primaryColor)
        } else {
            holder.tvCountdown.text = context.getString(R.string.time_up_warning)
            holder.tvCountdown.setTextColor(0xFFD32F2F.toInt())
        }

        // Expand/Collapse logic for steps
        val isExpanded = expandedGoalId == goal.id
        holder.stepsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        
        if (isExpanded) {
            holder.recyclerSteps.layoutManager = LinearLayoutManager(holder.itemView.context)
            val stepAdapter = GoalStepAdapter { step ->
                (context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch {
                    db.yearGoalDao().updateStep(step.copy(isCompleted = !step.isCompleted))
                    db.yearGoalDao().recalculateProgress(goal.id)
                    AchievementEngine.addXP(context, 10)
                }
            }
            holder.recyclerSteps.adapter = stepAdapter
            (context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch {
                val steps = db.yearGoalDao().getStepsForGoalSync(goal.id)
                stepAdapter.submitList(steps)
            }
        }

        val toggleExpand = View.OnClickListener {
            expandedGoalId = if (isExpanded) null else goal.id
            notifyItemChanged(position)
        }
        
        holder.container.setOnClickListener(toggleExpand)
        holder.tvTitle.setOnClickListener(toggleExpand)
        holder.tvCountdown.setOnClickListener(toggleExpand)

        holder.checkGoal.setOnCheckedChangeListener { _, _ -> onToggleComplete(goal) }
        holder.btnEdit.setOnClickListener { onEdit(goal) }
        holder.btnDelete.setOnClickListener { onDelete(goal) }
        holder.itemView.setOnClickListener { onGoalClick(goal) }
    }

    private fun getTargetMillis(goal: YearGoal): Long {
        return when {
            goal.targetDate != null -> goal.targetDate
            goal.quarter != null -> {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, goal.year)
                    when (goal.quarter) {
                        1 -> { set(Calendar.MONTH, 2); set(Calendar.DAY_OF_MONTH, 31) }
                        2 -> { set(Calendar.MONTH, 5); set(Calendar.DAY_OF_MONTH, 30) }
                        3 -> { set(Calendar.MONTH, 8); set(Calendar.DAY_OF_MONTH, 30) }
                        4 -> { set(Calendar.MONTH, 11); set(Calendar.DAY_OF_MONTH, 31) }
                    }
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                }.timeInMillis
            }
            else -> {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, goal.year); set(Calendar.MONTH, 11); set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                }.timeInMillis
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<YearGoal>() {
        override fun areItemsTheSame(a: YearGoal, b: YearGoal) = a.id == b.id
        override fun areContentsTheSame(a: YearGoal, b: YearGoal) = a == b
    }
}
