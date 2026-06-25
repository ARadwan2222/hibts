package com.yourname.habitapp.utils

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import com.yourname.habitapp.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var volumeReceiver: BroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val todoId = intent?.getIntExtra("TODO_ID", -1) ?: -1
        val notificationId = intent?.getIntExtra("NOTIFICATION_ID", -1) ?: -1
        val uriString = intent?.getStringExtra("TONE_URI")

        when (action) {
            "ACTION_PLAY" -> {
                uriString?.let { playSound(it) }
                registerVolumeReceiver(todoId)
            }
            "ACTION_STOP", "ACTION_DONE" -> {
                // Cancel notification if ID is provided
                if (notificationId != -1) {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(notificationId)
                }
                
                stopSoundAndMute(todoId, action == "ACTION_DONE")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun playSound(uriString: String) {
        try {
            stopMediaPlayer()
            val uri = android.net.Uri.parse(uriString)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@SoundService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun stopSoundAndMute(todoId: Int, markCompleted: Boolean) {
        stopMediaPlayer()
        if (todoId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getInstance(this@SoundService)
                val todo = db.todoDao().getTodoById(todoId)
                todo?.let { 
                    db.todoDao().update(it.copy(
                        isMuted = true,
                        isCompleted = if (markCompleted) true else it.isCompleted
                    )) 
                }
            }
        }
    }

    private fun stopMediaPlayer() {
        mediaPlayer?.apply {
            try { if (isPlaying) stop() } catch (e: Exception) {}
            release()
        }
        mediaPlayer = null
    }

    private fun registerVolumeReceiver(todoId: Int) {
        if (volumeReceiver != null) return
        volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                stopSoundAndMute(todoId, false)
                stopSelf()
            }
        }
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(volumeReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(volumeReceiver, filter)
        }
    }

    override fun onDestroy() {
        stopMediaPlayer()
        volumeReceiver?.let { try { unregisterReceiver(it) } catch (e: Exception) {} }
        super.onDestroy()
    }
}
