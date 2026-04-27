package com.pscholer.autoplayer.car.surface.gl

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.pscholer.autoplayer.player.MediaPlayerManager
import com.pscholer.autoplayer.util.AspectRatioCalculator
import com.pscholer.autoplayer.util.AspectRatioCalculator.ScalingMode

/**
 * Orchestrator for the Option B custom GL video pipeline.
 *
 * Owns:
 *   - dedicated GL HandlerThread (RESEARCH Pattern 2)
 *   - EglCore (display + context + window surface against Car App Surface, Pattern 5)
 *   - OesTextureProgram (shader pair + OES texture, Pattern 4)
 *   - GeometryQuad (unit-quad vertex/tex buffers)
 *   - SurfaceTexture wrapping the OES texture (Pattern 1)
 *   - intermediate Surface(SurfaceTexture) — handed to ExoPlayer
 *
 * Frame flow:
 *   ExoPlayer → intermediate Surface → SurfaceTexture (BufferQueue) → onFrameAvailable
 *   → glHandler.post { drawFrame } → updateTexImage + draw quad with vertex transform
 *   + eglSwapBuffers → Car App Surface (head unit display)
 *
 * Source: RESEARCH.md §"Architecture Patterns" 1, 2, 4, 5, 6, 7.
 */
class GLVideoPipeline {
    companion object {
        private const val TAG = "GLVideoPipeline"
    }

    private val thread = HandlerThread("GLVideoPipeline").apply { start() }
    private val glHandler = Handler(thread.looper)

    private val eglCore = EglCore()
    private val program = OesTextureProgram()
    private val quad = GeometryQuad()

    private var surfaceTexture: SurfaceTexture? = null
    private var intermediateSurface: Surface? = null

    // Inputs to vertex transform — protected by glHandler (only accessed on GL thread)
    private var videoWidth = 0
    private var videoHeight = 0
    private var videoPar = 1f
    private var videoRotation = 0
    private var containerWidth = 0
    private var containerHeight = 0
    private var scalingMode: ScalingMode = ScalingMode.FIT

    private val vertexMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private val texMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

    @Volatile private var initialized = false
    @Volatile private var frameAvailable = false

    /**
     * Called from VideoSurfaceRenderer.onSurfaceAvailable. Posts to GL thread:
     *   1. (first call) initContext + compile shaders + create OES texture + SurfaceTexture
     *   2. (every call) destroy old window surface, create new one against the new carSurface
     *   3. invoke onIntermediateReady(intermediateSurface) — called on GL thread; caller must
     *      post to its own thread if needed.
     */
    fun attach(carSurface: Surface, width: Int, height: Int, onIntermediateReady: (Surface) -> Unit) {
        glHandler.post {
            if (!initialized) {
                eglCore.initContext()
                eglCore.createWindowSurface(carSurface)
                eglCore.makeCurrent()
                program.init()
                quad.allocate()
                val st = SurfaceTexture(program.oesTextureId)
                st.setOnFrameAvailableListener {
                    frameAvailable = true
                    glHandler.post { drawFrame() }
                }
                surfaceTexture = st
                intermediateSurface = Surface(st)
                initialized = true
                Log.i(TAG, "Pipeline initialized (${width}x${height})")
            } else {
                eglCore.createWindowSurface(carSurface)
                eglCore.makeCurrent()
            }
            containerWidth = width
            containerHeight = height
            GLES20.glViewport(0, 0, width, height)
            recomputeVertexMatrix()
            onIntermediateReady(intermediateSurface!!)
        }
    }

    fun setVideoSize(videoSize: MediaPlayerManager.VideoSize) {
        glHandler.post {
            videoWidth = videoSize.width
            videoHeight = videoSize.height
            videoPar = videoSize.pixelAspectRatio
            videoRotation = videoSize.rotationDegrees
            recomputeVertexMatrix()
            if (frameAvailable) drawFrame()
        }
    }

    fun setVisibleArea(width: Int, height: Int) {
        glHandler.post {
            if (width <= 0 || height <= 0) return@post
            if (width == containerWidth && height == containerHeight) return@post
            containerWidth = width
            containerHeight = height
            GLES20.glViewport(0, 0, width, height)
            recomputeVertexMatrix()
            if (frameAvailable) drawFrame()
        }
    }

    fun setScalingMode(mode: ScalingMode) {
        glHandler.post {
            if (mode == scalingMode) return@post
            scalingMode = mode
            recomputeVertexMatrix()
            if (frameAvailable) drawFrame()
        }
    }

    /**
     * Car App Surface destroyed. Tear down window surface only — keep context alive
     * so attach() can recreate the window surface without re-initializing EGL.
     * RESEARCH Pattern 6 step 3.
     */
    fun detach() {
        glHandler.post {
            if (initialized) {
                eglCore.destroyWindowSurface()
            }
        }
    }

    /** Full teardown. RESEARCH Pattern 6 steps 1-4. */
    fun release() {
        glHandler.post {
            if (initialized) {
                surfaceTexture?.setOnFrameAvailableListener(null)
                surfaceTexture?.release()
                surfaceTexture = null
                intermediateSurface?.release()
                intermediateSurface = null
                program.release()
                eglCore.release()
                initialized = false
            }
        }
        thread.quitSafely()
    }

    // ── GL thread only ──────────────────────────────────────────────────────────

    private fun recomputeVertexMatrix() {
        val m = AspectRatioCalculator.computeVertexTransform(
            videoWidth = videoWidth,
            videoHeight = videoHeight,
            pixelAspectRatio = videoPar,
            rotationDegrees = videoRotation,
            containerWidth = containerWidth,
            containerHeight = containerHeight,
            mode = scalingMode
        )
        System.arraycopy(m, 0, vertexMatrix, 0, 16)
    }

    private fun drawFrame() {
        if (!initialized) return
        val st = surfaceTexture ?: return
        if (!frameAvailable) return
        try {
            st.updateTexImage()
            st.getTransformMatrix(texMatrix)
        } catch (t: Throwable) {
            Log.w(TAG, "updateTexImage threw — pipeline likely tearing down", t)
            return
        }
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program.programId)
        GLES20.glUniformMatrix4fv(program.uMvpMatrixLoc, 1, false, vertexMatrix, 0)
        GLES20.glUniformMatrix4fv(program.uTexMatrixLoc, 1, false, texMatrix, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(OesTextureProgram.GL_TEXTURE_EXTERNAL_OES, program.oesTextureId)
        GLES20.glUniform1i(program.uTextureLoc, 0)
        quad.draw(program.aPositionLoc, program.aTextureCoordLoc)
        eglCore.swapBuffers()
        frameAvailable = false
    }
}
