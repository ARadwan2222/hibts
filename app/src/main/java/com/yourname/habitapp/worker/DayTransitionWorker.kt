package com.yourname.habitapp.worker

import android.content.Context
import androidx.work.*
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.Achievement
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DayTransitionWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(context)
        val todoDao = db.todoDao()
        val habitDao = db.habitDao()
        val calendar = Calendar.getInstance()
        
        // 1. Mark missed tasks from yesterday
        val now = System.currentTimeMillis()
        todoDao.markMissedTasks(now)
        
        // 2. Collect stats for the "Achievements" summary
        val yesterday = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val startOfYesterday = yesterday.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val endOfYesterday = yesterday.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
        
        val allYesterdayTasks = todoDao.getAllTodosSync().filter { it.targetDate in startOfYesterday..endOfYesterday }
        val completed = allYesterdayTasks.count { it.isCompleted }
        val missed = allYesterdayTasks.count { it.isMissed || (!it.isCompleted && (it.endTime ?: 0) < now) }
        
        if (allYesterdayTasks.isNotEmpty()) {
            val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(yesterday.time)
            val summary = Achievement(
                achievementId = "DAILY_REPORT_$dateStr",
                title = context.getString(com.yourname.habitapp.R.string.daily_report_title, dateStr),
                description = context.getString(com.yourname.habitapp.R.string.daily_report_desc, completed, missed),
                icon = "📊",
                xpReward = completed * 10
            )
            db.achievementDao().insert(summary)
        }

        // 3. Habit Resets (Existing logic moved here for better midnight handling)
        habitDao.resetDailyHabits()
        habitDao.resetWeeklyHabits(calendar.get(Calendar.DAY_OF_WEEK))
        habitDao.resetMonthlyHabits(calendar.get(Calendar.DAY_OF_MONTH))

        // 4. Birthday Check
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val birthdate = prefs.getLong("user_birthdate", 0)
        if (birthdate > 0) {
            val birthCal = Calendar.getInstance().apply { timeInMillis = birthdate }
            if (birthCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) && 
                birthCal.get(Calendar.DAY_OF_MONTH) == calendar.get(Calendar.DAY_OF_MONTH)) {
                com.yourname.habitapp.utils.NotificationHelper.showBirthdayNotification(context)
            }
        }

        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val delay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<DayTransitionWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "day_transition_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
