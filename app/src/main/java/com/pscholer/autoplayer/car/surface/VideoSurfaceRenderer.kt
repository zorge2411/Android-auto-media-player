package com.pscholer.autoplayer.car.surface

import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.pscholer.autoplayer.car.surface.gl.GLVideoPipeline
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
        private const val VISIBLE_AREA_ATTACH_TIMEOUT_MS = 150L
    }

    private fun describeSurface(surface: Surface?): String {
        if (surface == null) return "null"
        return "Surface@${System.identityHashCode(surface).toString(16)}(valid=${surface.isValid})"
    }

    // ── Exposed state (screens can observe to adapt their UI) ─────────────────
    private val _state = MutableStateFlow<SurfaceState>(SurfaceState.Unavailable)
    val state: StateFlow<SurfaceState> = _state.asStateFlow()

    private var activeSurface: Surface? = null
    private var pendingAttachSurface: Surface? = null
    private var rawSurfaceWidth = 0
    private var rawSurfaceHeight = 0
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var lastVideoSize: MediaPlayerManager.VideoSize? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val glPipeline = GLVideoPipeline()
    private var pendingAttachFallback: Runnable? = null

    private fun cancelPendingAttachFallback() {
        pendingAttachFallback?.let(mainHandler::removeCallbacks)
        pendingAttachFallback = null
    }

    private fun attachPendingSurface(reason: String, width: Int, height: Int) {
        val surface = pendingAttachSurface ?: return
        if (width <= 0 || height <= 0) {
            Log.w(TAG, "attachPendingSurface($reason) skipped due to invalid size ${width}x${height}")
            return
        }

        cancelPendingAttachFallback()
        pendingAttachSurface = null
        surfaceWidth = width
        surfaceHeight = height

        Log.d(
            TAG,
            "Attaching pending surface from $reason using ${width}x${height}, " +
                "surface=${describeSurface(surface)}"
        )
        playerManager.setOutputSize(width, height)
        // GL pipeline always renders to the full EGL surface so the viewport covers the entire
        // display and FIT-mode bars are centered. The visible-area (width×height) is only used
        // for decoder output sizing above; the raw surface covers the whole screen.
        glPipeline.attach(surface, rawSurfaceWidth, rawSurfaceHeight) { intermediateSurface ->
            // Hand the SurfaceTexture-backed intermediate Surface to ExoPlayer (NOT the Car App Surface)
            mainHandler.post { playerManager.setVideoSurface(intermediateSurface) }
        }
        lastVideoSize?.let { glPipeline.setVideoSize(it) }
    }

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
                "@ ${surfaceContainer.dpi} dpi, surface=${describeSurface(surface)}")

        activeSurface = surface
        pendingAttachSurface = surface
        rawSurfaceWidth = surfaceContainer.width
        rawSurfaceHeight = surfaceContainer.height
        surfaceWidth = 0
        surfaceHeight = 0

        // Android Auto often delivers the full raw surface first and the actual visible area a
        // few milliseconds later. Treat the visible area as authoritative when possible so the
        // initial Presentation effect is created with the real renderable size.
        cancelPendingAttachFallback()
        pendingAttachFallback = Runnable {
            if (pendingAttachSurface === surface) {
                Log.w(
                    TAG,
                    "Visible area did not arrive within ${VISIBLE_AREA_ATTACH_TIMEOUT_MS}ms — " +
                        "falling back to raw surface size ${rawSurfaceWidth}x${rawSurfaceHeight}"
                )
                attachPendingSurface("surface-timeout", rawSurfaceWidth, rawSurfaceHeight)
            }
        }
        mainHandler.postDelayed(pendingAttachFallback!!, VISIBLE_AREA_ATTACH_TIMEOUT_MS)

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

        Log.d(
            TAG,
            "Visible area changed: $visibleArea => ${newW}x${newH} " +
                "(prev=${surfaceWidth}x${surfaceHeight}, surface=${describeSurface(activeSurface)})"
        )
        playerManager.notifyVisibleArea(visibleArea)

        if (pendingAttachSurface != null) {
            attachPendingSurface("visible-area", newW, newH)
        } else {
            surfaceWidth = newW
            surfaceHeight = newH

            // Update player output size to match the new visible area
            playerManager.setOutputSize(surfaceWidth, surfaceHeight)
            // Pipeline uses full raw surface for viewport — no setVisibleArea needed
        }

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
        Log.d(TAG, "Stable area changed: $stableArea, surface=${describeSurface(activeSurface)}")
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
        Log.i(
            TAG,
            "Surface destroyed, callbackSurface=${describeSurface(surfaceContainer.surface)}, " +
                "activeSurface=${describeSurface(activeSurface)}"
        )
        cancelPendingAttachFallback()
        pendingAttachSurface = null
        glPipeline.detach()
        playerManager.clearVideoSurface()
        activeSurface = null
        rawSurfaceWidth = 0
        rawSurfaceHeight = 0
        surfaceWidth = 0
        surfaceHeight = 0
        _state.value = SurfaceState.Unavailable
    }

    // ─────────────────────────────────────────────────────────────────────────

    /** Re-attach the surface to the GL pipeline after a configuration change. */
    fun onConfigurationChanged() {
        activeSurface?.let { surface ->
            Log.d(TAG, "Config changed — re-attaching surface to GL pipeline: ${describeSurface(surface)}")
            glPipeline.attach(surface, rawSurfaceWidth, rawSurfaceHeight) { intermediate ->
                mainHandler.post { playerManager.setVideoSurface(intermediate) }
            }
            lastVideoSize?.let { glPipeline.setVideoSize(it) }
        }
    }

    fun release() {
        Log.i(TAG, "Releasing GLVideoPipeline")
        glPipeline.release()
    }

    fun hasSurface(): Boolean = activeSurface != null

    fun onVideoSizeChanged(videoSize: MediaPlayerManager.VideoSize) {
        lastVideoSize = videoSize
        glPipeline.setVideoSize(videoSize)
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return

        val scaled = AspectRatioCalculator.calculateScaling(
            videoWidth = videoSize.width,
            videoHeight = videoSize.height,
            pixelAspectRatio = videoSize.pixelAspectRatio,
            rotationDegrees = videoSize.rotationDegrees,
            containerWidth = surfaceWidth,
            containerHeight = surfaceHeight
        )
        Log.d(
            TAG,
            "Video aspect ratio update: ${videoSize.width}x${videoSize.height} -> " +
                "container=${surfaceWidth}x${surfaceHeight}, scaled=${scaled.targetWidth}x${scaled.targetHeight}, " +
                "surface=${describeSurface(activeSurface)}"
        )
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
