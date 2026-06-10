package com.yourname.habitapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourname.habitapp.utils.NotificationHelper

class TodoReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val type      = inputData.getString("TYPE") ?: return Result.failure()
        val todoId    = inputData.getInt("TODO_ID", -1)
        val todoTitle = inputData.getString("TODO_TITLE") ?: return Result.failure()
        val minutes   = inputData.getInt("MINUTES", 10)

        when (type) {
            "BEFORE" -> {
                NotificationHelper.showTodoBeforeNotification(context, todoId, todoTitle, minutes)
            }
            "START"  -> {
                NotificationHelper.showTodoStartNotification(context, todoId, todoTitle)
            }
            "END"    -> {
                NotificationHelper.showTodoEndNotification(context, todoId, todoTitle)
            }
        }

        return Result.success()
    }
}
