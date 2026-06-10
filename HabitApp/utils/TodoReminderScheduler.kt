package com.yourname.habitapp.utils

import android.content.Context
import androidx.work.*
import com.yourname.habitapp.data.models.TodoItem
import com.yourname.habitapp.worker.TodoReminderWorker
import java.util.concurrent.TimeUnit

object TodoReminderScheduler {

    // يجدول 3 تنبيهات لكل مهمة لها وقت محدد
    fun scheduleTodoReminders(context: Context, todo: TodoItem) {
        val now = System.currentTimeMillis()

        // 1) تنبيه قبل البداية
        if (todo.startTime != null && todo.reminderBefore > 0) {
            val triggerTime = todo.startTime - (todo.reminderBefore * 60 * 1000L)
            if (triggerTime > now) {
                scheduleWorker(
                    context,
                    uniqueId = "todo_before_${todo.id}",
                    delayMs = triggerTime - now,
                    type = "BEFORE",
                    todoId = todo.id,
                    todoTitle = todo.title,
                    minutes = todo.reminderBefore
                )
            }
        }

        // 2) تنبيه لحظة البداية
        if (todo.startTime != null && todo.reminderStart) {
            val delayMs = todo.startTime - now
            if (delayMs > 0) {
                scheduleWorker(
                    context,
                    uniqueId = "todo_start_${todo.id}",
                    delayMs = delayMs,
                    type = "START",
                    todoId = todo.id,
                    todoTitle = todo.title
                )
            }
        }

        // 3) تنبيه لحظة النهاية
        if (todo.endTime != null && todo.reminderEnd) {
            val delayMs = todo.endTime - now
            if (delayMs > 0) {
                scheduleWorker(
                    context,
                    uniqueId = "todo_end_${todo.id}",
                    delayMs = delayMs,
                    type = "END",
                    todoId = todo.id,
                    todoTitle = todo.title
                )
            }
        }
    }

    // إلغاء تنبيهات مهمة (عند الحذف أو الإتمام)
    fun cancelTodoReminders(context: Context, todoId: Int) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork("todo_before_$todoId")
        workManager.cancelUniqueWork("todo_start_$todoId")
        workManager.cancelUniqueWork("todo_end_$todoId")
    }

    private fun scheduleWorker(
        context: Context,
        uniqueId: String,
        delayMs: Long,
        type: String,
        todoId: Int,
        todoTitle: String,
        minutes: Int = 0
    ) {
        val data = workDataOf(
            "TYPE"       to type,
            "TODO_ID"    to todoId,
            "TODO_TITLE" to todoTitle,
            "MINUTES"    to minutes
        )

        val request = OneTimeWorkRequestBuilder<TodoReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueId, ExistingWorkPolicy.REPLACE, request)
    }
}
