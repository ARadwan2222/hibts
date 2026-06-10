package com.yourname.habitapp.worker

import android.content.Context
import androidx.work.*
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.utils.NotificationHelper
import java.util.concurrent.TimeUnit

class HabitReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(context)
        val habits = db.habitDao().getAllHabitsSync()
        val pending = habits.count { !it.isCompletedToday }

        if (pending > 0) {
            NotificationHelper.showDailyReminderNotification(context, pending)
        }

        // أعد جدولة نفسك لليوم التالي
        scheduleNext(context)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_habit_reminder"

        // جدولة التذكير اليومي الساعة 8 صباحاً
        fun scheduleDailyReminder(context: Context) {
            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 8)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                if (before(now)) add(java.util.Calendar.DAY_OF_YEAR, 1)
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

        private fun scheduleNext(context: Context) {
            // PeriodicWork يُعيد التشغيل تلقائياً — لا حاجة لإعادة الجدولة
        }
    }
}
