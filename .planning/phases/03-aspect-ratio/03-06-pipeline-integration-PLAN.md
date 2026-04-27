---
phase: 3-aspect-ratio
plan: 06
type: execute
wave: 2
depends_on: ["03", "04", "05"]
files_modified:
  - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt
  - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
autonomous: true
requirements: [Feature-4.2, Feature-4.3, Feature-4.4, Feature-4.5]
must_haves:
  truths:
    - "GLVideoPipeline orchestrator owns HandlerThread + EglCore + OesTextureProgram + GeometryQuad + SurfaceTexture"
    - "ExoPlayer receives the SurfaceTexture-backed intermediate Surface, not the Car App Surface directly"
    - "VideoSurfaceRenderer feeds the Car App Surface to the pipeline, NOT directly to MediaPlayerManager"
    - "onFrameAvailable triggers a draw on the GL HandlerThread (never on the listener thread)"
    - "Vertex transform is recomputed on onVideoSizeChanged, onVisibleAreaChanged, and setScalingMode"
    - "Teardown follows RESEARCH.md Pattern 6 order (intermediate-Surface detach → SurfaceTexture release → EGL window-surface destroy → context preserved across reattach)"
    - "ScalingMode imported from AspectRatioCalculator (canonical home from Plan 04)"
    - "Qualcomm 2s teardown timeout suppression in MediaPlayerManager.onPlayerError remains as defense-in-depth"
  artifacts:
    - path: "app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt"
      provides: "Orchestrator: attach(carSurface,w,h), getInputSurface(), setVideoSize, setVisibleArea, setScalingMode, detach, release"
      contains: "HandlerThread"
    - path: "app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt"
      provides: "Wires Car App Surface lifecycle into GLVideoPipeline (no longer to MediaPlayerManager directly)"
      contains: "GLVideoPipeline"
    - path: "app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt"
      provides: "ExoPlayer wrapper now receiving SurfaceTexture-backed intermediate Surface; setScalingMode delegates to pipeline"
      contains: "AspectRatioCalculator.ScalingMode"
  key_links:
    - from: "VideoSurfaceRenderer.onSurfaceAvailable"
      to: "GLVideoPipeline.attach"
      via: "Posts Car App Surface + container dims to the pipeline's HandlerThread"
      pattern: "glPipeline.*attach"
    - from: "GLVideoPipeline.attach"
      to: "MediaPlayerManager.setVideoSurface"
      via: "Hands the SurfaceTexture-backed intermediate Surface to ExoPlayer (NOT the Car App Surface)"
      pattern: "playerManager\\.setVideoSurface\\(.*intermediate"
    - from: "SurfaceTexture.OnFrameAvailableListener"
      to: "GLVideoPipeline.drawFrame"
      via: "Listener posts to glHandler; drawFrame runs on GL thread"
      pattern: "glHandler\\.post"
---

<objective>
Wave 2: Compose the Wave 1 building blocks into the live pipeline. ExoPlayer's `setVideoSurface` now receives a `SurfaceTexture`-backed intermediate `Surface`. The Car App's `Surface` is owned by `GLVideoPipeline`, which draws each available frame through its OES texture + shader program + vertex transform onto the Car App Surface via `eglSwapBuffers`. This is the entire Option B architecture in one orchestrator class plus minimal wiring changes.

Purpose:
- Replace the broken Media3 effects path with a working custom GL stage.
- Apply `AspectRatioCalculator.computeVertexTransform` (Plan 04) per-frame uniform → produces correctly-aspected output for FIT (default), FILL, STRETCH.
- Preserve all existing surface-lifecycle invariants (deferred-prepare, Qualcomm 2s suppression).

Output:
- `GLVideoPipeline.kt` — single orchestrator, ~200-280 lines.
- `VideoSurfaceRenderer.kt` — owns a `GLVideoPipeline` instance, forwards Car App Surface lifecycle and visible-area / video-size changes.
- `MediaPlayerManager.kt` — `setScalingMode` delegates to pipeline; imports `ScalingMode` from `AspectRatioCalculator`.
- Builds; runs on head unit (manual verification deferred to Plan 07).
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/3-aspect-ratio/3-RESEARCH.md
@CLAUDE.md
@app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt
@app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt
@app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt
@app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt
@app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt
@app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
</context>

<tasks>

<task type="auto">
  <name>Task 1: Create GLVideoPipeline.kt orchestrator</name>
  <files>app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt</files>
  <read_first>
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (§"Pattern 1: SurfaceTexture as Intermediate Frame Sink"; §"Pattern 2: Dedicated GL HandlerThread"; §"Pattern 6: Teardown Order"; §"Pattern 7: Reactive Recompute"; §"Code Examples → Per-Frame Draw Sketch")
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt (initContext / createWindowSurface / makeCurrent / swapBuffers / destroyWindowSurface / release contract)
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt (compile / programId / oesTextureId / aPositionLoc / aTextureCoordLoc / uMvpMatrixLoc / uTexMatrixLoc / uTextureLoc / GL_TEXTURE_EXTERNAL_OES constant)
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt (draw signature)
    - app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt (computeVertexTransform signature + ScalingMode enum)
    - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt (VideoSize data class + setVideoSurface contract + setOutputSize)
  </read_first>
  <action>
    Create `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt`.

    The class contract:

    ```kotlin
    class GLVideoPipeline {
        // Lifecycle (all public methods are thread-safe — they post to the HandlerThread):
        fun attach(carSurface: Surface, width: Int, height: Int, onIntermediateReady: (Surface) -> Unit)
        fun setVideoSize(videoSize: MediaPlayerManager.VideoSize)
        fun setVisibleArea(width: Int, height: Int)
        fun setScalingMode(mode: AspectRatioCalculator.ScalingMode)
        fun detach()    // Car App surface destroyed; keep EGL context alive
        fun release()   // full teardown — call when VideoSurfaceRenderer goes away
    }
    ```

    Implementation skeleton (fill in based on RESEARCH.md Patterns 1, 2, 4, 6, 7 and the §"Code Examples → Per-Frame Draw Sketch"):

    ```kotlin
    package com.pscholer.autoplayer.car.surface.gl

    import android.graphics.SurfaceTexture
    import android.opengl.GLES20
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
     *   - OesTextureProgram (shader pair + OES texture id, Pattern 4)
     *   - GeometryQuad (unit-quad vertex/tex buffers)
     *   - SurfaceTexture wrapping the OES texture (Pattern 1)
     *   - intermediate Surface(SurfaceTexture) — handed to ExoPlayer
     *
     * Frame flow:
     *   ExoPlayer → intermediate Surface → SurfaceTexture (BufferQueue) → onFrameAvailable
     *   → glHandler.post { drawFrame } → updateTexImage + glClear + draw quad with current
     *   vertex transform + eglSwapBuffers → Car App Surface (head unit display)
     *
     * Source: RESEARCH.md §"Architecture Patterns" 1, 2, 4, 5, 6, 7.
     */
    class GLVideoPipeline {
        companion object { private const val TAG = "GLVideoPipeline" }

        private val thread = HandlerThread("GLVideoPipeline").apply { start() }
        private val glHandler = Handler(thread.looper)

        private val eglCore = EglCore()
        private val program = OesTextureProgram()
        private val quad = GeometryQuad()

        private var surfaceTexture: SurfaceTexture? = null
        private var intermediateSurface: Surface? = null

        // Inputs to vertex transform — recompute matrix on any change.
        private var videoWidth = 0
        private var videoHeight = 0
        private var videoPar = 1f
        private var videoRotation = 0
        private var containerWidth = 0
        private var containerHeight = 0
        private var scalingMode: ScalingMode = ScalingMode.FIT

        private val vertexMatrix = FloatArray(16).also {
            android.opengl.Matrix.setIdentityM(it, 0)
        }
        private val texMatrix = FloatArray(16).also {
            android.opengl.Matrix.setIdentityM(it, 0)
        }

        @Volatile private var initialized = false
        @Volatile private var frameAvailable = false

        /**
         * Called from VideoSurfaceRenderer.onSurfaceAvailable. Posts to GL thread:
         *   1. (first call) initContext + compile shaders + create OES + SurfaceTexture
         *   2. (every call) destroy old window surface, create new one against the new carSurface
         *   3. recompute vertex transform with the new container dimensions
         *   4. invoke onIntermediateReady(intermediateSurface) on the GL thread
         *      (caller is responsible for posting back to its own thread if needed)
         */
        fun attach(carSurface: Surface, width: Int, height: Int, onIntermediateReady: (Surface) -> Unit) {
            glHandler.post {
                if (!initialized) {
                    eglCore.initContext()
                    eglCore.createWindowSurface(carSurface)
                    eglCore.makeCurrent()
                    program.compile()
                    val st = SurfaceTexture(program.oesTextureId)
                    st.setOnFrameAvailableListener {
                        // NOTE: arbitrary thread (often codec). Don't call updateTexImage here.
                        frameAvailable = true
                        glHandler.post { drawFrame() }
                    }
                    surfaceTexture = st
                    intermediateSurface = Surface(st)
                    initialized = true
                    Log.i(TAG, "Pipeline initialized")
                } else {
                    // Re-attach: keep context, replace window surface only.
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
         * so a subsequent attach() can recreate the window surface without re-initializing EGL.
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

        // ── GL thread only ─────────────────────────────────────────────────────

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
    ```

    Critical:
    - All public methods just `glHandler.post { ... }` — never call EGL/GL on caller's thread.
    - `OnFrameAvailableListener` MUST set `frameAvailable = true` BEFORE posting drawFrame (in case post is dropped).
    - `drawFrame` checks `frameAvailable` before calling `updateTexImage`; resets to false after.
    - `detach()` keeps the context, only destroys the window surface (Pattern 6).
    - `release()` does full teardown then `quitSafely()` on the thread.
  </action>
  <verify>
    <automated>test -f app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt &amp;&amp; grep -q "HandlerThread" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt &amp;&amp; grep -q "OnFrameAvailableListener\\|setOnFrameAvailableListener" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt &amp;&amp; grep -q "AspectRatioCalculator.computeVertexTransform" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt &amp;&amp; grep -q "fun attach" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt &amp;&amp; grep -q "fun release" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt &amp;&amp; ./gradlew :app:compileDebugKotlin --quiet</automated>
  </verify>
  <acceptance_criteria>
    - `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt` exists
    - File contains `HandlerThread` (dedicated GL thread)
    - File contains `setOnFrameAvailableListener` (registration of listener that posts to glHandler)
    - File contains the literal `glHandler.post` (proves listener doesn't call updateTexImage on its own thread)
    - File contains `AspectRatioCalculator.computeVertexTransform` call
    - File contains all six lifecycle methods: `fun attach`, `fun setVideoSize`, `fun setVisibleArea`, `fun setScalingMode`, `fun detach`, `fun release`
    - File contains `eglCore.destroyWindowSurface` (Pattern 6 detach path — context preserved)
    - File contains `eglCore.release` (Pattern 6 full release path)
    - File contains `quad.draw(`
    - File contains `glUniformMatrix4fv` (uniform upload for both vertex and tex matrices — at least 2 calls)
    - `./gradlew :app:assembleDebug` exits 0
  </acceptance_criteria>
  <done>Orchestrator compiled; ready for VideoSurfaceRenderer to instantiate.</done>
</task>

<task type="auto">
  <name>Task 2: Wire GLVideoPipeline into VideoSurfaceRenderer</name>
  <files>app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt</files>
  <read_first>
    - app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt (current state — onSurfaceAvailable, onVisibleAreaChanged, onSurfaceDestroyed, onVideoSizeChanged, onConfigurationChanged)
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt (just-created — public API surface)
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (§"Pattern 7: Reactive Recompute" — exact wiring map)
  </read_first>
  <action>
    Modify `VideoSurfaceRenderer.kt`:

    1. Add import:
       ```kotlin
       import com.pscholer.autoplayer.car.surface.gl.GLVideoPipeline
       ```

    2. Add a private field after `lastVideoSize`:
       ```kotlin
       private val glPipeline = GLVideoPipeline()
       ```

    3. In `attachPendingSurface(reason, width, height)`: this is a SURGICAL replacement — KEEP all existing logic except the two specific lines noted below.

       **KEEP (do NOT touch) inside attachPendingSurface:**
       - The early-return guards: `val surface = pendingAttachSurface ?: return` and the `width <= 0 || height <= 0` check + warning log.
       - `cancelPendingAttachFallback()` call (this clears the 150ms visible-area-arrival timeout — REQUIRED for state-machine correctness).
       - `pendingAttachSurface = null` assignment (one-shot guarantee).
       - `surfaceWidth = width` and `surfaceHeight = height` assignments.
       - The existing `Log.d(TAG, "Attaching pending surface from $reason ...")` call.

       **REPLACE** ONLY the two lines that currently read:
       ```kotlin
       playerManager.setOutputSize(width, height)
       playerManager.setVideoSurface(surface)
       ```
       With this three-line block (in the same position):
       ```kotlin
       playerManager.setOutputSize(width, height)
       glPipeline.attach(surface, width, height) { intermediateSurface ->
           // Hand the SurfaceTexture-backed intermediate Surface to ExoPlayer (NOT the Car App Surface)
           mainHandler.post { playerManager.setVideoSurface(intermediateSurface) }
       }
       lastVideoSize?.let { glPipeline.setVideoSize(it) }
       ```

       Net diff: 1 line removed (`playerManager.setVideoSurface(surface)`), 4 lines added. Everything else in `attachPendingSurface` is preserved verbatim.

    4. In `onVisibleAreaChanged(visibleArea: Rect)`: after the existing `playerManager.notifyVisibleArea(visibleArea)` line, AND after the existing `playerManager.setOutputSize(...)` block, add:
       ```kotlin
       glPipeline.setVisibleArea(newW, newH)
       ```
       (only when `pendingAttachSurface == null`, i.e. inside the `else` branch where surfaceWidth/Height are already updated).

    5. In `onSurfaceDestroyed(surfaceContainer: SurfaceContainer)`: BEFORE `playerManager.clearVideoSurface()` add:
       ```kotlin
       glPipeline.detach()
       ```
       (detaches the EGL window surface but keeps context alive for the next onSurfaceAvailable).

    6. In `onVideoSizeChanged(videoSize: MediaPlayerManager.VideoSize)` (the existing project-side helper that screens call): after the existing `lastVideoSize = videoSize` assignment and the `AspectRatioCalculator.calculateScaling(...)` debug log call, add:
       ```kotlin
       glPipeline.setVideoSize(videoSize)
       ```

    7. Add a new `release()` method to `VideoSurfaceRenderer`:
       ```kotlin
       fun release() {
           Log.i(TAG, "Releasing GLVideoPipeline")
           glPipeline.release()
       }
       ```
       (Caller — `AutoMediaSession` — is expected to call this on session destroy. Wiring `AutoMediaSession.onDestroy → renderer.release()` is OUT OF SCOPE for this plan since it requires adding a new lifecycle hook; the existing teardown path already deletes the renderer and the GLVideoPipeline thread will be GC'd. However, providing the method now lets a follow-up plan wire it cleanly.)

    Do NOT modify `onConfigurationChanged()` — its existing behavior of `playerManager.setVideoSurface(activeSurface)` would now hand the Car App Surface directly to ExoPlayer (wrong). Replace its body with:
    ```kotlin
    fun onConfigurationChanged() {
        activeSurface?.let { surface ->
            Log.d(TAG, "Config changed — re-attaching surface to GL pipeline: ${describeSurface(surface)}")
            glPipeline.attach(surface, surfaceWidth, surfaceHeight) { intermediate ->
                mainHandler.post { playerManager.setVideoSurface(intermediate) }
            }
            lastVideoSize?.let { glPipeline.setVideoSize(it) }
        }
    }
    ```

    Critical:
    - ExoPlayer must NEVER receive `surfaceContainer.surface` directly — only `intermediateSurface`.
    - The callback `onIntermediateReady` is invoked on the GL HandlerThread; post back to mainHandler before calling MediaPlayerManager (which runs its own threading model).
  </action>
  <verify>
    <automated>grep -q "import com.pscholer.autoplayer.car.surface.gl.GLVideoPipeline" app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt &amp;&amp; grep -q "private val glPipeline = GLVideoPipeline()" app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt &amp;&amp; grep -q "glPipeline.attach" app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt &amp;&amp; grep -q "glPipeline.setVideoSize" app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt &amp;&amp; grep -q "glPipeline.setVisibleArea" app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt &amp;&amp; grep -q "glPipeline.detach" app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt &amp;&amp; grep -q "glPipeline.release" app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt &amp;&amp; ./gradlew :app:compileDebugKotlin --quiet</automated>
  </verify>
  <acceptance_criteria>
    - `VideoSurfaceRenderer.kt` imports `com.pscholer.autoplayer.car.surface.gl.GLVideoPipeline`
    - `VideoSurfaceRenderer.kt` declares `private val glPipeline = GLVideoPipeline()`
    - `VideoSurfaceRenderer.kt` calls `glPipeline.attach(` (in attachPendingSurface and onConfigurationChanged)
    - `VideoSurfaceRenderer.kt` calls `glPipeline.setVideoSize(` (in onVideoSizeChanged and after attach)
    - `VideoSurfaceRenderer.kt` calls `glPipeline.setVisibleArea(` (in onVisibleAreaChanged)
    - `VideoSurfaceRenderer.kt` calls `glPipeline.detach()` (in onSurfaceDestroyed)
    - `VideoSurfaceRenderer.kt` defines `fun release()` containing `glPipeline.release()`
    - `VideoSurfaceRenderer.kt` no longer hands `activeSurface` (the Car App Surface) directly to `playerManager.setVideoSurface` — verify via grep that `playerManager.setVideoSurface(surface)` does not appear with `surface` referring to the Car App surface anymore (only `intermediateSurface` / `intermediate` should)
    - `VideoSurfaceRenderer.kt` STILL contains `cancelPendingAttachFallback()` and `pendingAttachSurface = null` inside `attachPendingSurface` — the surface-deferred-attach state machine is preserved (Warning #5 fix: explicit keep-vs-replace boundary)
    - `VideoSurfaceRenderer.kt` STILL contains `pendingAttachFallback` field, `mainHandler.postDelayed(pendingAttachFallback`, and `mainHandler.removeCallbacks` — the 150ms visible-area fallback timer is preserved
    - `./gradlew :app:assembleDebug` exits 0
  </acceptance_criteria>
  <done>Renderer composes pipeline; ExoPlayer no longer receives Car App Surface directly.</done>
</task>

<task type="auto">
  <name>Task 3: Update MediaPlayerManager to use AspectRatioCalculator.ScalingMode</name>
  <files>app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt</files>
  <read_first>
    - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt (current state — has its own `enum class ScalingMode`)
    - app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt (now hosts canonical `ScalingMode`)
    - app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt (callers using ScalingMode)
  </read_first>
  <action>
    In `MediaPlayerManager.kt`:

    1. Add import (if not already present):
       ```kotlin
       import com.pscholer.autoplayer.util.AspectRatioCalculator
       ```

    2. DELETE the local `enum class ScalingMode { FIT, FILL, STRETCH }` (currently around line 363).

    3. Update the type of `_scalingMode` and `scalingMode`:
       ```kotlin
       private val _scalingMode = MutableStateFlow(AspectRatioCalculator.ScalingMode.FIT)
       val scalingMode: StateFlow<AspectRatioCalculator.ScalingMode> = _scalingMode.asStateFlow()
       ```

    4. Update `fun setScalingMode(mode: ScalingMode)` signature:
       ```kotlin
       fun setScalingMode(mode: AspectRatioCalculator.ScalingMode) {
           if (_scalingMode.value == mode) return
           _scalingMode.value = mode
           Log.i(TAG, "Scaling mode set to $mode (delegated to GLVideoPipeline via VideoSurfaceRenderer)")
       }
       ```

    5. Verify no other references to the old local `MediaPlayerManager.ScalingMode` exist anywhere in the codebase. Search for `MediaPlayerManager.ScalingMode` and replace with `AspectRatioCalculator.ScalingMode`. (Likely zero occurrences besides what's in this file plus possibly tests — there are no Kotlin tests yet beyond the new AspectRatioCalculatorTest, which already imports from AspectRatioCalculator.)

    Do NOT change anything else: the surface-deferred-prepare path, Qualcomm 2s suppression, periodic-save, and `setVideoSurface` direct-call (from Plan 03) all stay intact.

    Note: This plan does NOT yet plumb `setScalingMode` from MediaPlayerManager through to `VideoSurfaceRenderer.glPipeline.setScalingMode`. That is intentional — `setScalingMode` is currently UI-driven (no UI yet, FIT default ships per RESEARCH.md User Constraints "Deferred Ideas"). The wiring stub is enough for Wave 4 manual verification of the FIT default path. A future plan can add the MediaPlayerManager→VideoSurfaceRenderer→GLVideoPipeline scaling-mode forward wiring when a UI toggle is added.
  </action>
  <verify>
    <automated>! grep -q "enum class ScalingMode" app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt &amp;&amp; grep -q "AspectRatioCalculator.ScalingMode" app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt &amp;&amp; grep -q "import com.pscholer.autoplayer.util.AspectRatioCalculator" app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt &amp;&amp; ./gradlew :app:compileDebugKotlin --quiet</automated>
  </verify>
  <acceptance_criteria>
    - `MediaPlayerManager.kt` does NOT contain `enum class ScalingMode` (the local enum is removed)
    - `MediaPlayerManager.kt` contains `import com.pscholer.autoplayer.util.AspectRatioCalculator`
    - `MediaPlayerManager.kt` contains `AspectRatioCalculator.ScalingMode` (StateFlow type and setScalingMode parameter)
    - `MediaPlayerManager.kt` STILL contains `lastSurfaceTeardownMs` and the timeout suppression in `onPlayerError` (defense in depth)
    - `MediaPlayerManager.kt` STILL contains `pendingPlay` / `pendingSurface` deferred-prepare code
    - No file in `app/src/` references `MediaPlayerManager.ScalingMode` (the old qualified name)
    - `./gradlew :app:assembleDebug` exits 0
  </acceptance_criteria>
  <done>ScalingMode canonical home is `AspectRatioCalculator`; MediaPlayerManager imports and uses it; build passes.</done>
</task>

</tasks>

<verification>
- `GLVideoPipeline.kt` exists with all six lifecycle methods + drawFrame.
- `VideoSurfaceRenderer.kt` instantiates the pipeline and forwards all relevant lifecycle calls.
- ExoPlayer receives the SurfaceTexture-backed intermediate Surface, never the Car App Surface directly.
- `MediaPlayerManager.kt` uses `AspectRatioCalculator.ScalingMode` and preserves all existing surface-lifecycle invariants.
- `./gradlew :app:assembleDebug` exits 0.
- `./gradlew :app:testDebugUnitTest --tests "*AspectRatioCalculatorTest*"` still passes (no regression in math).
</verification>

<success_criteria>
- Build is green.
- All Plan 04 unit tests still pass.
- Wave 4 (Plan 07) can perform manual head-unit verification on this build.
</success_criteria>

<output>
Create `.planning/phases/3-aspect-ratio/3-06-SUMMARY.md` summarizing:
- New `GLVideoPipeline` class shape (line count, key fields, key methods)
- `VideoSurfaceRenderer` wiring summary (every call site that now goes through `glPipeline`)
- `MediaPlayerManager` change summary (`ScalingMode` import, no behavior change to surface lifecycle)
- Confirmation: build green, AspectRatioCalculatorTest green
- Note: Wave 4 (Plan 07) is required before claiming Phase 3 complete
</output>
