package com.pscholer.autoplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;
import androidx.core.app.NotificationCompat;
import com.pscholer.autoplayer.BuildConfig;
import com.pscholer.autoplayer.R;
import com.pscholer.autoplayer.car.AutoMediaSession;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Entry point for the Android Auto connection.
 *
 * Host validation strategy:
 * - DEBUG builds use ALLOW_ALL_HOSTS_VALIDATOR so the app can be sideloaded and tested
 *   without Play Store signing.
 * - RELEASE builds use the standard Google allowlist (Google Play Services, Android
 *   Automotive OS, Samsung Driving Mode, etc.) required for Play Store distribution.
 */
@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0007\u0018\u0000 \u00132\u00020\u0001:\u0001\u0013B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H\u0002J\b\u0010\u0005\u001a\u00020\u0006H\u0016J\b\u0010\u0007\u001a\u00020\bH\u0002J\b\u0010\t\u001a\u00020\bH\u0016J\b\u0010\n\u001a\u00020\u000bH\u0016J\"\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000f2\u0006\u0010\u0010\u001a\u00020\r2\u0006\u0010\u0011\u001a\u00020\rH\u0016J\b\u0010\u0012\u001a\u00020\bH\u0002\u00a8\u0006\u0014"}, d2 = {"Lcom/pscholer/autoplayer/service/AutoMediaService;", "Landroidx/car/app/CarAppService;", "()V", "buildNotification", "Landroid/app/Notification;", "createHostValidator", "Landroidx/car/app/validation/HostValidator;", "createNotificationChannel", "", "onCreate", "onCreateSession", "Landroidx/car/app/Session;", "onStartCommand", "", "intent", "Landroid/content/Intent;", "flags", "startId", "promoteForeground", "Companion", "app_debug"})
public final class AutoMediaService extends androidx.car.app.CarAppService {
    private static final int NOTIFICATION_ID = 1001;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CHANNEL_ID = "auto_media_playback";
    @org.jetbrains.annotations.NotNull()
    public static final com.pscholer.autoplayer.service.AutoMediaService.Companion Companion = null;
    
    public AutoMediaService() {
        super();
    }
    
    @java.lang.Override()
    public void onCreate() {
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
    private final void promoteForeground() {
    }
    
    @java.lang.Override()
    public int onStartCommand(@org.jetbrains.annotations.Nullable()
    android.content.Intent intent, int flags, int startId) {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public androidx.car.app.validation.HostValidator createHostValidator() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public androidx.car.app.Session onCreateSession() {
        return null;
    }
    
    private final void createNotificationChannel() {
    }
    
    private final android.app.Notification buildNotification() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/pscholer/autoplayer/service/AutoMediaService$Companion;", "", "()V", "CHANNEL_ID", "", "NOTIFICATION_ID", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}