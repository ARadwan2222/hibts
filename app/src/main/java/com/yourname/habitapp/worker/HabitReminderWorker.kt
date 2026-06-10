package com.yourname.habitapp.worker

import android.content.Context
import androidx.work.*
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.data.models.HabitFrequency
import com.yourname.habitapp.utils.NotificationHelper
import java.util.Calendar
import java.util.concurrent.TimeUnit

class HabitReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(context)
        val dao = db.habitDao()
        val calendar = Calendar.getInstance()
        
        val habits = dao.getAllHabitsSync()
        val nowMillis = System.currentTimeMillis()

        habits.forEach { habit ->
            var shouldReset = false
            when (habit.frequency) {
                HabitFrequency.DAILY -> shouldReset = true
                HabitFrequency.WEEKLY -> {
                    if (calendar.get(Calendar.DAY_OF_WEEK) == habit.specificDay) {
                        val lastCompleted = habit.lastCompletedTimestamp ?: 0L
                        val diffDays = (nowMillis - lastCompleted) / (1000 * 60 * 60 * 24)
                        if (diffDays >= 2) shouldReset = true
                    }
                }
                HabitFrequency.MONTHLY -> {
                    if (calendar.get(Calendar.DAY_OF_MONTH) == habit.specificDay) {
                        val lastCompleted = habit.lastCompletedTimestamp ?: 0L
                        val diffDays = (nowMillis - lastCompleted) / (1000 * 60 * 60 * 24)
                        if (diffDays >= 7) shouldReset = true
                    }
                }
                HabitFrequency.YEARLY -> {
                    if (calendar.get(Calendar.DAY_OF_YEAR) == 1) shouldReset = true
                }
            }

            if (shouldReset && habit.isCompletedToday) {
                dao.updateHabit(habit.copy(isCompletedToday = false))
            }
        }

        // 6. التعامل مع المهام (Todos)
        val todoDao = db.todoDao()
        val yesterday = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val startOfYesterday = yesterday.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val endOfYesterday = yesterday.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
        
        // وسم المهام التي فات وقتها ولم تكتمل بالسلب (Missed)
        todoDao.markMissedTasks(System.currentTimeMillis())

        // 5. تهنئة عيد الميلاد
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val birthdateMillis = prefs.getLong("user_birthdate", 0)
        if (birthdateMillis > 0) {
            val calBirth = Calendar.getInstance().apply { timeInMillis = birthdateMillis }
            if (calendar.get(Calendar.MONTH) == calBirth.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == calBirth.get(Calendar.DAY_OF_MONTH)) {
                NotificationHelper.showAchievementNotification(
                    context, 
                    context.getString(com.yourname.habitapp.R.string.birthday_congrats_title), 
                    context.getString(com.yourname.habitapp.R.string.birthday_congrats_msg)
                )
            }
        }

        val habitsList = dao.getAllHabitsSync()
        val pending = habitsList.count { !it.isCompletedToday && it.frequency == HabitFrequency.DAILY }

        if (pending > 0) {
            NotificationHelper.showDailyReminderNotification(context, pending)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_habit_reminder"

        // جدولة التذكير اليومي الساعة 8 صباحاً
        fun scheduleDailyReminder(context: Context) {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }

            val delay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<HabitReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
