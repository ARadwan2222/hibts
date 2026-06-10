package com.yourname.habitapp.ui.achievements

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.yourname.habitapp.R
import com.yourname.habitapp.utils.AchievementDef

class AchievementAdapter(
    private val allDefs: List<AchievementDef>,
    private val unlockedIds: Set<String>,
    private val onClick: (AchievementDef) -> Unit
) : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card      : CardView = view.findViewById(R.id.cardAchievement)
        val tvIcon    : TextView = view.findViewById(R.id.tvAchievementIcon)
        val tvTitle   : TextView = view.findViewById(R.id.tvAchievementTitle)
        val tvDesc    : TextView = view.findViewById(R.id.tvAchievementDesc)
        val tvLocked  : TextView = view.findViewById(R.id.tvLocked)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_achievement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val def = allDefs[position]
        val isUnlocked = def.id in unlockedIds

        holder.tvIcon.text  = if (isUnlocked) def.icon else "🔒"
        holder.tvTitle.text = def.title
        holder.tvDesc.text  = def.description
        holder.tvLocked.visibility = if (isUnlocked) View.GONE else View.VISIBLE

        // تعتيم الإنجازات المقفلة
        if (isUnlocked) {
            holder.card.alpha = 1.0f
            holder.tvIcon.colorFilter = null
        } else {
            holder.card.alpha = 0.5f
            holder.tvIcon.colorFilter = ColorMatrixColorFilter(ColorMatrix().also { it.setSaturation(0f) })
        }

        holder.itemView.setOnClickListener { onClick(def) }
    }

    override fun getItemCount() = allDefs.size
}
