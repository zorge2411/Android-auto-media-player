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
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00a2\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\b\u0012\b\u0007\u0018\u0000 V2\u00020\u0001:\u0004VWXYB\u0019\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\b\u00107\u001a\u000208H\u0002J\u0006\u00109\u001a\u000208J\u000e\u0010:\u001a\u0002082\u0006\u0010;\u001a\u000206J:\u0010<\u001a\u0002082\u0006\u0010=\u001a\u00020>2\b\b\u0002\u0010?\u001a\u00020\u001a2\n\b\u0002\u0010@\u001a\u0004\u0018\u00010\u001a2\b\b\u0002\u0010A\u001a\u00020\u001a2\n\b\u0002\u0010B\u001a\u0004\u0018\u00010\u001aJ*\u0010C\u001a\u0002082\u0006\u0010D\u001a\u00020E2\u0006\u0010?\u001a\u00020\u001a2\b\b\u0002\u0010A\u001a\u00020\u001a2\u0006\u0010B\u001a\u00020\u001aH\u0002JB\u0010F\u001a\u0002082\u0006\u0010=\u001a\u00020>2\u0012\u0010G\u001a\u000e\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u001a0H2\b\b\u0002\u0010?\u001a\u00020\u001a2\b\b\u0002\u0010A\u001a\u00020\u001a2\n\b\u0002\u0010B\u001a\u0004\u0018\u00010\u001aJ\u0006\u0010I\u001a\u000208J\u0010\u0010J\u001a\u0002082\b\b\u0002\u0010K\u001a\u00020\u000bJ\u0010\u0010L\u001a\u0002082\b\b\u0002\u0010K\u001a\u00020\u000bJ\u000e\u0010M\u001a\u0002082\u0006\u0010,\u001a\u00020\u000bJ\u0016\u0010N\u001a\u0002082\u0006\u0010O\u001a\u0002012\u0006\u0010P\u001a\u000201J\u000e\u0010Q\u001a\u0002082\u0006\u0010R\u001a\u00020\u0014J\u0018\u0010S\u001a\u0002082\u0006\u0010B\u001a\u00020\u001a2\u0006\u0010A\u001a\u00020\u001aH\u0002J\u0006\u0010T\u001a\u000208J\u0006\u0010U\u001a\u000208R\u0016\u0010\u0007\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\r0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000f0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0011\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00120\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0015\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\t0\u0016\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0010\u0010\u0019\u001a\u0004\u0018\u00010\u001aX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001b\u001a\u0004\u0018\u00010\u001aX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0016\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0018R\u0017\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\r0\u0016\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0018R\u000e\u0010\u001f\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010 \u001a\u00020!X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\"\u001a\u00020#X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010$\u001a\u00020%X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010&\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0016\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010\u0018R\u0011\u0010(\u001a\u00020)\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010+R\u0017\u0010,\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0016\u00a2\u0006\b\n\u0000\u001a\u0004\b-\u0010\u0018R\u0010\u0010.\u001a\u0004\u0018\u00010/X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00100\u001a\u000201X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u00102\u001a\u000201X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0019\u00103\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00120\u0016\u00a2\u0006\b\n\u0000\u001a\u0004\b4\u0010\u0018R\u0010\u00105\u001a\u0004\u0018\u000106X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006Z"}, d2 = {"Lcom/pscholer/autoplayer/player/MediaPlayerManager;", "", "context", "Landroid/content/Context;", "playbackRepository", "Lcom/pscholer/autoplayer/data/PlaybackRepository;", "(Landroid/content/Context;Lcom/pscholer/autoplayer/data/PlaybackRepository;)V", "_currentItem", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$NowPlaying;", "_durationMs", "", "_isPlaying", "", "_playbackState", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$PlaybackState;", "_positionMs", "_videoSize", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$VideoSize;", "activeSurface", "Landroid/view/Surface;", "currentItem", "Lkotlinx/coroutines/flow/StateFlow;", "getCurrentItem", "()Lkotlinx/coroutines/flow/StateFlow;", "currentMediaId", "", "currentSource", "durationMs", "getDurationMs", "isPlaying", "lastSavedPositionMs", "loadControl", "Landroidx/media3/exoplayer/LoadControl;", "managerScope", "Lkotlinx/coroutines/CoroutineScope;", "okHttpClient", "Lokhttp3/OkHttpClient;", "playbackState", "getPlaybackState", "player", "Landroidx/media3/exoplayer/ExoPlayer;", "getPlayer", "()Landroidx/media3/exoplayer/ExoPlayer;", "positionMs", "getPositionMs", "savePositionJob", "Lkotlinx/coroutines/Job;", "surfaceHeight", "", "surfaceWidth", "videoSize", "getVideoSize", "visibleArea", "Landroid/graphics/Rect;", "applyPresentationEffect", "", "clearVideoSurface", "notifyVisibleArea", "area", "play", "uri", "Landroid/net/Uri;", "title", "mimeType", "source", "mediaId", "playMediaItem", "mediaItem", "Landroidx/media3/common/MediaItem;", "playWithHeaders", "headers", "", "release", "seekBack", "ms", "seekForward", "seekTo", "setOutputSize", "width", "height", "setVideoSurface", "surface", "startPeriodicSave", "stop", "togglePlayPause", "Companion", "NowPlaying", "PlaybackState", "VideoSize", "app_debug"})
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
    public final androidx.media3.exoplayer.ExoPlayer getPlayer() {
        return null;
    }
    
    /**
     * Attach the car display Surface to ExoPlayer's video pipeline.
     */
    public final void setVideoSurface(@org.jetbrains.annotations.NotNull()
    android.view.Surface surface) {
    }
    
    /**
     * Detach the surface (called before the Surface is released).
     */
    public final void clearVideoSurface() {
    }
    
    /**
     * Notify of the visible screen area (can be used for layout adjustments).
     */
    public final void notifyVisibleArea(@org.jetbrains.annotations.NotNull()
    android.graphics.Rect area) {
    }
    
    /**
     * Sizes the output frame to [width] x [height] with letterbox/pillarbox bars so the source
     * aspect ratio is preserved. Needed because CarAppService provides a raw Surface; the normal
     * videoScalingMode knob is a no-op without a SurfaceHolder.
     *
     * Called from VideoSurfaceRenderer.onSurfaceAvailable() and onVisibleAreaChanged().
     * The Car App surface persists for the lifetime of the session, so this fires once on
     * session start and again only if the display geometry changes. We store the dimensions
     * here so that each play() call can unconditionally re-apply Presentation effects before
     * prepare() — even when onSurfaceAvailable does not re-fire between plays.
     */
    public final void setOutputSize(int width, int height) {
    }
    
    /**
     * Apply (or re-apply) the Presentation effect to ExoPlayer's video pipeline.
     * Must be called before every prepare() so the new pipeline has valid output dimensions.
     * Safe to call multiple times — ExoPlayer replaces the effect list each time.
     */
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