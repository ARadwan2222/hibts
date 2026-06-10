package com.yourname.habitapp.utils

import android.content.Context
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.Achievement

// ─── تعريف كل الإنجازات الممكنة ─────────────────────────────────────────────
data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val xpReward: Int = 50
)

object AchievementDefinitions {
    fun getAll(context: Context): List<AchievementDef> {
        return listOf(
            AchievementDef("FIRST_HABIT",    context.getString(R.string.ach_first_habit_title),     context.getString(R.string.ach_first_habit_desc),              "🌱", 20),
            AchievementDef("WEEK_STREAK",    context.getString(R.string.ach_week_streak_title),     context.getString(R.string.ach_week_streak_desc),      "🔥", 70),
            AchievementDef("MONTH_STREAK",   context.getString(R.string.ach_month_streak_title),    context.getString(R.string.ach_month_streak_desc),      "💎", 300),
            AchievementDef("LEGEND_STREAK",  context.getString(R.string.ach_legend_streak_title),   context.getString(R.string.ach_legend_streak_desc),     "👑", 500),
            AchievementDef("FIVE_HABITS",    context.getString(R.string.ach_five_habits_title),     context.getString(R.string.ach_five_habits_desc),                "🥇", 50),
            AchievementDef("TEN_TODOS",      context.getString(R.string.ach_ten_todos_title),       context.getString(R.string.ach_ten_todos_desc),               "⚡", 50),
            AchievementDef("FIFTY_TODOS",    context.getString(R.string.ach_fifty_todos_title),     context.getString(R.string.ach_fifty_todos_desc),               "🏆", 150),
            AchievementDef("HUNDRED_TODOS",  context.getString(R.string.ach_hundred_todos_title),   context.getString(R.string.ach_hundred_todos_desc),              "🚀", 300),
            AchievementDef("TIMED_TODOS",    context.getString(R.string.ach_timed_todos_title),     context.getString(R.string.ach_timed_todos_desc),          "⏰", 80),
            AchievementDef("FIRST_GOAL",     context.getString(R.string.ach_first_goal_title),      context.getString(R.string.ach_first_goal_desc),          "🎯", 100),
            AchievementDef("THREE_GOALS",    context.getString(R.string.ach_three_goals_title),     context.getString(R.string.ach_three_goals_desc),         "🌟", 200),
            AchievementDef("FIVE_GOALS",     context.getString(R.string.ach_five_goals_title),      context.getString(R.string.ach_five_goals_desc),         "🌍", 350),
            AchievementDef("FIRST_SHARE",    context.getString(R.string.ach_first_share_title),     context.getString(R.string.ach_first_share_desc),      "🤝", 30),
            AchievementDef("TEN_SHARES",     context.getString(R.string.ach_ten_shares_title),      context.getString(R.string.ach_ten_shares_desc),            "📣", 100),
            AchievementDef("EARLY_BIRD",     context.getString(R.string.ach_early_bird_title),      context.getString(R.string.ach_early_bird_desc),    "🌅", 40),
            AchievementDef("NIGHT_OWL",      context.getString(R.string.ach_night_owl_title),       context.getString(R.string.ach_night_owl_desc),    "🌙", 40)
        )
    }
}

// ─── محرك الإنجازات ──────────────────────────────────────────────────────────
object AchievementEngine {

    suspend fun checkAndUnlock(context: Context, trigger: String, value: Int = 0) {
        val db = AppDatabase.getInstance(context)
        val achievementDao = db.achievementDao()

        val allDefs = AchievementDefinitions.getAll(context)
        val toUnlock = mutableListOf<AchievementDef>()

        when (trigger) {
            "HABIT_ADDED" -> {
                if (value == 1) allDefs.find { it.id == "FIRST_HABIT" }?.let { toUnlock.add(it) }
                if (value >= 5) allDefs.find { it.id == "FIVE_HABITS" }?.let { toUnlock.add(it) }
            }
            "STREAK_UPDATE" -> {
                if (value >= 7)   allDefs.find { it.id == "WEEK_STREAK" }?.let { toUnlock.add(it) }
                if (value >= 30)  allDefs.find { it.id == "MONTH_STREAK" }?.let { toUnlock.add(it) }
                if (value >= 100) allDefs.find { it.id == "LEGEND_STREAK" }?.let { toUnlock.add(it) }
            }
            "TODO_COMPLETED" -> {
                if (value >= 10)  allDefs.find { it.id == "TEN_TODOS" }?.let { toUnlock.add(it) }
                if (value >= 50)  allDefs.find { it.id == "FIFTY_TODOS" }?.let { toUnlock.add(it) }
                if (value >= 100) allDefs.find { it.id == "HUNDRED_TODOS" }?.let { toUnlock.add(it) }
            }
            "TIMED_TODO_DONE" -> {
                if (value >= 10) allDefs.find { it.id == "TIMED_TODOS" }?.let { toUnlock.add(it) }
            }
            "GOAL_COMPLETED" -> {
                if (value >= 1) allDefs.find { it.id == "FIRST_GOAL" }?.let { toUnlock.add(it) }
                if (value >= 3) allDefs.find { it.id == "THREE_GOALS" }?.let { toUnlock.add(it) }
                if (value >= 5) allDefs.find { it.id == "FIVE_GOALS" }?.let { toUnlock.add(it) }
            }
            "SHARED" -> {
                if (value == 1)  allDefs.find { it.id == "FIRST_SHARE" }?.let { toUnlock.add(it) }
                if (value >= 10) allDefs.find { it.id == "TEN_SHARES" }?.let { toUnlock.add(it) }
            }
            "EARLY_BIRD" -> allDefs.find { it.id == "EARLY_BIRD" }?.let { toUnlock.add(it) }
            "NIGHT_OWL"  -> allDefs.find { it.id == "NIGHT_OWL" }?.let { toUnlock.add(it) }
        }

        // أضف فقط الإنجازات التي لم تُفتح بعد
        for (def in toUnlock) {
            val existing = achievementDao.getByAchievementId(def.id)
            if (existing == null) {
                achievementDao.insert(Achievement(
                    achievementId = def.id,
                    title = def.title,
                    description = def.description,
                    icon = def.icon,
                    xpReward = def.xpReward
                ))
                // XP
                addXP(context, def.xpReward)
                // تنبيه
                NotificationHelper.showAchievementNotification(context, def.title, def.icon)
            }
        }
    }

    // ─── نظام XP ─────────────────────────────────────────────────────────────
    fun addXP(context: Context, amount: Int) {
        val prefs = context.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
        val current = prefs.getInt("total_xp", 0)
        prefs.edit().putInt("total_xp", current + amount).apply()
    }

    fun getTotalXP(context: Context): Int {
        val prefs = context.getSharedPreferences("habit_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("total_xp", 0)
    }

    fun getLevel(context: Context, xp: Int): Pair<Int, String> = when {
        xp < 100   -> Pair(1, context.getString(R.string.level_beginner))
        xp < 250   -> Pair(2, context.getString(R.string.level_learner))
        xp < 500   -> Pair(3, context.getString(R.string.level_active))
        xp < 800   -> Pair(4, context.getString(R.string.level_committed))
        xp < 1200  -> Pair(5, context.getString(R.string.level_persistent))
        xp < 1800  -> Pair(6, context.getString(R.string.level_professional))
        xp < 2500  -> Pair(7, context.getString(R.string.level_expert))
        else       -> Pair(8, context.getString(R.string.level_legend))
    }
}
