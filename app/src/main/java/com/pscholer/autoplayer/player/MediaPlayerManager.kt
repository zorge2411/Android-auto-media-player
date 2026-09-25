package com.pscholer.autoplayer.player

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ExoTimeoutException
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import okhttp3.OkHttpClient
import dagger.hilt.android.qualifiers.ApplicationContext
import com.pscholer.autoplayer.data.PlaybackRepository
import com.pscholer.autoplayer.util.AspectRatioCalculator
import com.pscholer.autoplayer.data.models.PlaybackState as PlaybackStateModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Singleton ExoPlayer wrapper; Media3 manages audio focus internally via handleAudioFocus. */
@OptIn(UnstableApi::class)
@Singleton
class MediaPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackRepository: PlaybackRepository
) {
    companion object {
        private const val TAG = "MediaPlayerManager"
        private const val PLAYBACK_SAVE_INTERVAL_MS = 10_000L
        // Timeline refresh rate while playing. ExoPlayer has no position callback during steady
        // playback, so positionMs is polled; VideoPlaybackScreen de-dupes on the formatted second.
        private const val POSITION_TICK_INTERVAL_MS = 1_000L
        // Media3 Presentation effects were removed in Phase 3 Wave 0 — empirically broken
        // on Android Auto's remote Surface (setFrameRate -38 errno in DefaultVideoFrameProcessor).
        // Replaced by GLVideoPipeline (custom EGL14 + OES intermediary). See 3-RESEARCH.md.

        // Any ERROR_CODE_TIMEOUT within this window of a surface teardown is non-fatal and
        // suppressed. ExoPlayer recovers automatically after the timeout completes.
        private const val TEARDOWN_TIMEOUT_WINDOW_MS = 4_000L
    }

    // Timestamp of the last surface teardown call. Used to identify non-fatal timeout errors
    // that fire when the Qualcomm codec cleanup blocks the playback thread for ~2s after a
    // natural video end.
    private var lastSurfaceTeardownMs = 0L

    private var playSequence = 0

    private val managerScope = CoroutineScope(Dispatchers.Main)
    private var currentMediaId: String? = null
    private var currentSource: String? = null
    private var lastSavedPositionMs = -1L
    private var savePositionJob: kotlinx.coroutines.Job? = null
    private var positionTickJob: kotlinx.coroutines.Job? = null

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // ── Playback state ────────────────────────────────────────────────────────
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentItem = MutableStateFlow<NowPlaying?>(null)
    val currentItem: StateFlow<NowPlaying?> = _currentItem.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    data class VideoSize(
        val width: Int,
        val height: Int,
        val pixelAspectRatio: Float = 1.0f,
        val rotationDegrees: Int = 0
    )

    private val _videoSize = MutableStateFlow<VideoSize?>(null)
    val videoSize: StateFlow<VideoSize?> = _videoSize.asStateFlow()

    private var activeSurface: Surface? = null
    private var visibleArea: Rect? = null

    // When play() is called before a Surface is available we store the intent here and
    // execute it the moment setVideoSurface() delivers a real surface from onSurfaceAvailable.
    private var pendingPlay: PendingPlay? = null
    private var pendingSurface: Surface? = null

    private val _scalingMode = MutableStateFlow(AspectRatioCalculator.ScalingMode.FIT)
    val scalingMode: StateFlow<AspectRatioCalculator.ScalingMode> = _scalingMode.asStateFlow()

    private sealed class PendingPlay {
        data class Item(val item: androidx.media3.common.MediaItem) : PendingPlay()
        data class Source(val source: androidx.media3.exoplayer.source.MediaSource) : PendingPlay()
    }

    private fun describeSurface(surface: Surface?): String {
        if (surface == null) return "null"
        return "Surface@${System.identityHashCode(surface).toString(16)}(valid=${surface.isValid})"
    }

    private fun describePendingPlay(pendingPlay: PendingPlay?): String = when (pendingPlay) {
        null -> "none"
        is PendingPlay.Item -> "item"
        is PendingPlay.Source -> "source"
    }

    private fun logPipelineSnapshot(prefix: String) {
        Log.d(
            TAG,
            "$prefix | playerState=${player.playbackState} activeSurface=${describeSurface(activeSurface)} " +
                "pendingSurface=${describeSurface(pendingSurface)} pendingPlay=${describePendingPlay(pendingPlay)} " +
                "outputSize=${surfaceWidth}x${surfaceHeight} visibleArea=$visibleArea"
        )
    }

    // Short playback/rebuffer thresholds let video start quickly on car head units;
    // small back-buffer caps memory in the Automotive environment.
    private val loadControl: LoadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(30_000, 60_000, 1_500, 3_000)
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(5_000, true)
        .build()

    // ── ExoPlayer instance ────────────────────────────────────────────────────
    val player: ExoPlayer = ExoPlayer.Builder(
        context,
        DefaultRenderersFactory(context)
    )
        .setLoadControl(loadControl)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .also { exo ->
            exo.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                    if (playing) {
                        startPositionTicker()
                    } else {
                        stopPositionTicker()
                        // Publish the exact paused position (the last tick may be up to 1s stale)
                        updatePositionAndDuration()
                    }
                }

                override fun onPlaybackStateChanged(state: Int) {
                    logPipelineSnapshot("onPlaybackStateChanged(state=$state)")
                    _playbackState.value = when (state) {
                        Player.STATE_IDLE     -> {
                            pendingSurface?.let { surface ->
                                Log.d(TAG, "Player is now idle. Attaching pending surface.")
                                pendingSurface = null
                                setVideoSurface(surface) // Re-enter to take the idle path
                            }
                            PlaybackState.Idle
                        }
                        Player.STATE_BUFFERING -> PlaybackState.Buffering
                        Player.STATE_READY    -> PlaybackState.Ready
                        Player.STATE_ENDED    -> PlaybackState.Ended
                        else                  -> PlaybackState.Idle
                    }
                    updatePositionAndDuration()
                }

                override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                    updatePositionAndDuration()
                }

                override fun onPlayerError(error: PlaybackException) {
                    // Suppress the ExoTimeoutException that fires when Qualcomm's
                    // c2.qti.avc.decoder cleanup blocks the playback thread for ~2s
                    // after a natural video end. clearVideoSurface() calls
                    // player.clearVideoSurface() during this window → timeout, but
                    // ExoPlayer recovers automatically and the next play works fine.
                    val msSinceTeardown = System.currentTimeMillis() - lastSurfaceTeardownMs
                    if (error.errorCode == PlaybackException.ERROR_CODE_TIMEOUT
                        && msSinceTeardown < TEARDOWN_TIMEOUT_WINDOW_MS
                    ) {
                        Log.w(
                            TAG,
                            "Surface teardown timeout suppressed (${msSinceTeardown}ms after teardown, " +
                                "Qualcomm codec cleanup) — playback will recover automatically"
                        )
                        return
                    }
                    Log.e(TAG, "Playback error [${error.errorCode}]: ${error.message}", error)
                    _playbackState.value = PlaybackState.Error(
                        error.message ?: "Unknown playback error",
                        error.errorCode
                    )
                }

                @Suppress("DEPRECATION")
                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                    Log.d(
                        TAG,
                        "onVideoSizeChanged raw=${videoSize.width}x${videoSize.height} " +
                            "par=${videoSize.pixelWidthHeightRatio} rot=${videoSize.unappliedRotationDegrees} " +
                            "outputSize=${surfaceWidth}x${surfaceHeight} activeSurface=${describeSurface(activeSurface)}"
                    )
                    _videoSize.value = VideoSize(
                        width = videoSize.width,
                        height = videoSize.height,
                        pixelAspectRatio = videoSize.pixelWidthHeightRatio,
                        rotationDegrees = videoSize.unappliedRotationDegrees
                    )
                }

                private fun updatePositionAndDuration() {
                    _positionMs.value = exo.currentPosition.coerceAtLeast(0L)
                    _durationMs.value = exo.duration.coerceAtLeast(0L)
                }
            })
        }

    // ─────────────────────────────────────────────────────────────────────────
    //  Surface management  (called by VideoSurfaceRenderer)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Store the car display Surface and attach it to ExoPlayer, then flush any deferred prepare.
     *
     * INVARIANT enforced by play()/playMediaItem()/playWithHeaders():
     * If activeSurface is null when play() is called, prepare() is NOT called immediately.
     * Instead the MediaItem/MediaSource is stored in [pendingPlay] and this method executes
     * it here — after surface attachment — so the video renderer ALWAYS starts with a real
     * android.view.Surface. This prevents:
     *  1. FinalShaderWrapper "Output surface and size not set" dropping every frame (which
     *     happened when prepare() ran before the surface arrived).
     *  2. ExoTimeoutException in setVideoSurface() (which happened when onSurfaceAvailable
     *     called setVideoSurface() against an already-BUFFERING renderer).
     *
     * When activeSurface IS non-null at play-time, prepare() runs immediately after
     * setVideoSurface() in the play path — the player is in IDLE at that point so there is
     * no active renderer to race against, and the surface detach completes instantly.
     */
    fun setVideoSurface(surface: Surface) {
        Log.d(
            TAG,
            "setVideoSurface requested surface=${describeSurface(surface)} " +
                "currentActive=${describeSurface(activeSurface)} playerState=${player.playbackState} " +
                "outputSize=${surfaceWidth}x${surfaceHeight} pendingPlay=${describePendingPlay(pendingPlay)}"
        )

        // Guard: duplicate callback with the same Surface instance (common in Android Auto
        // when visible-area changes trigger a re-deliver of the same surface).
        if (activeSurface == surface) {
            Log.d(TAG, "Surface already attached — ignoring duplicate setVideoSurface")
            return
        }

        // If player is not idle, store the surface and stop the player first.
        // The onPlaybackStateChanged listener will re-call this function once IDLE.
        if (player.playbackState != Player.STATE_IDLE) {
            Log.w(TAG, "Player is not idle (state=${player.playbackState}). Stopping and deferring surface attach.")
            pendingSurface = surface
            player.stop()
            return
        }

        // Player is IDLE — safe to attach the surface without racing the renderer.
        Log.d(TAG, "Attaching video surface (player state=${player.playbackState})")
        activeSurface = surface

        player.setVideoSurface(surface)
        logPipelineSnapshot("setVideoSurface completed")

        // If a play() call was deferred (surface wasn't available when play was requested),
        // execute it now that both surface and IDLE state are guaranteed.
        val deferred = pendingPlay ?: return
        pendingPlay = null
        Log.d(TAG, "Executing deferred prepare now that surface is available")
        when (deferred) {
            is PendingPlay.Item   -> player.apply { setMediaItem(deferred.item);   prepare(); playWhenReady = true }
            is PendingPlay.Source -> player.apply { setMediaSource(deferred.source); prepare(); playWhenReady = true }
        }
    }


    /**
     * Called from VideoSurfaceRenderer.onSurfaceDestroyed — the car host is about to
     * destroy the surface, so we must stop rendering immediately.
     *
     * Calls `player.stop()` to release the codec; the Qualcomm 2s cleanup hang is handled
     * by `lastSurfaceTeardownMs` suppression in `onPlayerError`. The next `setVideoSurface()`
     * call will attach a new surface from IDLE.
     */
    fun clearVideoSurface() {
        Log.d(TAG, "Surface destroyed — stopping player")
        logPipelineSnapshot("clearVideoSurface start")
        pendingPlay = null    // discard any deferred prepare — the surface is gone
        pendingSurface = null // discard any deferred surface — it's about to be destroyed
        // Record timestamp so onPlayerError can suppress the ~2s timeout that fires when
        // Qualcomm codec cleanup is already running (natural-video-end case).
        lastSurfaceTeardownMs = System.currentTimeMillis()
        player.stop()
        activeSurface = null
        logPipelineSnapshot("clearVideoSurface done")
    }

    /** Notify of the visible screen area (can be used for layout adjustments). */
    fun notifyVisibleArea(area: Rect) {
        visibleArea = area
    }

    // Surface dimensions reported by the car display — survive across stop()/play() cycles
    // because the physical surface does not change between VideoPlaybackScreen navigations.
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    fun setOutputSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val oldWidth = surfaceWidth
        val oldHeight = surfaceHeight
        surfaceWidth = width
        surfaceHeight = height
        Log.d(
            TAG,
            "setOutputSize ${oldWidth}x${oldHeight} -> ${surfaceWidth}x${surfaceHeight} " +
                "activeSurface=${describeSurface(activeSurface)} playerState=${player.playbackState}"
        )
    }

    fun setScalingMode(mode: AspectRatioCalculator.ScalingMode) {
        if (_scalingMode.value == mode) return
        _scalingMode.value = mode
        Log.i(TAG, "Scaling mode set to $mode (delegated to GLVideoPipeline via VideoSurfaceRenderer)")
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Playback control
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Start playback of a simple URL with optional MIME type.
     * Uses ExoPlayer's built-in progressive / adaptive detection.
     */
    fun play(uri: Uri, title: String = "", mimeType: String? = null, source: String = "LOCAL", mediaId: String? = null) {
        val itemBuilder = MediaItem.Builder().setUri(uri)
        mimeType?.let { itemBuilder.setMimeType(it) }

        playMediaItem(itemBuilder.build(), title, source, mediaId ?: uri.toString())
    }

    /**
     * Start playback with custom HTTP headers (required for Plex/Jellyfin auth).
     * Uses OkHttpDataSource for better performance and DefaultMediaSourceFactory
     * for adaptive stream support.
     */
    fun playWithHeaders(uri: Uri, headers: Map<String, String>, title: String = "", source: String = "LOCAL", mediaId: String? = null) {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(headers)
            .setUserAgent("AutoPlayer/1.0 (Android Auto)")

        // Use DefaultMediaSourceFactory to support HLS/Dash if Jellyfin provides them,
        // while still using our custom dataSourceFactory with headers.
        val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))

        val seq = ++playSequence
        Log.i(TAG, "=== PLAY #$seq (headers) === $uri")
        Log.i(TAG, "  activeSurface=${describeSurface(activeSurface)} valid=${activeSurface?.isValid}")
        Log.i(TAG, "  pendingPlay=${describePendingPlay(pendingPlay)} pendingSurface=${describeSurface(pendingSurface)}")
        Log.i(TAG, "  playerState=${player.playbackState} surfaceSize=${surfaceWidth}x${surfaceHeight}")
        _currentItem.value = NowPlaying(uri.toString(), title)

        // Reset to IDLE before preparing a new item. If the player is in ERROR state (e.g.
        // after a surface-detach crash), calling prepare() without stop() is a no-op and
        // nothing plays. stop() transitions ERROR → IDLE so prepare() works every time.
        player.stop()

        if (activeSurface == null) {
            // Surface not yet available (onSurfaceAvailable has not fired yet, or fired after
            // onSurfaceDestroyed nulled activeSurface). Defer prepare() until the surface
            // arrives in setVideoSurface(). This prevents FinalShaderWrapper from dropping
            // every decoded frame AND prevents ExoTimeoutException when setVideoSurface() is
            // later called against an already-BUFFERING renderer.
            Log.d(TAG, "Surface not yet available — deferring prepare() until onSurfaceAvailable")
            pendingPlay = PendingPlay.Source(mediaSource)
            // Start periodic-save now so position tracking begins once playback starts
            startPeriodicSave(mediaId ?: uri.toString(), source)
            return
        }

        // Re-attach surface BEFORE prepare(). This is safe here because activeSurface is non-null
        // and the player is in IDLE (stop() above), so no renderer is active to race against.
        val surface = activeSurface
        if (surface == null || !surface.isValid) {
            Log.e(TAG, "DIAGNOSTIC #$seq: activeSurface is ${if (surface == null) "null" else "invalid"} — forcing deferred path")
            activeSurface = null
            pendingPlay = PendingPlay.Source(mediaSource)
            startPeriodicSave(mediaId ?: uri.toString(), source)
            return
        }
        player.setVideoSurface(surface)

        player.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }

        // Start saving playback position periodically
        startPeriodicSave(mediaId ?: uri.toString(), source)
    }

    private fun playMediaItem(mediaItem: MediaItem, title: String, source: String = "LOCAL", mediaId: String) {
        val seq = ++playSequence
        Log.i(TAG, "=== PLAY #$seq === ${mediaItem.localConfiguration?.uri}")
        Log.i(TAG, "  activeSurface=${describeSurface(activeSurface)} valid=${activeSurface?.isValid}")
        Log.i(TAG, "  pendingPlay=${describePendingPlay(pendingPlay)} pendingSurface=${describeSurface(pendingSurface)}")
        Log.i(TAG, "  playerState=${player.playbackState} surfaceSize=${surfaceWidth}x${surfaceHeight}")
        logPipelineSnapshot("playMediaItem entry")
        _currentItem.value = NowPlaying(
            mediaItem.localConfiguration?.uri?.toString() ?: "",
            title
        )

        // Reset to IDLE before preparing a new item. If the player is in ERROR state (e.g.
        // after a surface-detach crash), calling prepare() without stop() is a no-op and
        // nothing plays. stop() transitions ERROR → IDLE so prepare() works every time.
        player.stop()

        if (activeSurface == null) {
            // Surface not yet available (onSurfaceAvailable has not fired yet, or fired after
            // onSurfaceDestroyed nulled activeSurface). Defer prepare() until the surface
            // arrives in setVideoSurface(). This prevents FinalShaderWrapper from dropping
            // every decoded frame AND prevents ExoTimeoutException when setVideoSurface() is
            // later called against an already-BUFFERING renderer.
            Log.d(TAG, "Surface not yet available — deferring prepare() until onSurfaceAvailable")
            pendingPlay = PendingPlay.Item(mediaItem)
            // Start periodic-save now so position tracking begins once playback starts
            startPeriodicSave(mediaId, source)
            return
        }

        // Re-attach surface BEFORE prepare(). This is safe here because activeSurface is non-null
        // and the player is in IDLE (stop() above), so no renderer is active to race against.
        val surface = activeSurface
        if (surface == null || !surface.isValid) {
            Log.e(TAG, "DIAGNOSTIC #$seq: activeSurface is ${if (surface == null) "null" else "invalid"} — forcing deferred path")
            activeSurface = null
            pendingPlay = PendingPlay.Item(mediaItem)
            startPeriodicSave(mediaId, source)
            return
        }
        player.setVideoSurface(surface)

        player.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }

        // Start saving playback position periodically
        startPeriodicSave(mediaId, source)
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekForward(ms: Long = 10_000L) {
        player.seekTo(player.currentPosition + ms)
    }

    fun seekBack(ms: Long = 10_000L) {
        player.seekTo(maxOf(0L, player.currentPosition - ms))
    }

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun stop() {
        // Cancel before resetting positionMs so a pending tick cannot overwrite the reset
        stopPositionTicker()
        player.stop()
        player.clearMediaItems()
        _currentItem.value = null
        _positionMs.value = 0L
        _durationMs.value = 0L
        currentMediaId = null
        currentSource = null
        savePositionJob?.cancel()
        // surfaceWidth/surfaceHeight are intentionally preserved so that the next play() call
        // can apply Presentation effects before prepare() even when onSurfaceAvailable does
        // not re-fire (the surface persists across VideoPlaybackScreen navigations in a session).
    }

    private fun startPositionTicker() {
        positionTickJob?.cancel()
        positionTickJob = managerScope.launch {
            while (true) {
                _positionMs.value = player.currentPosition.coerceAtLeast(0L)
                _durationMs.value = player.duration.coerceAtLeast(0L)
                kotlinx.coroutines.delay(POSITION_TICK_INTERVAL_MS)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickJob?.cancel()
        positionTickJob = null
    }

    private fun startPeriodicSave(mediaId: String, source: String) {
        currentMediaId = mediaId
        currentSource = source
        lastSavedPositionMs = -1L
        savePositionJob?.cancel()
        savePositionJob = managerScope.launch {
            while (true) {
                kotlinx.coroutines.delay(PLAYBACK_SAVE_INTERVAL_MS)
                val id = currentMediaId ?: continue
                val src = currentSource ?: continue
                if (!player.isPlaying || player.duration <= 0) continue

                val position = player.currentPosition
                val duration = player.duration
                val state = PlaybackStateModel(id, src, position, duration)
                if (state.isResumable()) {
                    if (position != lastSavedPositionMs) {
                        playbackRepository.save(state)
                        lastSavedPositionMs = position
                    }
                } else {
                    playbackRepository.delete(id)
                }
            }
        }
    }

    fun release() {
        stopPositionTicker()
        savePositionJob?.cancel()
        managerScope.cancel()
        player.release()
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  State models
    // ─────────────────────────────────────────────────────────────────────────

    sealed class PlaybackState {
        object Idle : PlaybackState()
        object Buffering : PlaybackState()
        object Ready : PlaybackState()
        object Ended : PlaybackState()
        data class Error(val message: String, val code: Int) : PlaybackState()
    }

    data class NowPlaying(val url: String, val title: String)
}
