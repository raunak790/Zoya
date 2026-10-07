package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ZoyaVoiceService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "zoya_voice_service_channel"
        const val NOTIFICATION_ID = 2026
        const val ACTION_START_SERVICE = "com.example.service.ACTION_START"
        const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP"
        const val ACTION_AWAKEN = "com.example.service.ACTION_AWAKEN"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        ZoyaController.initialize(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_AWAKEN -> {
                ZoyaController.awakenZoya(applicationContext)
            }
            else -> {
                startForegroundWithNotification()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ZoyaVoiceService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val awakenIntent = Intent(this, ZoyaVoiceService::class.java).apply {
            action = ACTION_AWAKEN
        }
        val awakenPendingIntent = PendingIntent.getService(
            this,
            2,
            awakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zoya Voice Assistant")
            .setContentText("Listening for 'Hey Zoya' in the background...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_btn_speak_now, "Talk Now", awakenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Zoya Background Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Zoya active and listening for wake-word activation"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Zoya::WakeWordWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L) // 10 minutes timeout, auto-renewed
            }
        }
    }

    private fun stopForegroundService() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
        wakeLock = null

        ZoyaController.stopForegroundService(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
