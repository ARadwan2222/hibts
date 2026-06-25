package com.yourname.habitapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yourname.habitapp.R
import com.yourname.habitapp.ui.MainActivity

object NotificationHelper {

    private fun getChannelId(context: Context): String {
        val settingsPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val toneUri = settingsPrefs.getString("notification_tone", "default")
        return "todo_reminders_${toneUri.hashCode()}"
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(getChannelId(context), "تنبيهات المهام", NotificationManager.IMPORTANCE_HIGH).apply {
            enableLights(true)
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(null, null) 
        }
        manager.createNotificationChannel(channel)
    }

    fun showTodoStartNotification(context: Context, todoId: Int, title: String) {
        show(context, todoId * 10 + 1, getChannelId(context), "🚀 حان وقت المهمة!", title, todoId)
        startSoundService(context, todoId, true)
    }

    fun showTodoEndNotification(context: Context, todoId: Int, title: String) {
        show(context, todoId * 10 + 2, getChannelId(context), "🏁 انتهى وقت المهمة", "هل أكملت: $title ؟", todoId)
        startSoundService(context, todoId, false)
    }

    fun showTodoBeforeNotification(context: Context, todoId: Int, title: String, minutes: Int) {
        show(context, todoId * 10 + 3, getChannelId(context), "⏰ تنبيه مسبق", "\"$title\" تبدأ خلال $minutes دقيقة", todoId)
    }

    private fun startSoundService(context: Context, todoId: Int, isStart: Boolean) {
        val settingsPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        if (settingsPrefs.getBoolean("mute_notifications", false)) return

        val customToneUriString = settingsPrefs.getString("notification_tone", null)
        val toneUri = if (customToneUriString != null) customToneUriString 
                      else if (isStart) android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString() 
                      else android.provider.Settings.System.DEFAULT_RINGTONE_URI.toString()

        val intent = Intent(context, SoundService::class.java).apply {
            action = "ACTION_PLAY"
            putExtra("TODO_ID", todoId)
            putExtra("TONE_URI", toneUri)
        }
        context.startService(intent)
    }

    fun stopAllSounds(context: Context, todoId: Int = -1) {
        val intent = Intent(context, SoundService::class.java).apply {
            action = "ACTION_STOP"
            putExtra("TODO_ID", todoId)
        }
        context.startService(intent)
    }

    private fun show(context: Context, id: Int, channelId: String, title: String, message: String, todoId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // DIRECT SERVICE CALL for Done
        val doneIntent = Intent(context, SoundService::class.java).apply { 
            action = "ACTION_DONE"
            putExtra("NOTIFICATION_ID", id)
            putExtra("TODO_ID", todoId)
        }
        val donePendingIntent = PendingIntent.getService(context, id + 10000, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // DIRECT SERVICE CALL for Dismiss (Swipe)
        val deleteIntent = Intent(context, SoundService::class.java).apply { 
            action = "ACTION_STOP" 
            putExtra("NOTIFICATION_ID", id)
            putExtra("TODO_ID", todoId)
        }
        val deletePendingIntent = PendingIntent.getService(context, id + 20000, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deletePendingIntent) 
            .addAction(0, "تم", donePendingIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, builder.build())
    }

    // Unused compatibility placeholders
    fun showHabitCompleteNotification(c: Context, n: String, s: Int) {}
    fun showStreakMilestoneNotification(c: Context, n: String, s: Int) {}
    fun showDailyReminderNotification(c: Context, count: Int) {}
    fun showAchievementNotification(c: Context, t: String, i: String) {}
    fun cancelNotification(context: Context, id: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(id)
        stopAllSounds(context)
    }
}
