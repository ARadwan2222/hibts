package com.yourname.habitapp.ui.achievements

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.utils.AchievementDefinitions

class AchievementsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerAchievements)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        val db = AppDatabase.getInstance(this)
        db.achievementDao().getAllAchievements().observe(this) { unlocked ->
            val unlockedIds = unlocked.map { it.achievementId }.toSet()
            val allDefs = AchievementDefinitions.getAll(this)
            val adapter = AchievementAdapter(allDefs, unlockedIds) { def ->
                AchievementDetailDialog(def).show(supportFragmentManager, "AchievementDetail")
            }
            recyclerView.adapter = adapter
        }
    }
}
