package com.pscholer.autoplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.core.app.NotificationCompat
import com.pscholer.autoplayer.R
import com.pscholer.autoplayer.car.AutoMediaSession
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point for the Android Auto connection.
 *
 * ALLOW_ALL_HOSTS_VALIDATOR is intentional for a sideloaded app — it bypasses
 * the host signature check that restricts un-approved apps on Play Store builds.
 * Never use this for a production Play Store release.
 */
@AndroidEntryPoint
class AutoMediaService : CarAppService() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "auto_media_playback"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * CarAppService is a bound service. Android Auto binds it via bindService(), which does NOT
     * trigger onStartCommand(). We therefore call startForeground() from onCreate() directly.
     *
     * Why this is required (Android 14+ / targetSdk 35):
     * AudioFlinger checks whether the calling process holds an active foreground service with
     * foregroundServiceType MEDIA_PLAYBACK before granting an audio output route. Without
     * startForeground(), AudioFlinger returns output=0 (EINVAL) and AudioTrack.Builder.build()
     * throws, causing ExoPlaybackException error 5001.
     *
     * All manifest declarations are already correct (foregroundServiceType="mediaPlayback" on
     * the service element, FOREGROUND_SERVICE + FOREGROUND_SERVICE_MEDIA_PLAYBACK permissions).
     * This call is the required runtime counterpart.
     */
    private fun promoteForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session {
        // Promote to foreground here as well: AA binds via bindService() which bypasses
        // onStartCommand(). Calling startForeground() from both paths is safe and idempotent —
        // the OS simply updates the notification on a second call with the same ID.
        promoteForeground()
        return AutoMediaSession()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the media service alive during Android Auto playback"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Playing on Android Auto")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .build()
}
