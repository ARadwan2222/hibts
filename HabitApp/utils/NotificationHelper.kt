package com.yourname.habitapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yourname.habitapp.R
import com.yourname.habitapp.ui.MainActivity

object NotificationHelper {

    // ─── معرّفات القنوات ────────────────────────────────────────────────────
    const val CHANNEL_HABIT_COMPLETE = "habit_complete"
    const val CHANNEL_HABIT_STREAK   = "habit_streak"
    const val CHANNEL_HABIT_REMINDER = "habit_reminder"
    const val CHANNEL_TODO           = "todo_reminders"
    const val CHANNEL_ACHIEVEMENT    = "achievements"

    // ─── إنشاء قنوات التنبيه (يُستدعى مرة واحدة عند فتح التطبيق) ──────────
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(CHANNEL_HABIT_COMPLETE, "إتمام العادات",   NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_HABIT_STREAK,   "إنجاز Streak",    NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_HABIT_REMINDER, "تذكير يومي",      NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_TODO,           "تنبيهات المهام",  NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_ACHIEVEMENT,    "الإنجازات",       NotificationManager.IMPORTANCE_DEFAULT)
        )

        channels.forEach { manager.createNotificationChannel(it) }
    }

    // ─── تنبيه إتمام عادة ──────────────────────────────────────────────────
    fun showHabitCompleteNotification(context: Context, habitName: String, streak: Int) {
        val message = if (streak > 1) "أحسنت! ${streak} يوم متتالي 🔥" else "أحسنت! استمر في هذا الإيقاع 💪"

        show(
            context,
            id = habitName.hashCode(),
            channel = CHANNEL_HABIT_COMPLETE,
            title = "✅ أكملت: $habitName",
            message = message
        )
    }

    // ─── تنبيه Streak مميز ─────────────────────────────────────────────────
    fun showStreakMilestoneNotification(context: Context, habitName: String, streak: Int) {
        val emoji = when (streak) {
            7    -> "🔥"
            30   -> "💎"
            100  -> "👑"
            else -> "⭐"
        }
        show(
            context,
            id = (habitName + streak).hashCode(),
            channel = CHANNEL_HABIT_STREAK,
            title = "$emoji تهانينا! $streak يوم متتالي",
            message = "عادة \"$habitName\" وصلت لـ $streak يوم متتالي — رائع!"
        )
    }

    // ─── تنبيه التذكير اليومي ──────────────────────────────────────────────
    fun showDailyReminderNotification(context: Context, pendingCount: Int) {
        show(
            context,
            id = 1001,
            channel = CHANNEL_HABIT_REMINDER,
            title = "⏰ تذكير يومي",
            message = "لديك $pendingCount عادة لم تكتمل بعد — هيا نبدأ!"
        )
    }

    // ─── تنبيهات المهام ────────────────────────────────────────────────────
    fun showTodoStartNotification(context: Context, todoId: Int, title: String) {
        show(context, id = todoId * 10 + 1, channel = CHANNEL_TODO,
            title = "🚀 حان وقت المهمة!", message = title)
    }

    fun showTodoEndNotification(context: Context, todoId: Int, title: String) {
        show(context, id = todoId * 10 + 2, channel = CHANNEL_TODO,
            title = "🏁 انتهى وقت المهمة", message = "هل أكملت: $title ؟")
    }

    fun showTodoBeforeNotification(context: Context, todoId: Int, title: String, minutes: Int) {
        show(context, id = todoId * 10 + 3, channel = CHANNEL_TODO,
            title = "⏰ تنبيه مسبق", message = "\"$title\" تبدأ خلال $minutes دقيقة")
    }

    // ─── تنبيه إنجاز جديد ──────────────────────────────────────────────────
    fun showAchievementNotification(context: Context, achievementTitle: String, icon: String) {
        show(context, id = achievementTitle.hashCode(), channel = CHANNEL_ACHIEVEMENT,
            title = "$icon إنجاز جديد!", message = "تهانينا! فتحت إنجاز: $achievementTitle")
    }

    // ─── الدالة الداخلية المشتركة ──────────────────────────────────────────
    private fun show(context: Context, id: Int, channel: String, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)   // أنشئ أيقونة بسيطة في res/drawable
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // المستخدم لم يمنح صلاحية POST_NOTIFICATIONS (Android 13+)
        }
    }
}
