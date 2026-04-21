package com.pscholer.autoplayer.player;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.effect.Presentation;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.ExoTimeoutException;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import okhttp3.OkHttpClient;
import dagger.hilt.android.qualifiers.ApplicationContext;
import com.pscholer.autoplayer.data.PlaybackRepository;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.Dispatchers;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.pscholer.autoplayer.data.models.PlaybackState;

/**
 * Singleton ExoPlayer wrapper; Media3 manages audio focus internally via handleAudioFocus.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00b0\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\b\u0016\b\u0007\u0018\u0000 ^2\u00020\u0001:\u0006^_`abcB\u0019\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\b\u0010=\u001a\u00020>H\u0002J\u0006\u0010?\u001a\u00020>J\u000e\u0010@\u001a\u00020>2\u0006\u0010A\u001a\u00020<J:\u0010B\u001a\u00020>2\u0006\u0010C\u001a\u00020D2\b\b\u0002\u0010E\u001a\u00020\u001c2\n\b\u0002\u0010F\u001a\u0004\u0018\u00010\u001c2\b\b\u0002\u0010G\u001a\u00020\u001c2\n\b\u0002\u0010H\u001a\u0004\u0018\u00010\u001cJ*\u0010I\u001a\u00020>2\u0006\u0010J\u001a\u00020K2\u0006\u0010E\u001a\u00020\u001c2\b\b\u0002\u0010G\u001a\u00020\u001c2\u0006\u0010H\u001a\u00020\u001cH\u0002JB\u0010L\u001a\u00020>2\u0006\u0010C\u001a\u00020D2\u0012\u0010M\u001a\u000e\u0012\u0004\u0012\u00020\u001c\u0012\u0004\u0012\u00020\u001c0N2\b\b\u0002\u0010E\u001a\u00020\u001c2\b\b\u0002\u0010G\u001a\u00020\u001c2\n\b\u0002\u0010H\u001a\u0004\u0018\u00010\u001cJ\u0006\u0010O\u001a\u00020>J\u0010\u0010P\u001a\u00020>2\b\b\u0002\u0010Q\u001a\u00020\u000bJ\u0010\u0010R\u001a\u00020>2\b\b\u0002\u0010Q\u001a\u00020\u000bJ\u000e\u0010S\u001a\u00020>2\u0006\u00100\u001a\u00020\u000bJ\u0016\u0010T\u001a\u00020>2\u0006\u0010U\u001a\u0002072\u0006\u0010V\u001a\u000207J\u000e\u0010W\u001a\u00020>2\u0006\u0010X\u001a\u00020\u0012J\u000e\u0010Y\u001a\u00020>2\u0006\u0010Z\u001a\u00020\u0016J\u0018\u0010[\u001a\u00020>2\u0006\u0010H\u001a\u00020\u001c2\u0006\u0010G\u001a\u00020\u001cH\u0002J\u0006\u0010\\\u001a\u00020>J\u0006\u0010]\u001a\u00020>R\u0016\u0010\u0007\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00120\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0013\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00140\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0017\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0010\u0010\u001b\u001a\u0004\u0018\u00010\u001cX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001d\u001a\u0004\u0018\u00010\u001cX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u001aR\u0017\u0010 \u001a\b\u0012\u0004\u0012\u00020\r0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u001aR\u000e\u0010!\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\"\u001a\u00020#X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010$\u001a\u00020%X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010&\u001a\u00020\'X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010(\u001a\u0004\u0018\u00010)X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010*\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010\u001aR\u0011\u0010,\u001a\u00020-\u00a2\u0006\b\n\u0000\u001a\u0004\b.\u0010/R\u0017\u00100\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b1\u0010\u001aR\u0010\u00102\u001a\u0004\u0018\u000103X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u00104\u001a\b\u0012\u0004\u0012\u00020\u00120\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b5\u0010\u001aR\u000e\u00106\u001a\u000207X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00108\u001a\u000207X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0019\u00109\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00140\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b:\u0010\u001aR\u0010\u0010;\u001a\u0004\u0018\u00010<X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006d"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager;", "", "context", "Landroid/content/Context;", "playbackRepository", "Lcom/pscholer/autoplayer/data/PlaybackRepository;", "(Landroid/content/Context;Lcom/pscholer/autoplayer/data/PlaybackRepository;)V", "_currentItem", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$NowPlaying;", "_durationMs", "", "_isPlaying", "", "_playbackState", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState;", "_positionMs", "_scalingMode", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$ScalingMode;", "_videoSize", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$VideoSize;", "activeSurface", "Landroid/view/Surface;", "currentItem", "Lkotlinx/coroutines/flow/StateFlow;", "getCurrentItem", "()Lkotlinx/coroutines/flow/StateFlow;", "currentMediaId", "", "currentSource", "durationMs", "getDurationMs", "isPlaying", "lastSavedPositionMs", "loadControl", "Landroidx/media3/exoplayer/LoadControl;", "managerScope", "Lkotlinx/coroutines/CoroutineScope;", "okHttpClient", "Lokhttp3/OkHttpClient;", "pendingPlay", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PendingPlay;", "playbackState", "getPlaybackState", "player", "Landroidx/media3/exoplayer/ExoPlayer;", "getPlayer", "()Landroidx/media3/exoplayer/ExoPlayer;", "positionMs", "getPositionMs", "savePositionJob", "Lkotlinx/coroutines/Job;", "scalingMode", "getScalingMode", "surfaceHeight", "", "surfaceWidth", "videoSize", "getVideoSize", "visibleArea", "Landroid/graphics/Rect;", "applyPresentationEffect", "", "clearVideoSurface", "notifyVisibleArea", "area", "play", "uri", "Landroid/net/Uri;", "title", "mimeType", "source", "mediaId", "playMediaItem", "mediaItem", "Landroidx/media3/common/MediaItem;", "playWithHeaders", "headers", "", "release", "seekBack", "ms", "seekForward", "seekTo", "setOutputSize", "width", "height", "setScalingMode", "mode", "setVideoSurface", "surface", "startPeriodicSave", "stop", "togglePlayPause", "Companion", "NowPlaying", "PendingPlay", "PlaybackState", "ScalingMode", "VideoSize", "app_debug"})
@androidx.annotation.OptIn(markerClass = {androidx.media3.common.util.UnstableApi.class})
public final class MediaPlayerManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.data.PlaybackRepository playbackRepository = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "MediaPlayerManager";
    private static final long PLAYBACK_SAVE_INTERVAL_MS = 10000L;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope managerScope = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String currentMediaId;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String currentSource;
    private long lastSavedPositionMs = -1L;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job savePositionJob;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient okHttpClient = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState> _playbackState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState> playbackState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isPlaying = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPlaying = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pscholer.autoplayer.player.MediaPlayerManager.NowPlaying> _currentItem = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.player.MediaPlayerManager.NowPlaying> currentItem = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Long> _positionMs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Long> positionMs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Long> _durationMs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Long> durationMs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pscholer.autoplayer.player.MediaPlayerManager.VideoSize> _videoSize = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.player.MediaPlayerManager.VideoSize> videoSize = null;
    @org.jetbrains.annotations.Nullable()
    private android.view.Surface activeSurface;
    @org.jetbrains.annotations.Nullable()
    private android.graphics.Rect visibleArea;
    @org.jetbrains.annotations.Nullable()
    private com.pscholer.autoplayer.player.MediaPlayerManager.PendingPlay pendingPlay;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pscholer.autoplayer.player.MediaPlayerManager.ScalingMode> _scalingMode = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.player.MediaPlayerManager.ScalingMode> scalingMode = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.media3.exoplayer.LoadControl loadControl = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.media3.exoplayer.ExoPlayer player = null;
    private int surfaceWidth = 0;
    private int surfaceHeight = 0;
    @org.jetbrains.annotations.NotNull()
    public static final com.pscholer.autoplayer.player.MediaPlayerManager.Companion Companion = null;
    
    @javax.inject.Inject()
    public MediaPlayerManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.data.PlaybackRepository playbackRepository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState> getPlaybackState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPlaying() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.player.MediaPlayerManager.NowPlaying> getCurrentItem() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Long> getPositionMs() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Long> getDurationMs() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.player.MediaPlayerManager.VideoSize> getVideoSize() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.player.MediaPlayerManager.ScalingMode> getScalingMode() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.media3.exoplayer.ExoPlayer getPlayer() {
        return null;
    }
    
    /**
     * Store the car display Surface and attach it to ExoPlayer, then flush any deferred prepare.
     *
     * INVARIANT enforced by play()/playMediaItem()/playWithHeaders():
     * If activeSurface is null when play() is called, prepare() is NOT called immediately.
     * Instead the MediaItem/MediaSource is stored in [pendingPlay] and this method executes
     * it here — after surface attachment — so the video renderer ALWAYS starts with a real
     * android.view.Surface. This prevents:
     * 1. FinalShaderWrapper "Output surface and size not set" dropping every frame (which
     *    happened when prepare() ran before the surface arrived).
     * 2. ExoTimeoutException in setVideoSurface() (which happened when onSurfaceAvailable
     *    called setVideoSurface() against an already-BUFFERING renderer).
     *
     * When activeSurface IS non-null at play-time, prepare() runs immediately after
     * setVideoSurface() in the play path — the player is in IDLE at that point so there is
     * no active renderer to race against, and the surface detach completes instantly.
     */
    public final void setVideoSurface(@org.jetbrains.annotations.NotNull()
    android.view.Surface surface) {
    }
    
    /**
     * Called from VideoSurfaceRenderer.onSurfaceDestroyed — the car host is about to
     * destroy the surface, so we must stop rendering immediately.
     *
     * WHY we do NOT call player.clearVideoSurface() here:
     * clearVideoSurface() is a BLOCKING call — it sends a synchronous message to
     * ExoPlayer's playback thread and awaits a reply within a fixed timeout.
     * player.stop() is fire-and-forget (posts to the playback thread, returns at once).
     * Because stop() returns before the playback thread processes it, calling
     * clearVideoSurface() right after still races against an active renderer → timeout:
     *  ExoTimeoutException: Detaching surface timed out.
     *
     * The correct approach for Android Auto:
     * - Call player.stop() — ExoPlayer transitions to IDLE asynchronously; all rendering
     *   ceases. The playback thread will discover the surface is gone on its own.
     * - Null activeSurface so no future play() accidentally re-attaches a dead surface.
     * - The car host destroys the actual Surface AFTER this callback returns, so there
     *   is no window where ExoPlayer could write to an already-freed buffer.
     * - When onSurfaceAvailable fires for a new session, setVideoSurface(newSurface) +
     *   prepare() rebuild the pipeline from scratch.
     */
    public final void clearVideoSurface() {
    }
    
    /**
     * Notify of the visible screen area (can be used for layout adjustments).
     */
    public final void notifyVisibleArea(@org.jetbrains.annotations.NotNull()
    android.graphics.Rect area) {
    }
    
    public final void setOutputSize(int width, int height) {
    }
    
    /**
     * Set the scaling mode. Soft-restarts the player if playback is active.
     */
    public final void setScalingMode(@org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.player.MediaPlayerManager.ScalingMode mode) {
    }
    
    private final void applyPresentationEffect() {
    }
    
    /**
     * Start playback of a simple URL with optional MIME type.
     * Uses ExoPlayer's built-in progressive / adaptive detection.
     */
    public final void play(@org.jetbrains.annotations.NotNull()
    android.net.Uri uri, @org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.Nullable()
    java.lang.String mimeType, @org.jetbrains.annotations.NotNull()
    java.lang.String source, @org.jetbrains.annotations.Nullable()
    java.lang.String mediaId) {
    }
    
    /**
     * Start playback with custom HTTP headers (required for Plex/Jellyfin auth).
     * Uses OkHttpDataSource for better performance and DefaultMediaSourceFactory
     * for adaptive stream support.
     */
    public final void playWithHeaders(@org.jetbrains.annotations.NotNull()
    android.net.Uri uri, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> headers, @org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String source, @org.jetbrains.annotations.Nullable()
    java.lang.String mediaId) {
    }
    
    private final void playMediaItem(androidx.media3.common.MediaItem mediaItem, java.lang.String title, java.lang.String source, java.lang.String mediaId) {
    }
    
    public final void togglePlayPause() {
    }
    
    public final void seekForward(long ms) {
    }
    
    public final void seekBack(long ms) {
    }
    
    public final void seekTo(long positionMs) {
    }
    
    public final void stop() {
    }
    
    private final void startPeriodicSave(java.lang.String mediaId, java.lang.String source) {
    }
    
    public final void release() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$Companion;", "", "()V", "PLAYBACK_SAVE_INTERVAL_MS", "", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005J\t\u0010\t\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\n\u001a\u00020\u0003H\u00c6\u0003J\u001d\u0010\u000b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001J\t\u0010\u0011\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0007\u00a8\u0006\u0012"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$NowPlaying;", "", "url", "", "title", "(Ljava/lang/String;Ljava/lang/String;)V", "getTitle", "()Ljava/lang/String;", "getUrl", "component1", "component2", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class NowPlaying {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String url = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String title = null;
        
        public NowPlaying(@org.jetbrains.annotations.NotNull()
        java.lang.String url, @org.jetbrains.annotations.NotNull()
        java.lang.String title) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getUrl() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTitle() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pscholer.autoplayer.player.MediaPlayerManager.NowPlaying copy(@org.jetbrains.annotations.NotNull()
        java.lang.String url, @org.jetbrains.annotations.NotNull()
        java.lang.String title) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b2\u0018\u00002\u00020\u0001:\u0002\u0003\u0004B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0002\u0005\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$PendingPlay;", "", "()V", "Item", "Source", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PendingPlay$Item;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PendingPlay$Source;", "app_debug"})
    static abstract class PendingPlay {
        
        private PendingPlay() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$PendingPlay$Item;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PendingPlay;", "item", "Landroidx/media3/common/MediaItem;", "(Landroidx/media3/common/MediaItem;)V", "getItem", "()Landroidx/media3/common/MediaItem;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
        public static final class Item extends com.pscholer.autoplayer.player.MediaPlayerManager.PendingPlay {
            @org.jetbrains.annotations.NotNull()
            private final androidx.media3.common.MediaItem item = null;
            
            public Item(@org.jetbrains.annotations.NotNull()
            androidx.media3.common.MediaItem item) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final androidx.media3.common.MediaItem getItem() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final androidx.media3.common.MediaItem component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pscholer.autoplayer.player.MediaPlayerManager.PendingPlay.Item copy(@org.jetbrains.annotations.NotNull()
            androidx.media3.common.MediaItem item) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$PendingPlay$Source;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PendingPlay;", "source", "Landroidx/media3/exoplayer/source/MediaSource;", "(Landroidx/media3/exoplayer/source/MediaSource;)V", "getSource", "()Landroidx/media3/exoplayer/source/MediaSource;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
        public static final class Source extends com.pscholer.autoplayer.player.MediaPlayerManager.PendingPlay {
            @org.jetbrains.annotations.NotNull()
            private final androidx.media3.exoplayer.source.MediaSource source = null;
            
            public Source(@org.jetbrains.annotations.NotNull()
            androidx.media3.exoplayer.source.MediaSource source) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final androidx.media3.exoplayer.source.MediaSource getSource() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final androidx.media3.exoplayer.source.MediaSource component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pscholer.autoplayer.player.MediaPlayerManager.PendingPlay.Source copy(@org.jetbrains.annotations.NotNull()
            androidx.media3.exoplayer.source.MediaSource source) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0005\u0003\u0004\u0005\u0006\u0007B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0005\b\t\n\u000b\f\u00a8\u0006\r"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState;", "", "()V", "Buffering", "Ended", "Error", "Idle", "Ready", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState$Buffering;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState$Ended;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState$Error;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState$Idle;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState$Ready;", "app_debug"})
    public static abstract class PlaybackState {
        
        private PlaybackState() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState$Buffering;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState;", "()V", "app_debug"})
        public static final class Buffering extends com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState {
            @org.jetbrains.annotations.NotNull()
            public static final com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState.Buffering INSTANCE = null;
            
            private Buffering() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState$Ended;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState;", "()V", "app_debug"})
        public static final class Ended extends com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState {
            @org.jetbrains.annotations.NotNull()
            public static final com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState.Ended INSTANCE = null;
            
            private Ended() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\t\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0003\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\t\u0010\u000b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\f\u001a\u00020\u0005H\u00c6\u0003J\u001d\u0010\r\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0011H\u00d6\u0003J\t\u0010\u0012\u001a\u00020\u0005H\u00d6\u0001J\t\u0010\u0013\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0014"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState$Error;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState;", "message", "", "code", "", "(Ljava/lang/String;I)V", "getCode", "()I", "getMessage", "()Ljava/lang/String;", "component1", "component2", "copy", "equals", "", "other", "", "hashCode", "toString", "app_debug"})
        public static final class Error extends com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String message = null;
            private final int code = 0;
            
            public Error(@org.jetbrains.annotations.NotNull()
            java.lang.String message, int code) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getMessage() {
                return null;
            }
            
            public final int getCode() {
                return 0;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            public final int component2() {
                return 0;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState.Error copy(@org.jetbrains.annotations.NotNull()
            java.lang.String message, int code) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState$Idle;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState;", "()V", "app_debug"})
        public static final class Idle extends com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState {
            @org.jetbrains.annotations.NotNull()
            public static final com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState.Idle INSTANCE = null;
            
            private Idle() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState$Ready;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState;", "()V", "app_debug"})
        public static final class Ready extends com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState {
            @org.jetbrains.annotations.NotNull()
            public static final com.pscholer.autoplayer.player.MediaPlayerManager.PlaybackState.Ready INSTANCE = null;
            
            private Ready() {
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005\u00a8\u0006\u0006"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$ScalingMode;", "", "(Ljava/lang/String;I)V", "FIT", "FILL", "STRETCH", "app_debug"})
    public static enum ScalingMode {
        /*public static final*/ FIT /* = new FIT() */,
        /*public static final*/ FILL /* = new FILL() */,
        /*public static final*/ STRETCH /* = new STRETCH() */;
        
        ScalingMode() {
        }
        
        @org.jetbrains.annotations.NotNull()
        public static kotlin.enums.EnumEntries<com.pscholer.autoplayer.player.MediaPlayerManager.ScalingMode> getEntries() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u000e\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B)\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\bJ\t\u0010\u000f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0010\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0011\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0003H\u00c6\u0003J1\u0010\u0013\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0017\u001a\u00020\u0003H\u00d6\u0001J\t\u0010\u0018\u001a\u00020\u0019H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0007\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\nR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\n\u00a8\u0006\u001a"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager$VideoSize;", "", "width", "", "height", "pixelAspectRatio", "", "rotationDegrees", "(IIFI)V", "getHeight", "()I", "getPixelAspectRatio", "()F", "getRotationDegrees", "getWidth", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "toString", "", "app_debug"})
    public static final class VideoSize {
        private final int width = 0;
        private final int height = 0;
        private final float pixelAspectRatio = 0.0F;
        private final int rotationDegrees = 0;
        
        public VideoSize(int width, int height, float pixelAspectRatio, int rotationDegrees) {
            super();
        }
        
        public final int getWidth() {
            return 0;
        }
        
        public final int getHeight() {
            return 0;
        }
        
        public final float getPixelAspectRatio() {
            return 0.0F;
        }
        
        public final int getRotationDegrees() {
            return 0;
        }
        
        public final int component1() {
            return 0;
        }
        
        public final int component2() {
            return 0;
        }
        
        public final float component3() {
            return 0.0F;
        }
        
        public final int component4() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pscholer.autoplayer.player.MediaPlayerManager.VideoSize copy(int width, int height, float pixelAspectRatio, int rotationDegrees) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}