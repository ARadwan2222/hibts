package com.yourname.habitapp.ui.todo

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.yourname.habitapp.R
import java.text.SimpleDateFormat
import java.util.*

class DayStripAdapter(private val onDateSelected: (Calendar) -> Unit) : RecyclerView.Adapter<DayStripAdapter.ViewHolder>() {

    private val dates = mutableListOf<Calendar>()
    private var selectedPosition = 0

    init {
        val cal = Calendar.getInstance()
        // Load 30 days for future planning
        for (i in 0 until 30) {
            val d = cal.clone() as Calendar
            dates.add(d)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardDay)
        val tvName: TextView = view.findViewById(R.id.tvDayName)
        val tvNumber: TextView = view.findViewById(R.id.tvDayNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_day_strip, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = dates[position]
        val isSelected = position == selectedPosition

        val nameFormat = SimpleDateFormat("EEE", Locale.getDefault())
        holder.tvName.text = nameFormat.format(date.time)
        holder.tvNumber.text = date.get(Calendar.DAY_OF_MONTH).toString()

        if (isSelected) {
            holder.card.setCardBackgroundColor(Color.parseColor("#2196F3"))
            holder.tvName.setTextColor(Color.WHITE)
            holder.tvNumber.setTextColor(Color.WHITE)
        } else {
            holder.card.setCardBackgroundColor(Color.WHITE)
            holder.tvName.setTextColor(Color.parseColor("#888888"))
            holder.tvNumber.setTextColor(Color.parseColor("#333333"))
        }

        holder.itemView.setOnClickListener {
            val oldPos = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)
            onDateSelected(date)
        }
    }

    override fun getItemCount() = dates.size
}