package com.yourname.habitapp.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", -1)
        val todoId = intent.getIntExtra("TODO_ID", -1)

        if (notificationId != -1) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notificationId)
        }
        
        // Use the ID from the intent to stop the sound service and mute the task
        NotificationHelper.stopAllSounds(context, todoId)
    }
}
