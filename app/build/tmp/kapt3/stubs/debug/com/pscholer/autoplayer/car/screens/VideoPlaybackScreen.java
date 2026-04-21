package com.pscholer.autoplayer.car.screens;

import android.net.Uri;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.MessageInfo;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.pscholer.autoplayer.R;
import com.pscholer.autoplayer.data.models.MediaItem;
import com.pscholer.autoplayer.data.models.Favorite;
import com.pscholer.autoplayer.di.AppEntryPoint;
import com.pscholer.autoplayer.player.MediaPlayerManager;
import com.pscholer.autoplayer.util.TimeFormatter;
import dagger.hilt.android.EntryPointAccessors;

/**
 * Playback screen — the visual layer during video playback.
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  Car display                                                    │
 * │  ┌───────────────────────────────────────────────────────────┐  │
 * │  │  ExoPlayer video (rendered to Surface by VideoSurface-    │  │
 * │  │  Renderer — sits BEHIND all Car App Library templates)    │  │
 * │  │                                                           │  │
 * │  │  NavigationTemplate overlays minimal controls on top:     │  │
 * │  │    • ActionStrip (right edge): ⏮ ⏯ ⏭ icons              │  │
 * │  │    • MapActionStrip (top-right): ✕ Stop button           │  │
 * │  │                                                           │  │
 * │  └───────────────────────────────────────────────────────────┘  │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * NavigationTemplate is the correct choice here because:
 * 1. It exposes the maximum visible Surface area of any template type.
 * 2. It provides ActionStrip + MapActionStrip for media controls.
 * 3. Its "map" region is fully transparent, letting video show through.
 *
 * The screen calls invalidate() whenever play/pause state changes so the
 * correct icon is displayed on the action strip.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000j\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u001e\u0010#\u001a\u00020$2\u0006\u0010%\u001a\u00020&2\f\u0010\'\u001a\b\u0012\u0004\u0012\u00020)0(H\u0002J\b\u0010*\u001a\u00020\u0018H\u0002J\b\u0010+\u001a\u00020,H\u0016R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\n\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\rR\u001b\u0010\u0010\u001a\u00020\u00118BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0014\u0010\u000f\u001a\u0004\b\u0012\u0010\u0013R\u000e\u0010\u0015\u001a\u00020\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0018X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0019\u001a\u00020\u001a8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001d\u0010\u000f\u001a\u0004\b\u001b\u0010\u001cR\u001b\u0010\u001e\u001a\u00020\u001f8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\"\u0010\u000f\u001a\u0004\b \u0010!\u00a8\u0006-"}, d2 = {"Lcom/pscholer/autoplayer/car/screens/VideoPlaybackScreen;", "Landroidx/car/app/Screen;", "carContext", "Landroidx/car/app/CarContext;", "mediaItem", "Lcom/pscholer/autoplayer/data/models/MediaItem;", "(Landroidx/car/app/CarContext;Lcom/pscholer/autoplayer/data/models/MediaItem;)V", "currentDurationMs", "", "currentPositionMs", "entryPoint", "Lcom/pscholer/autoplayer/di/AppEntryPoint;", "getEntryPoint", "()Lcom/pscholer/autoplayer/di/AppEntryPoint;", "entryPoint$delegate", "Lkotlin/Lazy;", "favoriteRepository", "Lcom/pscholer/autoplayer/data/FavoriteRepository;", "getFavoriteRepository", "()Lcom/pscholer/autoplayer/data/FavoriteRepository;", "favoriteRepository$delegate", "isFavorite", "", "lastFormattedPosition", "", "playbackRepository", "Lcom/pscholer/autoplayer/data/PlaybackRepository;", "getPlaybackRepository", "()Lcom/pscholer/autoplayer/data/PlaybackRepository;", "playbackRepository$delegate", "playerManager", "Lcom/pscholer/autoplayer/player/MediaPlayerManager;", "getPlayerManager", "()Lcom/pscholer/autoplayer/player/MediaPlayerManager;", "playerManager$delegate", "buildAction", "Landroidx/car/app/model/Action;", "iconRes", "", "onClick", "Lkotlin/Function0;", "", "buildTimelineText", "onGetTemplate", "Landroidx/car/app/model/Template;", "app_debug"})
public final class VideoPlaybackScreen extends androidx.car.app.Screen {
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.data.models.MediaItem mediaItem = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy entryPoint$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy playerManager$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy playbackRepository$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy favoriteRepository$delegate = null;
    private long currentPositionMs = 0L;
    private long currentDurationMs = 0L;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String lastFormattedPosition = "";
    private boolean isFavorite = false;
    
    public VideoPlaybackScreen(@org.jetbrains.annotations.NotNull()
    androidx.car.app.CarContext carContext, @org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.data.models.MediaItem mediaItem) {
        super(null);
    }
    
    private final com.pscholer.autoplayer.di.AppEntryPoint getEntryPoint() {
        return null;
    }
    
    private final com.pscholer.autoplayer.player.MediaPlayerManager getPlayerManager() {
        return null;
    }
    
    private final com.pscholer.autoplayer.data.PlaybackRepository getPlaybackRepository() {
        return null;
    }
    
    private final com.pscholer.autoplayer.data.FavoriteRepository getFavoriteRepository() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public androidx.car.app.model.Template onGetTemplate() {
        return null;
    }
    
    private final androidx.car.app.model.Action buildAction(int iconRes, kotlin.jvm.functions.Function0<kotlin.Unit> onClick) {
        return null;
    }
    
    private final java.lang.String buildTimelineText() {
        return null;
    }
}