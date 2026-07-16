package com.yourname.habitapp.worker

import android.content.Context
import androidx.work.*
import com.yourname.habitapp.R
import com.yourname.habitapp.data.AppDatabase
import com.yourname.habitapp.utils.NotificationHelper
import java.util.concurrent.TimeUnit

class GoalReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(context)
        val currentGoals = db.yearGoalDao().getAllCurrentGoalsSync()
        
        if (currentGoals.isNotEmpty()) {
            val goal = currentGoals.first() 
            NotificationHelper.showYearGoalNotification(context, goal.title)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "goal_periodic_reminder"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<GoalReminderWorker>(10, TimeUnit.DAYS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
