package com.reactivate.caraudiopro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.IBinder

/** App screen band ho tab bhi processing chalti rahe. */
class CarAudioService : Service() {

    companion object {
        fun start(ctx: Context) {
            val i = Intent(ctx, CarAudioService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
    }

    private val sessionRx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            val s = i.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
            if (s <= 0) return
            val pkg = i.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "?"
            if (i.action == AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION) AudioEngine.onSessionOpen(s, pkg)
            else AudioEngine.onSessionClose(s)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, notification())
        val f = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(sessionRx, f, Context.RECEIVER_EXPORTED)
        else registerReceiver(sessionRx, f)
        AudioEngine.init(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AudioEngine.current?.let { AudioEngine.apply(it) }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(sessionRx) }
        AudioEngine.release()
        super.onDestroy()
    }

    private fun notification(): Notification {
        val b = if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel("engine", "Audio Engine", NotificationManager.IMPORTANCE_LOW))
            Notification.Builder(this, "engine")
        } else @Suppress("DEPRECATION") Notification.Builder(this)
        return b.setContentTitle("Car Audio Pro active")
            .setSmallIcon(android.R.drawable.ic_media_play).setOngoing(true).build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
