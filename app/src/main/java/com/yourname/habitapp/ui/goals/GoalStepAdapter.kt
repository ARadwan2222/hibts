package com.yourname.habitapp.ui.goals

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourname.habitapp.R
import com.yourname.habitapp.data.models.GoalStep

class GoalStepAdapter(
    private val onStepToggle: (GoalStep) -> Unit
) : ListAdapter<GoalStep, GoalStepAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val check: CheckBox = view.findViewById(R.id.checkStep)
        val title: TextView = view.findViewById(R.id.tvStepTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal_step, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val step = getItem(position)
        holder.title.text = step.title
        
        holder.check.setOnCheckedChangeListener(null)
        holder.check.isChecked = step.isCompleted
        
        holder.title.paintFlags = if (step.isCompleted) holder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG 
                                  else holder.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        holder.check.setOnCheckedChangeListener { _, _ -> onStepToggle(step) }
    }

    class DiffCallback : DiffUtil.ItemCallback<GoalStep>() {
        override fun areItemsTheSame(old: GoalStep, new: GoalStep) = old.id == new.id
        override fun areContentsTheSame(old: GoalStep, new: GoalStep) = old == new
    }
}
