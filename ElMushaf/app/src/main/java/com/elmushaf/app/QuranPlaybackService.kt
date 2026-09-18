package com.elmushaf.app

import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class QuranPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    override fun onCreate() {
        super.onCreate()
        val intent = android.content.Intent(this, MainActivity::class.java)
        val pending = android.app.PendingIntent.getActivity(this, 0, intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
        val manager = getSystemService(android.app.NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(android.app.NotificationChannel("quran_playback", "تلاوة القرآن",
                android.app.NotificationManager.IMPORTANCE_LOW))
        }
        // Enter foreground immediately, even if network preparation is still buffering.
        startForeground(1001, NotificationCompat.Builder(this, "quran_playback")
            .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("الاستماع للقرآن")
            .setContentText("جاري تجهيز التلاوة…").setContentIntent(pending)
            .setCategory(android.app.Notification.CATEGORY_TRANSPORT).setOngoing(true).build())
        session = MediaSession.Builder(this, QuranAudioPlayer.shared(this).player)
            .setSessionActivity(pending).build()
        addSession(session!!)
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session
    override fun onDestroy() {
        session?.release(); session = null
        QuranAudioPlayer.releaseShared()
        super.onDestroy()
    }
}
