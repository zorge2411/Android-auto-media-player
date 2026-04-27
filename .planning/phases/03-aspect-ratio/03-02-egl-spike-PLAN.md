---
phase: 3-aspect-ratio
plan: 02
type: execute
wave: 0
depends_on: []
files_modified:
  - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt
autonomous: false
requirements: [Feature-4.3]
must_haves:
  truths:
    - "EGL14 can create a window surface against the Car App Surface on this head unit"
    - "glClearColor + eglSwapBuffers produces visible output on the head unit screen"
    - "Spike code is THROWAWAY — must be deleted in Wave 1 before productionization"
  artifacts:
    - path: "app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt"
      provides: "Minimal EGL14 + clear-to-red proof-of-concept (TEMPORARY)"
      contains: "EGL14.eglCreateWindowSurface"
  key_links:
    - from: "VideoSurfaceRenderer.onSurfaceAvailable"
      to: "EglSpike.runRedScreenSpike"
      via: "Direct call passing the Car App Surface"
      pattern: "EglSpike\\.runRedScreenSpike"
---

<objective>
Wave 0b: Validate that `EGL14.eglCreateWindowSurface` succeeds against the Car App's remote `Surface`. RESEARCH.md flags this as the one MEDIUM-confidence assumption gating the entire Option B architecture; a 30-line clear-to-red spike resolves it cheaply.

Purpose: If the spike succeeds (head unit shows red screen for ~1s), the rest of the architecture is unblocked and we know `EglCore` will work. If it fails (`EGL_NO_SURFACE`, `EGL_BAD_NATIVE_WINDOW`, or no visible red), we must escalate before sinking effort into Waves 2-3.

Output:
- `EglSpike.kt` — single-file, ~60 lines, creates EGL14 display+context+window-surface against the Car App Surface, calls `glClearColor(1f,0f,0f,1f); glClear; eglSwapBuffers`, then tears everything down.
- `VideoSurfaceRenderer.onSurfaceAvailable` patched to call the spike ONCE on first surface arrival, log the EGL extension string, then proceed with existing flow.
- Manual verification on head unit confirming the screen flashes red.
- **Spike code is explicitly marked THROWAWAY** — it MUST be deleted in Wave 1 before productionization (Plan 05 tasks remove it).
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/3-aspect-ratio/3-RESEARCH.md
@CLAUDE.md
@app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt
</context>

<tasks>

<task type="auto">
  <name>Task 1: Create EglSpike.kt (THROWAWAY — Wave 1 deletes)</name>
  <files>app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt</files>
  <read_first>
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (§"Pattern 5: EGL14 Window Surface from a Car App Surface" — exact attrib lists; §"Code Examples" → "EGL Core Skeleton" — reference Kotlin port; §"Verification spike (Wave 0)" — what success looks like)
    - Grafika EglCore reference: https://github.com/google/grafika/blob/master/app/src/main/java/com/android/grafika/gles/EglCore.java
    - app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt (so the call site you'll add in Task 2 makes sense)
  </read_first>
  <action>
    Create new file `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt`. The directory `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/` does not exist yet — create it as part of this task.

    File contents (verbatim, including the THROWAWAY warning header):

    ```kotlin
    package com.pscholer.autoplayer.car.surface.gl

    import android.opengl.EGL14
    import android.opengl.EGLConfig
    import android.opengl.EGLContext
    import android.opengl.EGLDisplay
    import android.opengl.EGLSurface
    import android.opengl.GLES20
    import android.util.Log
    import android.view.Surface

    /**
     * THROWAWAY Wave 0b spike — DELETE in Wave 1 (Plan 05 productionizes EglCore).
     *
     * Goal: Prove EGL14.eglCreateWindowSurface succeeds on the Car App's remote Surface.
     * If runRedScreenSpike() returns true and the head unit briefly flashes red, the
     * entire Option B architecture is unblocked. If false, escalate before Wave 1.
     *
     * Reference: RESEARCH.md §"Pattern 5", Grafika EglCore.java.
     */
    object EglSpike {
        private const val TAG = "EglSpike"

        /**
         * Runs the clear-to-red spike on the calling thread (must be a thread you own
         * — VideoSurfaceRenderer should post this to a HandlerThread, NOT call it on
         * the Car App main thread). Returns true if EGL setup + swap succeeded.
         */
        fun runRedScreenSpike(carSurface: Surface, width: Int, height: Int): Boolean {
            var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
            var context: EGLContext = EGL14.EGL_NO_CONTEXT
            var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
            try {
                display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                if (display === EGL14.EGL_NO_DISPLAY) {
                    Log.e(TAG, "eglGetDisplay returned EGL_NO_DISPLAY")
                    return false
                }
                val v = IntArray(2)
                if (!EGL14.eglInitialize(display, v, 0, v, 1)) {
                    Log.e(TAG, "eglInitialize failed err=0x${EGL14.eglGetError().toString(16)}")
                    return false
                }
                Log.i(TAG, "EGL initialized v${v[0]}.${v[1]}")

                val configAttribs = intArrayOf(
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 0,
                    EGL14.EGL_STENCIL_SIZE, 0,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_NONE
                )
                val configs = arrayOfNulls<EGLConfig>(1)
                val n = IntArray(1)
                if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, n, 0) || n[0] == 0) {
                    Log.e(TAG, "eglChooseConfig failed err=0x${EGL14.eglGetError().toString(16)}")
                    return false
                }
                val config = configs[0]

                val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
                if (context === EGL14.EGL_NO_CONTEXT) {
                    Log.e(TAG, "eglCreateContext failed err=0x${EGL14.eglGetError().toString(16)}")
                    return false
                }

                val surfAttribs = intArrayOf(EGL14.EGL_NONE)
                eglSurface = EGL14.eglCreateWindowSurface(display, config, carSurface, surfAttribs, 0)
                if (eglSurface === EGL14.EGL_NO_SURFACE) {
                    val err = EGL14.eglGetError()
                    Log.e(TAG, "eglCreateWindowSurface FAILED on Car App Surface — err=0x${err.toString(16)} " +
                        "(EGL_BAD_NATIVE_WINDOW=0x300B, EGL_BAD_MATCH=0x3009)")
                    return false
                }
                Log.i(TAG, "eglCreateWindowSurface OK on Car App Surface (${width}x${height})")

                if (!EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) {
                    Log.e(TAG, "eglMakeCurrent failed err=0x${EGL14.eglGetError().toString(16)}")
                    return false
                }
                Log.i(TAG, "GL_VENDOR=${GLES20.glGetString(GLES20.GL_VENDOR)} " +
                    "GL_RENDERER=${GLES20.glGetString(GLES20.GL_RENDERER)} " +
                    "GL_EXT=${GLES20.glGetString(GLES20.GL_EXTENSIONS)?.take(200)}...")

                GLES20.glViewport(0, 0, width, height)
                GLES20.glClearColor(1f, 0f, 0f, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                if (!EGL14.eglSwapBuffers(display, eglSurface)) {
                    Log.e(TAG, "eglSwapBuffers failed err=0x${EGL14.eglGetError().toString(16)}")
                    return false
                }
                Log.i(TAG, "SPIKE SUCCESS — head unit should briefly show red. Sleeping 1000ms before teardown.")
                Thread.sleep(1000L)
                return true
            } catch (t: Throwable) {
                Log.e(TAG, "Spike threw", t)
                return false
            } finally {
                if (display !== EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                    if (eglSurface !== EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
                    if (context !== EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
                    EGL14.eglTerminate(display)
                }
            }
        }
    }
    ```

    Critical notes:
    - The class is `object` (singleton) — no state across calls.
    - All EGL teardown is in `finally` to avoid leaks even if the spike fails partway.
    - `Thread.sleep(1000L)` keeps the red visible long enough for the human to see it.
    - The THROWAWAY header at the top is REQUIRED — Wave 1 (Plan 05 Task 1) deletes this file.
  </action>
  <verify>
    <automated>test -f app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt && grep -q "EGL14.eglCreateWindowSurface" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt && grep -q "THROWAWAY" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt</automated>
  </verify>
  <acceptance_criteria>
    - `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt` exists
    - File contains the literal string `THROWAWAY` (deletion marker for Wave 1)
    - File contains the literal string `EGL14.eglCreateWindowSurface`
    - File contains the literal string `glClearColor(1f, 0f, 0f, 1f)`
    - File contains the literal string `eglSwapBuffers`
    - File compiles: `./gradlew :app:compileDebugKotlin` exits 0
  </acceptance_criteria>
  <done>EglSpike.kt compiled into the app, ready to be invoked by VideoSurfaceRenderer in Task 2.</done>
</task>

<task type="auto">
  <name>Task 2: Wire spike into VideoSurfaceRenderer.onSurfaceAvailable (THROWAWAY)</name>
  <files>app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt</files>
  <read_first>
    - app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt (current file — onSurfaceAvailable is at lines ~85-124)
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt (just-created)
  </read_first>
  <action>
    Patch `VideoSurfaceRenderer.kt` to invoke `EglSpike.runRedScreenSpike` ONCE on the first `onSurfaceAvailable` call, on a dedicated short-lived thread (do NOT block the Car App main thread).

    Add at the top of the file (after existing imports):
    ```kotlin
    import com.pscholer.autoplayer.car.surface.gl.EglSpike
    ```

    Add a new private field near the other private fields (around line 42):
    ```kotlin
    // THROWAWAY (Wave 1 deletes): one-shot spike trigger
    private var spikeRan = false
    ```

    Inside `onSurfaceAvailable(surfaceContainer: SurfaceContainer)`, immediately after the existing `Log.i(TAG, "Surface available — ...")` line and before the `activeSurface = surface` assignment, insert:
    ```kotlin
            // ── THROWAWAY Wave 0b spike (Wave 1 Plan 05 Task 1 deletes this block) ──
            if (!spikeRan) {
                spikeRan = true
                val w = surfaceContainer.width
                val h = surfaceContainer.height
                Thread({
                    val ok = EglSpike.runRedScreenSpike(surface, w, h)
                    Log.i(TAG, "EGL clear-to-red spike result: ok=$ok (true => Option B unblocked)")
                }, "egl-spike").start()
            }
            // ── END SPIKE BLOCK ──
    ```

    Critical:
    - The spike must run on a NEW thread (`Thread(...).start()`), NOT the Car App main thread, because EGL `eglMakeCurrent` binds the context to the calling thread and we must not pollute the AA framework's thread with our EGL state.
    - `spikeRan` ensures it executes once per renderer instance (re-runs across AA reconnects are fine but not strictly required for the spike's purpose).
    - Both opening and closing comment markers (`THROWAWAY Wave 0b spike` and `END SPIKE BLOCK`) are REQUIRED — Wave 1 Plan 05 Task 1 greps for them to delete the block.

    Do NOT modify any other part of `VideoSurfaceRenderer.kt`. Do NOT add new imports beyond `EglSpike`.
  </action>
  <verify>
    <automated>grep -q "EglSpike.runRedScreenSpike" app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt && grep -q "THROWAWAY Wave 0b spike" app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt && grep -q "END SPIKE BLOCK" app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt && ./gradlew :app:compileDebugKotlin --quiet</automated>
  </verify>
  <acceptance_criteria>
    - `VideoSurfaceRenderer.kt` contains `EglSpike.runRedScreenSpike`
    - `VideoSurfaceRenderer.kt` contains the literal `THROWAWAY Wave 0b spike` comment marker
    - `VideoSurfaceRenderer.kt` contains the literal `END SPIKE BLOCK` comment marker
    - Spike is invoked on a new thread (file contains `Thread({` near the EglSpike call)
    - `spikeRan` field exists and is set to `true` before spike invocation (one-shot guarantee)
    - `./gradlew :app:assembleDebug` exits 0
  </acceptance_criteria>
  <done>App builds; spike will fire on next head-unit connection.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <what-built>
    EGL14 clear-to-red spike wired into VideoSurfaceRenderer. On the next Android Auto session, the head unit should briefly flash red (~1 second) immediately after the surface becomes available, then proceed with normal app behavior (which will currently show no video — that's Wave 3's job).
  </what-built>
  <how-to-verify>
    1. Build & install: `./gradlew :app:installDebug`
    2. Connect to Android Auto (emulator or head unit)
    3. Launch the app from the AA launcher
    4. Tap into a media source (LOCAL / PLEX / JELLYFIN) far enough that the player Surface is created — i.e. tap a video to start playback
    5. Watch for: head unit screen briefly turning solid RED for ~1 second on first surface availability
    6. Capture logcat: `adb logcat -d | grep -E "EglSpike|VideoSurfaceRenderer"`
    7. Logcat MUST contain `EglSpike: SPIKE SUCCESS` AND `VideoSurfaceRenderer: EGL clear-to-red spike result: ok=true`
    8. Logcat MUST NOT contain `eglCreateWindowSurface FAILED`

    Expected GL extension log line should include `GL_OES_EGL_image_external` somewhere in the GL_EXT output. Note this for Wave 1 confidence.

    If spike fails (no red, or `ok=false`):
    - Capture full logcat including all EglSpike lines
    - Note exact `eglGetError()` hex code reported
    - DO NOT proceed to Wave 1 — escalate per RESEARCH.md "Open Questions §1" mitigations (try without alpha channel, try ES3, etc.)

    If spike succeeds:
    - Reply `approved` so Wave 1 can begin (Plan 05 will delete the spike)
  </how-to-verify>
  <resume-signal>Type "approved" if head unit flashed red AND logcat shows `ok=true`. Otherwise describe what was observed (no red / wrong color / crash / specific eglGetError code).</resume-signal>
</task>

</tasks>

<verification>
- `EglSpike.kt` exists with THROWAWAY marker.
- `VideoSurfaceRenderer.kt` invokes spike on first surface arrival, on a dedicated thread.
- App compiles and installs.
- Head unit shows red flash on first Surface arrival.
- Logcat confirms `eglCreateWindowSurface` succeeded.
</verification>

<success_criteria>
- EGL14 confirmed working on Car App Surface — Option B architecture unblocked.
- `GL_OES_EGL_image_external` confirmed present in GL extensions string.
- Spike code is in place AND clearly marked THROWAWAY for Wave 1 deletion.
</success_criteria>

<output>
Create `.planning/phases/3-aspect-ratio/3-02-SUMMARY.md` documenting:
- Spike outcome (red flash observed: yes/no)
- `eglGetError()` codes seen (if any)
- GL_VENDOR / GL_RENDERER / GL_OES_EGL_image_external presence from logcat
- Confirmation that THROWAWAY markers are in place for Wave 1 deletion
</output>
