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

class MyTasksAdapter(
    private val onTodoToggle: (TodoItem) -> Unit,
    private val onHabitToggle: (Habit) -> Unit,
    private val onEdit: (Any) -> Unit,
    private val onDelete: (Any) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(DiffCallback()) {

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

        fun bind(todo: TodoItem) {
            title.text = todo.title
            val timeView = itemView.findViewById<TextView>(R.id.tvTodoTime)
            
            if (todo.isMissed) {
                title.setTextColor(0xFFD32F2F.toInt())
                timeView.text = "⚠️ فات وقتها"
                timeView.visibility = View.VISIBLE
            } else {
                title.setTextColor(0xFF333333.toInt())
                timeView.visibility = if (todo.startTime != null) View.VISIBLE else View.GONE
            }

            check.visibility = View.VISIBLE
            check.setOnCheckedChangeListener(null)
            check.isChecked = todo.isCompleted
            check.setOnCheckedChangeListener { _, _ -> onTodoToggle(todo) }
            
            title.paintFlags = if (todo.isCompleted) title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG 
                               else title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            
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

        fun bind(habit: Habit) {
            title.text = habit.name
            icon.text = habit.icon
            check.visibility = View.VISIBLE
            check.setOnCheckedChangeListener(null)
            check.isChecked = habit.isCompletedToday
            check.setOnCheckedChangeListener { _, _ -> onHabitToggle(habit) }
            
            title.paintFlags = if (habit.isCompletedToday) title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG 
                               else title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            
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