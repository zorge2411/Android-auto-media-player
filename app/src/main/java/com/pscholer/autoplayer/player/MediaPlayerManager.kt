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

    /** Attach the car display Surface to ExoPlayer's video pipeline. */
    fun setVideoSurface(surface: Surface) {
        Log.d(TAG, "Attaching video surface")
        activeSurface = surface
        player.setVideoSurface(surface)
    }

    /** Detach the surface (called before the Surface is released). */
    fun clearVideoSurface() {
        Log.d(TAG, "Clearing video surface")
        player.clearVideoSurface()
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

    private val _scalingMode = MutableStateFlow(ScalingMode.FIT)
    val scalingMode: StateFlow<ScalingMode> = _scalingMode.asStateFlow()

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
    fun setOutputSize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        surfaceWidth = width
        surfaceHeight = height
        applyPresentationEffect()
    }

    /** Set the scaling mode and re-apply effects. */
    fun setScalingMode(mode: ScalingMode) {
        _scalingMode.value = mode
        applyPresentationEffect()
    }

    /**
     * Apply (or re-apply) the Presentation effect to ExoPlayer's video pipeline.
     * Must be called before every prepare() so the new pipeline has valid output dimensions.
     * Safe to call multiple times — ExoPlayer replaces the effect list each time.
     */
    private fun applyPresentationEffect() {
        val w = surfaceWidth
        val h = surfaceHeight
        if (w <= 0 || h <= 0) return

        val layoutMode = when (_scalingMode.value) {
            ScalingMode.FIT     -> Presentation.LAYOUT_SCALE_TO_FIT
            ScalingMode.FILL    -> Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROPPING
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

        // Re-attach surface BEFORE prepare(). After player.stop(), Media3 releases renderers
        // internally. If setVideoSurface() is not called before prepare(), the new
        // DefaultVideoFrameProcessor creates an EGLSurface with no backing android.view.Surface,
        // causing EGL_WIDTH queries to return -1 — which propagates to
        // Presentation.createForWidthAndHeight and crashes as "width -1 must be positive".
        activeSurface?.let { player.setVideoSurface(it) }

        // Re-apply Presentation effects BEFORE prepare() so the new renderer pipeline has valid
        // output dimensions. The car surface persists across screen navigations — onSurfaceAvailable
        // does not re-fire between plays, so we must apply effects explicitly here each time.
        applyPresentationEffect()

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

        // Re-attach surface BEFORE prepare(). After player.stop(), Media3 releases renderers
        // internally. If setVideoSurface() is not called before prepare(), the new
        // DefaultVideoFrameProcessor creates an EGLSurface with no backing android.view.Surface,
        // causing EGL_WIDTH queries to return -1 — which propagates to
        // Presentation.createForWidthAndHeight and crashes as "width -1 must be positive".
        activeSurface?.let { player.setVideoSurface(it) }

        // Re-apply Presentation effects BEFORE prepare() so the new renderer pipeline has valid
        // output dimensions. The car surface persists across screen navigations — onSurfaceAvailable
        // does not re-fire between plays, so we must apply effects explicitly here each time.
        applyPresentationEffect()

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
