package com.yourname.habitapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yourname.habitapp.R
import com.yourname.habitapp.ui.MainActivity

object NotificationHelper {

    private var activeRingtone: Ringtone? = null

    private fun getChannelId(base: String, context: Context): String {
        val settingsPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val toneUri = settingsPrefs.getString("notification_tone", "default")
        return "${base}_${toneUri.hashCode()}"
    }

    const val BASE_HABIT_COMPLETE = "habit_complete"
    const val BASE_HABIT_STREAK   = "habit_streak"
    const val BASE_HABIT_REMINDER = "habit_reminder"
    const val BASE_TODO           = "todo_reminders"
    const val BASE_ACHIEVEMENT    = "achievements"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val settingsPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val customToneUriString = settingsPrefs.getString("notification_tone", null)
        val toneUri = if (customToneUriString != null) android.net.Uri.parse(customToneUriString) 
                      else android.provider.Settings.System.DEFAULT_NOTIFICATION_URI

        val channelConfigs = listOf(
            Pair(BASE_HABIT_COMPLETE, "إتمام العادات"),
            Pair(BASE_HABIT_STREAK, "إنجاز Streak"),
            Pair(BASE_HABIT_REMINDER, "تذكير يومي"),
            Pair(BASE_TODO, "تنبيهات المهام"),
            Pair(BASE_ACHIEVEMENT, "الإنجازات")
        )

        channelConfigs.forEach { (baseId, name) ->
            val channelId = getChannelId(baseId, context)
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH).apply {
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(toneUri, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun showHabitCompleteNotification(context: Context, habitName: String, streak: Int) {
        val message = if (streak > 1) "أحسنت! ${streak} يوم متتالي 🔥" else "أحسنت! استمر في هذا الإيقاع 💪"
        show(context, habitName.hashCode(), getChannelId(BASE_HABIT_COMPLETE, context), "✅ أكملت: $habitName", message)
    }

    fun showStreakMilestoneNotification(context: Context, habitName: String, streak: Int) {
        val emoji = when (streak) {
            7    -> "🔥"
            30   -> "💎"
            100  -> "👑"
            else -> "⭐"
        }
        show(context, (habitName + streak).hashCode(), getChannelId(BASE_HABIT_STREAK, context), "$emoji تهانينا! $streak يوم متتالي", "عادة \"$habitName\" وصلت لـ $streak يوم متتالي")
    }

    fun showDailyReminderNotification(context: Context, pendingCount: Int) {
        show(context, 1001, getChannelId(BASE_HABIT_REMINDER, context), "⏰ تذكير يومي", "لديك $pendingCount عادة لم تكتمل بعد — هيا نبدأ!")
    }

    fun showTodoStartNotification(context: Context, todoId: Int, title: String) {
        show(context, todoId * 10 + 1, getChannelId(BASE_TODO, context), "🚀 حان وقت المهمة!", title)
        playTaskSound(context, "START", true)
    }

    fun showTodoEndNotification(context: Context, todoId: Int, title: String) {
        show(context, todoId * 10 + 2, getChannelId(BASE_TODO, context), "🏁 انتهى وقت المهمة", "هل أكملت: $title ؟")
        playTaskSound(context, "END", false)
    }

    fun showTodoBeforeNotification(context: Context, todoId: Int, title: String, minutes: Int) {
        show(context, todoId * 10 + 3, getChannelId(BASE_TODO, context), "⏰ تنبيه مسبق", "\"$title\" تبدأ خلال $minutes دقيقة")
    }

    fun cancelNotification(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
        stopAllSounds()
    }

    fun showAchievementNotification(context: Context, achievementTitle: String, icon: String) {
        show(context, achievementTitle.hashCode(), getChannelId(BASE_ACHIEVEMENT, context), "$icon إنجاز جديد!", "تهانينا! فتحت إنجاز: $achievementTitle")
    }

    fun playTaskSound(context: Context, type: String, isStart: Boolean) {
        try {
            val settingsPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            if (settingsPrefs.getBoolean("mute_notifications", false)) return // Muted

            val customToneUriString = settingsPrefs.getString("notification_tone", null)
            val toneUri = if (customToneUriString != null) android.net.Uri.parse(customToneUriString) 
                          else if (isStart) android.provider.Settings.System.DEFAULT_NOTIFICATION_URI 
                          else android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
            
            stopAllSounds()
            val ringtone = android.media.RingtoneManager.getRingtone(context, toneUri)
            activeRingtone = ringtone
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAllSounds() {
        try {
            activeRingtone?.let {
                if (it.isPlaying) it.stop()
            }
            activeRingtone = null
        } catch (e: Exception) {}
    }

    private fun show(context: Context, id: Int, channelId: String, title: String, message: String) {
        val settingsPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val isMuted = settingsPrefs.getBoolean("mute_notifications", false)
        val vibrationEnabled = settingsPrefs.getBoolean("vibration", true)
        val customToneUriString = settingsPrefs.getString("notification_tone", null)
        val toneUri = if (customToneUriString != null) android.net.Uri.parse(customToneUriString) 
                      else android.provider.Settings.System.DEFAULT_NOTIFICATION_URI

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply { putExtra("NOTIFICATION_ID", id) }
        val donePendingIntent = PendingIntent.getBroadcast(context, id + 10000, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val deleteIntent = Intent(context, NotificationActionReceiver::class.java).apply { action = "ACTION_DISMISS" }
        val deletePendingIntent = PendingIntent.getBroadcast(context, id + 20000, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deletePendingIntent) // Stop sound on swipe away
            .addAction(0, "تم", donePendingIntent)
            .setAutoCancel(true)

        if (!isMuted) {
            builder.setSound(toneUri)
        } else {
            builder.setSound(null)
        }

        if (vibrationEnabled && !isMuted) {
            builder.setVibrate(longArrayOf(0, 500, 200, 500))
            builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        val notification = builder.build()
        notification.flags = notification.flags or NotificationCompat.FLAG_INSISTENT

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) { }
    }
}
