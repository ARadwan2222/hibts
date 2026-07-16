package com.yourname.habitapp.ui.todo

import android.graphics.Paint
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
import com.yourname.habitapp.data.models.TodoItem
import java.text.SimpleDateFormat
import java.util.*

class MyTasksAdapter(
    private val onTodoToggle: (TodoItem) -> Unit,
    private val onHabitToggle: (Habit) -> Unit,
    private val onEdit: (Any) -> Unit,
    private val onDelete: (Any) -> Unit,
    private val onMuteToggle: (Any) -> Unit,
    private val onNotesClick: (Any) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    private fun formatTime(millis: Long): String {
        return timeFormatter.format(Date(millis))
    }

    companion object {
        private const val TYPE_TASK = 1
        private const val TYPE_HABIT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is TodoItem) TYPE_TASK else TYPE_HABIT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == TYPE_TASK) R.layout.item_todo else R.layout.item_habit
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return if (viewType == TYPE_TASK) TaskViewHolder(view) else HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is TaskViewHolder && item is TodoItem) holder.bind(item)
        else if (holder is HabitViewHolder && item is Habit) holder.bind(item)
    }

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.tvTodoTitle)
        private val check = view.findViewById<CheckBox>(R.id.checkTodoDone)
        private val btnEdit = view.findViewById<ImageView>(R.id.btnEdit)
        private val btnDelete = view.findViewById<ImageView>(R.id.btnDelete)
        private val ivBell = view.findViewById<ImageView>(R.id.ivBell)
        private val ivNotes = view.findViewById<ImageView>(R.id.ivNotes)

        fun bind(todo: TodoItem) {
            title.text = todo.title
            val timeView = itemView.findViewById<TextView>(R.id.tvTodoTime)
            
            ivNotes.visibility = if (todo.notes?.isNotEmpty() == true) View.VISIBLE else View.GONE
            ivNotes.setOnClickListener { onNotesClick(todo) }

            // Bell logic: Lit ONLY if a reminder is active and NOT muted
            val reminderOn = todo.reminderStart || todo.reminderEnd
            val showLit = reminderOn && !todo.isMuted && !todo.isCompleted
            
            ivBell.visibility = View.VISIBLE
            ivBell.setImageResource(if (showLit) R.drawable.ic_notification else R.drawable.ic_notification_off)
            ivBell.imageTintList = android.content.res.ColorStateList.valueOf(if (showLit) 0xFFFFD600.toInt() else 0xFFBDBDBD.toInt())
            ivBell.setOnClickListener { onMuteToggle(todo) }

            if (todo.isMissed) {
                title.setTextColor(0xFFD32F2F.toInt())
                timeView.text = "⚠️ فات وقتها"
                timeView.visibility = View.VISIBLE
            } else {
                title.setTextColor(0xFF333333.toInt())
                if (todo.startTime != null) {
                    val start = formatTime(todo.startTime)
                    val end = todo.endTime?.let { " - ${formatTime(it)}" } ?: ""
                    timeView.text = "$start$end"
                    timeView.visibility = View.VISIBLE
                } else {
                    timeView.visibility = View.GONE
                }
            }

            check.visibility = View.VISIBLE
            check.setOnCheckedChangeListener(null)
            check.isChecked = todo.isCompleted
            check.setOnCheckedChangeListener { _, _ -> onTodoToggle(todo) }
            
            title.paintFlags = if (todo.isCompleted) title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG 
                               else title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            itemView.alpha = if (todo.isCompleted) 0.6f else 1.0f
            
            btnEdit.setOnClickListener { onEdit(todo) }
            btnDelete.setOnClickListener { onDelete(todo) }
        }
    }

    inner class HabitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.tvHabitName)
        private val icon = view.findViewById<TextView>(R.id.tvHabitIcon)
        private val check = view.findViewById<CheckBox>(R.id.checkHabitDone)
        private val btnEdit = view.findViewById<ImageView>(R.id.btnEdit)
        private val btnDelete = view.findViewById<ImageView>(R.id.btnDelete)
        private val ivNotes = view.findViewById<ImageView>(R.id.ivNotes)

        fun bind(habit: Habit) {
            title.text = habit.name
            icon.text = habit.icon

            ivNotes.visibility = if (habit.notes.isNotEmpty()) View.VISIBLE else View.GONE
            ivNotes.setOnClickListener { onNotesClick(habit) }

            check.visibility = View.VISIBLE
            check.setOnCheckedChangeListener(null)
            check.isChecked = habit.isCompletedToday
            check.setOnCheckedChangeListener { _, _ -> onHabitToggle(habit) }
            
            title.paintFlags = if (habit.isCompletedToday) title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG 
                               else title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            itemView.alpha = if (habit.isCompletedToday) 0.6f else 1.0f
            
            btnEdit.setOnClickListener { onEdit(habit) }
            btnDelete.setOnClickListener { onDelete(habit) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(old: Any, new: Any): Boolean {
            return if (old is TodoItem && new is TodoItem) old.id == new.id
            else if (old is Habit && new is Habit) old.id == new.id
            else false
        }
        override fun areContentsTheSame(old: Any, new: Any): Boolean = old == new
    }
}
