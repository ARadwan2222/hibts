package com.yourname.habitapp.ui.goals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourname.habitapp.R
import com.yourname.habitapp.data.models.GoalStep
import com.yourname.habitapp.data.models.YearGoal

class YearGoalAdapter(
    private val onStepCompleted: (Int, GoalStep) -> Unit,
    private val onGoalClick: (YearGoal) -> Unit
) : ListAdapter<YearGoal, YearGoalAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon     : TextView    = view.findViewById(R.id.tvGoalIcon)
        val tvTitle    : TextView    = view.findViewById(R.id.tvGoalTitle)
        val tvProgress : TextView    = view.findViewById(R.id.tvGoalProgress)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBarGoal)
        val tvCompleted: TextView    = view.findViewById(R.id.tvGoalCompleted)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_year_goal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val goal = getItem(position)

        holder.tvIcon.text       = goal.icon
        holder.tvTitle.text      = goal.title
        holder.tvProgress.text   = "${goal.progress}%"
        holder.progressBar.progress = goal.progress
        holder.tvCompleted.visibility = if (goal.isCompleted) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onGoalClick(goal) }
    }

    class DiffCallback : DiffUtil.ItemCallback<YearGoal>() {
        override fun areItemsTheSame(a: YearGoal, b: YearGoal) = a.id == b.id
        override fun areContentsTheSame(a: YearGoal, b: YearGoal) = a == b
    }
}
