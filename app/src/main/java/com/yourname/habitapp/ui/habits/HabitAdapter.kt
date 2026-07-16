package com.yourname.habitapp.ui.habits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourname.habitapp.R
import com.yourname.habitapp.data.models.Habit
import com.yourname.habitapp.data.models.HabitFrequency

class HabitAdapter(
    private val onCompleteClick: (Habit) -> Unit,
    private val onEditClick: (Habit) -> Unit,
    private val onDeleteClick: (Habit) -> Unit,
    private val onNotesClick: (Habit) -> Unit
) : ListAdapter<Habit, HabitAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon    : TextView = view.findViewById(R.id.tvHabitIcon)
        val tvName    : TextView = view.findViewById(R.id.tvHabitName)
        val tvStreak  : TextView = view.findViewById(R.id.tvStreak)
        val tvFreq    : TextView = view.findViewById(R.id.tvFrequency)
        val checkDone : CheckBox = view.findViewById(R.id.checkHabitDone)
        val btnEdit   : View = view.findViewById(R.id.btnEdit)
        val btnDelete : View = view.findViewById(R.id.btnDelete)
        val indicator : View = view.findViewById(R.id.viewPriorityIndicator)
        val ivNotes   : View = view.findViewById(R.id.ivNotes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val habit = getItem(position)
        holder.tvIcon.text   = habit.icon
        holder.tvName.text   = habit.name
        holder.tvStreak.text = if (habit.streak > 0) "🔥 ${habit.streak}" else ""
        holder.tvFreq.text   = getFrequencyText(habit)
        
        holder.indicator.setBackgroundColor(0xFFFFD166.toInt()) // Soft Yellow

        holder.ivNotes.visibility = if (habit.notes.isNotEmpty()) View.VISIBLE else View.GONE
        holder.ivNotes.setOnClickListener { onNotesClick(habit) }

        holder.checkDone.setOnCheckedChangeListener(null)
        holder.checkDone.isChecked = habit.isCompletedToday
        holder.checkDone.setOnCheckedChangeListener { _, _ -> onCompleteClick(habit) }
        
        holder.itemView.alpha = if (habit.isCompletedToday) 0.6f else 1.0f

        holder.btnEdit.setOnClickListener { onEditClick(habit) }
        holder.btnDelete.setOnClickListener { onDeleteClick(habit) }
    }

    private fun getFrequencyText(habit: Habit): String {
        return when (habit.frequency) {
            HabitFrequency.DAILY -> "Daily"
            HabitFrequency.WEEKLY -> "Weekly"
            HabitFrequency.MONTHLY -> "Monthly"
            HabitFrequency.YEARLY -> "Yearly"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Habit>() {
        override fun areItemsTheSame(a: Habit, b: Habit) = a.id == b.id
        override fun areContentsTheSame(a: Habit, b: Habit) = a == b
    }
}
