package com.yourname.habitapp.utils

import android.content.Context
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
    val ALL = listOf(
        AchievementDef("FIRST_HABIT",    "بداية الرحلة",     "أضف أول عادة",              "🌱", 20),
        AchievementDef("WEEK_STREAK",    "أسبوع كامل",       "7 أيام streak متتالية",      "🔥", 70),
        AchievementDef("MONTH_STREAK",   "شهر كامل",         "30 يوم streak متتالية",      "💎", 300),
        AchievementDef("LEGEND_STREAK",  "أسطورة",           "100 يوم streak متتالية",     "👑", 500),
        AchievementDef("FIVE_HABITS",    "محترف",            "أضف 5 عادات",                "🥇", 50),
        AchievementDef("TEN_TODOS",      "منجز",             "أكمل 10 مهام",               "⚡", 50),
        AchievementDef("FIFTY_TODOS",    "بطل المهام",       "أكمل 50 مهمة",               "🏆", 150),
        AchievementDef("HUNDRED_TODOS",  "أسطورة المهام",    "أكمل 100 مهمة",              "🚀", 300),
        AchievementDef("TIMED_TODOS",    "منظم الوقت",       "10 مهام بوقت محدد",          "⏰", 80),
        AchievementDef("FIRST_GOAL",     "هدف محقق",         "أكمل هدفاً سنوياً",          "🎯", 100),
        AchievementDef("THREE_GOALS",    "منجز السنة",       "أكمل 3 أهداف سنوية",         "🌟", 200),
        AchievementDef("FIVE_GOALS",     "فاتح الأهداف",     "أكمل 5 أهداف سنوية",         "🌍", 350),
        AchievementDef("FIRST_SHARE",    "المشارك",          "شارك إنجازاً لأول مرة",      "🤝", 30),
        AchievementDef("TEN_SHARES",     "المؤثر",           "شارك 10 إنجازات",            "📣", 100),
        AchievementDef("EARLY_BIRD",     "الباكر",           "أكمل مهمة قبل 8 صباحاً",    "🌅", 40),
        AchievementDef("NIGHT_OWL",      "الليلي",           "أكمل مهمة بعد 10 مساءً",    "🌙", 40),
        AchievementDef("STATS_VIEWER",   "المحلل",           "افتح الإحصاءات 10 مرات",     "📊", 30),
        AchievementDef("THIRTY_DAYS",    "مداوم",            "30 يوم دخول متتالي",          "📅", 200),
        AchievementDef("COLORFUL",       "متعدد المواهب",    "عادة من كل نوع تكرار",        "🌈", 60)
    )
}

// ─── محرك الإنجازات ──────────────────────────────────────────────────────────
object AchievementEngine {

    suspend fun checkAndUnlock(context: Context, trigger: String, value: Int = 0) {
        val db = AppDatabase.getInstance(context)
        val achievementDao = db.achievementDao()

        val toUnlock = mutableListOf<AchievementDef>()

        when (trigger) {
            "HABIT_ADDED" -> {
                if (value == 1) toUnlock.add(findDef("FIRST_HABIT"))
                if (value >= 5) toUnlock.add(findDef("FIVE_HABITS"))
            }
            "STREAK_UPDATE" -> {
                if (value >= 7)   toUnlock.add(findDef("WEEK_STREAK"))
                if (value >= 30)  toUnlock.add(findDef("MONTH_STREAK"))
                if (value >= 100) toUnlock.add(findDef("LEGEND_STREAK"))
            }
            "TODO_COMPLETED" -> {
                if (value >= 10)  toUnlock.add(findDef("TEN_TODOS"))
                if (value >= 50)  toUnlock.add(findDef("FIFTY_TODOS"))
                if (value >= 100) toUnlock.add(findDef("HUNDRED_TODOS"))
            }
            "TIMED_TODO_DONE" -> {
                if (value >= 10) toUnlock.add(findDef("TIMED_TODOS"))
            }
            "GOAL_COMPLETED" -> {
                if (value >= 1) toUnlock.add(findDef("FIRST_GOAL"))
                if (value >= 3) toUnlock.add(findDef("THREE_GOALS"))
                if (value >= 5) toUnlock.add(findDef("FIVE_GOALS"))
            }
            "SHARED" -> {
                if (value == 1)  toUnlock.add(findDef("FIRST_SHARE"))
                if (value >= 10) toUnlock.add(findDef("TEN_SHARES"))
            }
            "EARLY_BIRD" -> toUnlock.add(findDef("EARLY_BIRD"))
            "NIGHT_OWL"  -> toUnlock.add(findDef("NIGHT_OWL"))
        }

        // أضف فقط الإنجازات التي لم تُفتح بعد
        for (def in toUnlock.filterNotNull()) {
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

    private fun findDef(id: String): AchievementDef? =
        AchievementDefinitions.ALL.find { it.id == id }

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

    fun getLevel(xp: Int): Pair<Int, String> = when {
        xp < 100   -> Pair(1, "مبتدئ 🥉")
        xp < 250   -> Pair(2, "متعلم 🥈")
        xp < 500   -> Pair(3, "نشيط 🥇")
        xp < 800   -> Pair(4, "ملتزم ⭐")
        xp < 1200  -> Pair(5, "مثابر 🌟")
        xp < 1800  -> Pair(6, "محترف 💫")
        xp < 2500  -> Pair(7, "خبير 🏆")
        else       -> Pair(8, "أسطورة 👑")
    }
}
