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
import androidx.media3.effect.Presentation
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
    }

    private val managerScope = CoroutineScope(Dispatchers.Main)
    private var currentMediaId: String? = null
    private var currentSource: String? = null
    private var lastSavedPositionMs = -1L
    private var savePositionJob: kotlinx.coroutines.Job? = null

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

    private val _scalingMode = MutableStateFlow(ScalingMode.FIT)
    val scalingMode: StateFlow<ScalingMode> = _scalingMode.asStateFlow()

    private sealed class PendingPlay {
        data class Item(val item: androidx.media3.common.MediaItem) : PendingPlay()
        data class Source(val source: androidx.media3.exoplayer.source.MediaSource) : PendingPlay()
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
            // videoScalingMode is ignored for raw Surfaces (CarAppService gives us one);
            // aspect-ratio fit is enforced via setVideoEffects(Presentation) in setOutputSize().
            exo.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }

                override fun onPlaybackStateChanged(state: Int) {
                    _playbackState.value = when (state) {
                        Player.STATE_IDLE     -> PlaybackState.Idle
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
                    Log.e(TAG, "Playback error [${error.errorCode}]: ${error.message}", error)
                    _playbackState.value = PlaybackState.Error(
                        error.message ?: "Unknown playback error",
                        error.errorCode
                    )
                }

                @Suppress("DEPRECATION")
                override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
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
        // Guard 1: duplicate callback with the same Surface instance (common in Android Auto
        // when visible-area changes trigger a re-deliver of the same surface).
        if (activeSurface == surface && pendingPlay == null) {
            Log.d(TAG, "Surface already attached — ignoring duplicate setVideoSurface")
            return
        }

        // Guard 2: if we are replacing a live surface while the renderer is active, stop first.
        // In normal lifecycle onSurfaceDestroyed calls clearVideoSurface() which stops and nulls
        // activeSurface before a new surface arrives. If the host breaks that invariant,
        // detaching an active renderer surface causes ExoTimeoutException.
        if (activeSurface != null && activeSurface != surface &&
            player.playbackState != Player.STATE_IDLE) {
            Log.w(TAG, "Replacing active surface while player is active — stopping first")
            player.stop()
        }

        Log.d(TAG, "Attaching video surface (player state=${player.playbackState})")
        activeSurface = surface

        // Apply effects BEFORE attaching surface. Our logs show that when setVideoEffects()
        // is called after setVideoSurface() the FinalShaderWrapper never receives the output
        // surface on Qualcomm Automotive decoders and drops every frame. Creating the effects
        // pipeline first and then delivering the surface appears to initialise the wrapper
        // correctly.
        applyPresentationEffect()
        player.setVideoSurface(surface)

        // If a play() call arrived before the surface was ready, execute it now.
        // The player is in IDLE state (stop() was called before prepare() was deferred),
        // so setVideoSurface() + prepare() here cannot race with an active renderer.
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
     * WHY we do NOT call player.clearVideoSurface() here:
     * clearVideoSurface() is a BLOCKING call — it sends a synchronous message to
     * ExoPlayer's playback thread and awaits a reply within a fixed timeout.
     * player.stop() is fire-and-forget (posts to the playback thread, returns at once).
     * Because stop() returns before the playback thread processes it, calling
     * clearVideoSurface() right after still races against an active renderer → timeout:
     *   ExoTimeoutException: Detaching surface timed out.
     *
     * The correct approach for Android Auto:
     *  - Call player.stop() — ExoPlayer transitions to IDLE asynchronously; all rendering
     *    ceases. The playback thread will discover the surface is gone on its own.
     *  - Null activeSurface so no future play() accidentally re-attaches a dead surface.
     *  - The car host destroys the actual Surface AFTER this callback returns, so there
     *    is no window where ExoPlayer could write to an already-freed buffer.
     *  - When onSurfaceAvailable fires for a new session, setVideoSurface(newSurface) +
     *    prepare() rebuild the pipeline from scratch.
     */
    fun clearVideoSurface() {
        Log.d(TAG, "Surface destroyed — stopping player (no blocking clearVideoSurface)")
        pendingPlay = null   // discard any deferred prepare — the surface is gone
        player.stop()
        activeSurface = null
        // surfaceWidth/surfaceHeight are intentionally NOT cleared here.
        // When a new surface becomes available (onSurfaceAvailable), setOutputSize() will be
        // called with the new dimensions — which may be the same or different from before.
        // Preserving the old dimensions means applyPresentationEffect() in play() can still
        // use them if called before the new onSurfaceAvailable fires.
    }

    /** Notify of the visible screen area (can be used for layout adjustments). */
    fun notifyVisibleArea(area: Rect) {
        visibleArea = area
    }

    // Surface dimensions reported by the car display — survive across stop()/play() cycles
    // because the physical surface does not change between VideoPlaybackScreen navigations.
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    enum class ScalingMode { FIT, FILL, STRETCH }

    fun setOutputSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        surfaceWidth = width
        surfaceHeight = height

        // NOTE: We intentionally do NOT call applyPresentationEffect() here.
        // Calling setVideoEffects() while the player is idle but no surface is attached yet
        // (possible because onSurfaceAvailable calls setOutputSize before setVideoSurface)
        // can leave FinalShaderWrapper without an output surface on some decoders.
        // Effects are applied explicitly in setVideoSurface() and playMediaItem() where we
        // know the surface (or imminent surface attachment) is present.
    }

    /** Set the scaling mode. Soft-restarts the player if playback is active. */
    fun setScalingMode(mode: ScalingMode) {
        if (_scalingMode.value == mode) return
        _scalingMode.value = mode
        
        if (player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            applyPresentationEffect()
        } else {
            // Player is active. Changing effects mid-stream wedges the hardware decoder.
            // Soft-restart the pipeline to safely apply the new scaling mode.
            val wasPlaying = player.isPlaying
            val pos = player.currentPosition
            
            // Capture what's playing (we could use player.currentMediaItem, but
            // re-routing through our internal play state is cleaner for our architecture).
            val oldItem = player.currentMediaItem
            
            Log.i(TAG, "Soft-restarting player to apply new scaling mode: $mode")
            player.stop() // guarantees transition to IDLE
            applyPresentationEffect()
            
            oldItem?.let {
                player.setMediaItem(it)
                player.prepare()
                player.seekTo(pos)
                player.playWhenReady = wasPlaying
            }
        }
    }

    private fun applyPresentationEffect() {
        val w = surfaceWidth
        val h = surfaceHeight
        if (w <= 0 || h <= 0) return

        val layoutMode = when (_scalingMode.value) {
            ScalingMode.FIT     -> Presentation.LAYOUT_SCALE_TO_FIT
            ScalingMode.FILL    -> Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            ScalingMode.STRETCH -> Presentation.LAYOUT_STRETCH_TO_FIT
        }

        Log.d(TAG, "Applying Presentation effect: ${w}x${h}, mode: ${_scalingMode.value}")
        player.setVideoEffects(
            listOf(Presentation.createForWidthAndHeight(w, h, layoutMode))
        )
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

        Log.i(TAG, "Starting playback with headers: $uri")
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

        // Re-apply Presentation effects BEFORE attaching surface so the effects pipeline is
        // created before the surface arrives. This matches the order in setVideoSurface().
        applyPresentationEffect()

        // Re-attach surface BEFORE prepare(). This is safe here because activeSurface is non-null
        // and the player is in IDLE (stop() above), so no renderer is active to race against.
        player.setVideoSurface(activeSurface!!)

        player.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }

        // Start saving playback position periodically
        startPeriodicSave(mediaId ?: uri.toString(), source)
    }

    private fun playMediaItem(mediaItem: MediaItem, title: String, source: String = "LOCAL", mediaId: String) {
        Log.i(TAG, "Starting playback: ${mediaItem.localConfiguration?.uri}")
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

        // Re-apply Presentation effects BEFORE attaching surface so the effects pipeline is
        // created before the surface arrives. This matches the order in setVideoSurface().
        applyPresentationEffect()

        // Re-attach surface BEFORE prepare(). This is safe here because activeSurface is non-null
        // and the player is in IDLE (stop() above), so no renderer is active to race against.
        player.setVideoSurface(activeSurface!!)

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
