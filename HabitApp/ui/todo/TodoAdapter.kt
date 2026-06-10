package com.yourname.habitapp.ui.todo

import android.graphics.Color
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
import com.yourname.habitapp.data.models.Priority
import com.yourname.habitapp.data.models.TodoItem
import java.text.SimpleDateFormat
import java.util.*

class TodoAdapter(
    private val onCompleteClick: (TodoItem) -> Unit
) : ListAdapter<TodoItem, TodoAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle      : TextView = view.findViewById(R.id.tvTodoTitle)
        val tvTime       : TextView = view.findViewById(R.id.tvTodoTime)
        val tvPriority   : TextView = view.findViewById(R.id.tvPriority)
        val checkDone    : CheckBox = view.findViewById(R.id.checkTodoDone)
        val priorityBar  : View     = view.findViewById(R.id.viewPriorityBar)
    }

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val todo = getItem(position)

        holder.tvTitle.text = todo.title

        // عرض الوقت
        if (todo.startTime != null) {
            val start = timeFormat.format(Date(todo.startTime))
            val end   = if (todo.endTime != null) " - ${timeFormat.format(Date(todo.endTime))}" else ""
            holder.tvTime.text = "⏰ $start$end"
            holder.tvTime.visibility = View.VISIBLE
        } else {
            holder.tvTime.visibility = View.GONE
        }

        // الأولوية
        val (priorityText, priorityColor) = when (todo.priority) {
            Priority.HIGH   -> Pair("عالية 🔴", Color.parseColor("#FF4444"))
            Priority.MEDIUM -> Pair("متوسطة 🟡", Color.parseColor("#FFAA00"))
            Priority.LOW    -> Pair("منخفضة 🟢", Color.parseColor("#44BB44"))
        }
        holder.tvPriority.text = priorityText
        holder.priorityBar.setBackgroundColor(priorityColor)

        // حالة الإتمام
        holder.checkDone.setOnCheckedChangeListener(null)
        holder.checkDone.isChecked = todo.isCompleted
        holder.tvTitle.paintFlags = if (todo.isCompleted)
            holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else
            holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        holder.itemView.alpha = if (todo.isCompleted) 0.5f else 1.0f

        holder.checkDone.setOnCheckedChangeListener { _, _ -> onCompleteClick(todo) }
    }

    class DiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(a: TodoItem, b: TodoItem) = a.id == b.id
        override fun areContentsTheSame(a: TodoItem, b: TodoItem) = a == b
    }
}
