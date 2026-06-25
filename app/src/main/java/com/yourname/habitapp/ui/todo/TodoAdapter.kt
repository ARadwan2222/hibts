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
import com.yourname.habitapp.data.models.Priority
import com.yourname.habitapp.data.models.TodoItem
import java.text.SimpleDateFormat
import java.util.*

class TodoAdapter(
    private val onCompleteClick: (TodoItem) -> Unit,
    private val onEditClick: (TodoItem) -> Unit,
    private val onDeleteClick: (TodoItem) -> Unit,
    private val onMuteToggle: (TodoItem) -> Unit 
) : ListAdapter<TodoItem, TodoAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle      : TextView = view.findViewById(R.id.tvTodoTitle)
        val tvTime       : TextView = view.findViewById(R.id.tvTodoTime)
        val checkDone    : CheckBox = view.findViewById(R.id.checkTodoDone)
        val btnEdit      : View = view.findViewById(R.id.btnEdit)
        val btnDelete    : View = view.findViewById(R.id.btnDelete)
        val indicator    : View = view.findViewById(R.id.viewPriorityIndicator)
        val ivBell       : ImageView = view.findViewById(R.id.ivBell)
    }

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val todo = getItem(position)
        holder.tvTitle.text = todo.title

        val priorityColor = when(todo.priority) {
            Priority.HIGH -> 0xFFE84393.toInt() 
            Priority.MEDIUM -> 0xFF6C5CE7.toInt()
            Priority.LOW -> 0xFF00CEC9.toInt()
        }
        holder.indicator.setBackgroundColor(priorityColor)
        
        // Bell logic: Lit ONLY if a reminder is active and NOT muted
        val reminderOn = todo.reminderStart || todo.reminderEnd
        val showLit = reminderOn && !todo.isMuted && !todo.isCompleted
        
        holder.ivBell.visibility = View.VISIBLE
        holder.ivBell.setImageResource(if (showLit) R.drawable.ic_notification else R.drawable.ic_notification_off)
        holder.ivBell.imageTintList = android.content.res.ColorStateList.valueOf(if (showLit) 0xFFFFD600.toInt() else 0xFFBDBDBD.toInt())
        holder.ivBell.setOnClickListener { onMuteToggle(todo) }

        if (todo.isMissed) {
            holder.tvTitle.setTextColor(0xFFD32F2F.toInt())
            holder.tvTime.text = "⚠️ فات وقتها"
            holder.tvTime.visibility = View.VISIBLE
        } else if (todo.startTime != null) {
            val start = timeFormat.format(Date(todo.startTime))
            val end = todo.endTime?.let { " - ${timeFormat.format(Date(it))}" } ?: ""
            holder.tvTime.text = "⏰ $start$end"
            holder.tvTime.visibility = View.VISIBLE
            holder.tvTitle.setTextColor(0xFF333333.toInt())
        } else {
            holder.tvTime.visibility = View.GONE
            holder.tvTitle.setTextColor(0xFF333333.toInt())
        }

        holder.checkDone.setOnCheckedChangeListener(null)
        holder.checkDone.isChecked = todo.isCompleted
        
        holder.tvTitle.paintFlags = if (todo.isCompleted)
            holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else
            holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        holder.checkDone.setOnCheckedChangeListener { _, _ -> onCompleteClick(todo) }
        
        holder.btnEdit.setOnClickListener { onEditClick(todo) }
        holder.btnDelete.setOnClickListener { onDeleteClick(todo) }
    }

    class DiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(a: TodoItem, b: TodoItem) = a.id == b.id
        override fun areContentsTheSame(a: TodoItem, b: TodoItem) = a == b
    }
}
