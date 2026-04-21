package com.pscholer.autoplayer.car.surface

import android.graphics.Rect
import android.util.Log
import android.view.Surface
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.pscholer.autoplayer.player.MediaPlayerManager
import com.pscholer.autoplayer.util.AspectRatioCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges the Car App Surface to ExoPlayer. Invariants:
 *  - Never hand a Surface to ExoPlayer after onSurfaceDestroyed; always clearVideoSurface first.
 *  - Requires ACCESS_SURFACE + NAVIGATION category (MEDIA category sandboxes Surface access).
 */
class VideoSurfaceRenderer(
    private val carContext: CarContext,
    private val playerManager: MediaPlayerManager
) : SurfaceCallback {

    companion object {
        private const val TAG = "VideoSurfaceRenderer"
    }

    // ── Exposed state (screens can observe to adapt their UI) ─────────────────
    private val _state = MutableStateFlow<SurfaceState>(SurfaceState.Unavailable)
    val state: StateFlow<SurfaceState> = _state.asStateFlow()

    private var activeSurface: Surface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var lastVideoSize: MediaPlayerManager.VideoSize? = null

    // ─────────────────────────────────────────────────────────────────────────
    //  SurfaceCallback implementation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when the car display Surface becomes ready.
     * Wire it into ExoPlayer immediately so video starts rendering.
     */
    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val surface = surfaceContainer.surface ?: run {
            Log.w(TAG, "onSurfaceAvailable: SurfaceContainer.surface is null — " +
                    "check ACCESS_SURFACE permission in manifest")
            return
        }

        Log.i(TAG, "Surface available — ${surfaceContainer.width}x${surfaceContainer.height} " +
                "@ ${surfaceContainer.dpi} dpi")

        activeSurface = surface
        surfaceWidth = surfaceContainer.width
        surfaceHeight = surfaceContainer.height
        playerManager.setVideoSurface(surface)
        playerManager.setOutputSize(surfaceWidth, surfaceHeight)
        _state.value = SurfaceState.Available(
            surface = surface,
            width = surfaceContainer.width,
            height = surfaceContainer.height,
            dpi = surfaceContainer.dpi
        )
    }

    /**
     * Called when the visible portion of the Surface changes.
     * "Visible" excludes areas hidden by template chrome (status bar, action strip, etc.).
     * Use this rect to letterbox/pillarbox content if desired.
     */
    override fun onVisibleAreaChanged(visibleArea: Rect) {
        val newW = visibleArea.width()
        val newH = visibleArea.height()
        if (newW == surfaceWidth && newH == surfaceHeight) return

        Log.d(TAG, "Visible area changed: $visibleArea")
        playerManager.notifyVisibleArea(visibleArea)
        surfaceWidth = newW
        surfaceHeight = newH

        // Update player output size to match the new visible area
        playerManager.setOutputSize(surfaceWidth, surfaceHeight)

        lastVideoSize?.let { onVideoSizeChanged(it) }

        val current = _state.value
        if (current is SurfaceState.Available) {
            _state.value = current.copy(visibleArea = visibleArea)
        }
    }

    /**
     * The stable area is guaranteed never to be obscured, even transiently
     * (e.g. no overlapping toasts or temporary UI). Safe zone for HUD overlays.
     */
    override fun onStableAreaChanged(stableArea: Rect) {
        Log.d(TAG, "Stable area changed: $stableArea")
        val current = _state.value
        if (current is SurfaceState.Available) {
            _state.value = current.copy(stableArea = stableArea)
        }
    }

    /**
     * Called when the car disconnects, session ends, or surface is recycled
     * due to a configuration change. MUST detach from ExoPlayer immediately.
     */
    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        Log.i(TAG, "Surface destroyed")
        playerManager.clearVideoSurface()
        activeSurface = null
        _state.value = SurfaceState.Unavailable
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Re-attach the surface to ExoPlayer after a configuration change. */
    fun onConfigurationChanged() {
        activeSurface?.let {
            Log.d(TAG, "Config changed — re-attaching surface to player")
            playerManager.setVideoSurface(it)
        }
    }

    fun hasSurface(): Boolean = activeSurface != null

    fun onVideoSizeChanged(videoSize: MediaPlayerManager.VideoSize) {
        lastVideoSize = videoSize
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return

        // Calculation here is for logging/debugging or future UI overlays.
        // The actual video rendering is handled by MediaPlayerManager using
        // Media3 Presentation effects which correctly handle letterboxing/cropping.
        val scaled = AspectRatioCalculator.calculateScaling(
            videoWidth = videoSize.width,
            videoHeight = videoSize.height,
            pixelAspectRatio = videoSize.pixelAspectRatio,
            rotationDegrees = videoSize.rotationDegrees,
            containerWidth = surfaceWidth,
            containerHeight = surfaceHeight
        )
        Log.d(TAG, "Video aspect ratio update: ${videoSize.width}x${videoSize.height} -> scaled to match car display")
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  State model
    // ─────────────────────────────────────────────────────────────────────────

    sealed class SurfaceState {
        object Unavailable : SurfaceState()

        data class Available(
            val surface: Surface,
            val width: Int,
            val height: Int,
            val dpi: Int,
            val visibleArea: Rect = Rect(0, 0, width, height),
            val stableArea: Rect = Rect(0, 0, width, height)
        ) : SurfaceState()
    }
}
