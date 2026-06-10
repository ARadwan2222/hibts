package com.yourname.habitapp.ui.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.utils.AchievementDefinitions
import com.yourname.habitapp.utils.AchievementEngine

class AchievementsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_achievements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerAchievements)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val tvXP    = view.findViewById<TextView>(R.id.tvTotalXP)
        val tvLevel = view.findViewById<TextView>(R.id.tvLevel)

        // XP ومستوى المستخدم
        val totalXP = AchievementEngine.getTotalXP(requireContext())
        val (level, levelName) = AchievementEngine.getLevel(totalXP)
        tvXP?.text   = "⚡ $totalXP XP"
        tvLevel?.text = "المستوى $level — $levelName"

        // مراقبة الإنجازات المحققة
        db.achievementDao().getAllAchievements().observe(viewLifecycleOwner) { unlocked ->
            val unlockedIds = unlocked.map { it.achievementId }.toSet()
            val allDefs = AchievementDefinitions.ALL
            val adapter = AchievementAdapter(allDefs, unlockedIds) { def ->
                // عرض تفاصيل الإنجاز عند الضغط
                AchievementDetailDialog(def).show(parentFragmentManager, "AchievementDetail")
            }
            recyclerView.adapter = adapter
        }
    }
}
