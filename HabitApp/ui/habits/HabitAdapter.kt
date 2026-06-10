package com.yourname.habitapp.ui.habits

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
import com.yourname.habitapp.data.models.Habit

class HabitAdapter(
    private val onCompleteClick: (Habit) -> Unit,
    private val onLongClick: (Habit) -> Unit
) : ListAdapter<Habit, HabitAdapter.ViewHolder>(DiffCallback()) {

    // ─── ViewHolder ──────────────────────────────────────────────────────────
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon    : TextView = view.findViewById(R.id.tvHabitIcon)
        val tvName    : TextView = view.findViewById(R.id.tvHabitName)
        val tvStreak  : TextView = view.findViewById(R.id.tvStreak)
        val tvFreq    : TextView = view.findViewById(R.id.tvFrequency)
        val checkDone : CheckBox = view.findViewById(R.id.checkHabitDone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habit, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val habit = getItem(position)

        holder.tvIcon.text   = habit.icon
        holder.tvName.text   = habit.name
        holder.tvStreak.text = if (habit.streak > 0) "🔥 ${habit.streak}" else "—"
        holder.tvFreq.text   = when (habit.frequency.name) {
            "DAILY"   -> "يومي"
            "WEEKLY"  -> "أسبوعي"
            "MONTHLY" -> "شهري"
            else      -> ""
        }

        // تحديث CheckBox بدون إطلاق الـ listener
        holder.checkDone.setOnCheckedChangeListener(null)
        holder.checkDone.isChecked = habit.isCompletedToday

        // تأثير الشطب عند الإتمام
        holder.tvName.paintFlags = if (habit.isCompletedToday)
            holder.tvName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else
            holder.tvName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        holder.checkDone.setOnCheckedChangeListener { _, _ ->
            onCompleteClick(habit)
        }

        holder.itemView.setOnLongClickListener {
            onLongClick(habit)
            true
        }
    }

    // ─── DiffCallback ────────────────────────────────────────────────────────
    // يقارن العناصر ويُحدّث فقط المتغيرة → أداء أفضل
    class DiffCallback : DiffUtil.ItemCallback<Habit>() {
        override fun areItemsTheSame(a: Habit, b: Habit) = a.id == b.id
        override fun areContentsTheSame(a: Habit, b: Habit) = a == b
    }
}
