---
phase: 3-aspect-ratio
plan: 05
type: execute
wave: 1
depends_on: ["02"]
files_modified:
  - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt
autonomous: true
requirements: [Feature-4.3]
must_haves:
  truths:
    - "Wave 0b throwaway spike code is fully deleted (EglSpike.kt removed; VideoSurfaceRenderer spike block gone)"
    - "EglCore.kt provides reusable EGL14 context + window-surface lifecycle"
    - "OesTextureProgram.kt compiles vertex+fragment shaders for samplerExternalOES"
    - "GeometryQuad.kt provides the unit-quad VBO/FloatBuffer for the textured draw"
    - "Each new file is independently compilable; project builds end-to-end"
  artifacts:
    - path: "app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt"
      provides: "EGL14 display + context + window-surface helper with proper teardown order"
      contains: "EGL14.eglCreateContext"
    - path: "app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt"
      provides: "Compiled vertex+fragment shader pair for samplerExternalOES; uniform locations"
      contains: "samplerExternalOES"
    - path: "app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt"
      provides: "Unit quad vertex+texcoord FloatBuffer ready for glVertexAttribPointer"
      contains: "FloatBuffer"
  key_links:
    - from: "OesTextureProgram"
      to: "GL_OES_EGL_image_external extension"
      via: "GLSL fragment shader #extension directive + samplerExternalOES uniform"
      pattern: "#extension GL_OES_EGL_image_external : require"
    - from: "EglCore.createWindowSurface"
      to: "Car App Surface"
      via: "EGL14.eglCreateWindowSurface(display, config, surface, ...)"
      pattern: "EGL14\\.eglCreateWindowSurface"
---

<objective>
Wave 1 (parallel with Plan 04): Productionize the Wave 0b throwaway spike into three small focused reusable GL classes that Wave 2's `GLVideoPipeline` (Plan 06) composes. Also delete the spike entirely.

Purpose:
- `EglCore` owns EGL14 lifecycle (display, context, window surface) with proper teardown ordering (RESEARCH.md Patterns 5+6). The context survives across Car App surface availability/destruction cycles; only the window surface is recreated.
- `OesTextureProgram` owns the shader pair from RESEARCH.md Pattern 4 verbatim (Grafika `Texture2dProgram` Apache-2.0 derived) — vertex + fragment for `samplerExternalOES`, with compile-status checks and explicit uniform locations.
- `GeometryQuad` owns the unit-quad vertex+texcoord buffers — pre-allocated direct ByteBuffers, ready for `glVertexAttribPointer`.

Output:
- Wave 0b spike code deleted (`EglSpike.kt` removed, spike block in `VideoSurfaceRenderer.kt` removed).
- Three new files in `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/`.
- Project builds successfully.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/3-aspect-ratio/3-RESEARCH.md
@CLAUDE.md
@app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt
@app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt
</context>

<tasks>

<task type="auto">
  <name>Task 1: Delete Wave 0b spike (EglSpike.kt + VideoSurfaceRenderer spike block)</name>
  <files>app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt, app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt</files>
  <read_first>
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt (the file being deleted)
    - app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt (locate THROWAWAY markers added in Plan 02 Task 2)
    - .planning/phases/3-aspect-ratio/3-PLAN-02-egl-spike.md (confirms exact markers used)
  </read_first>
  <action>
    1. Delete `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt` entirely.
    2. In `VideoSurfaceRenderer.kt`:
       - Remove the import line `import com.pscholer.autoplayer.car.surface.gl.EglSpike`
       - Remove the `private var spikeRan = false` field
       - Remove the entire block from `// ── THROWAWAY Wave 0b spike (Wave 1 Plan 05 Task 1 deletes this block) ──` through `// ── END SPIKE BLOCK ──` (both marker lines inclusive) inside `onSurfaceAvailable`
    3. Verify nothing else references `EglSpike` (grep across `app/src/`)
    4. `./gradlew :app:compileDebugKotlin` must succeed
  </action>
  <verify>
    <automated>! test -f app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt &amp;&amp; ! grep -r "EglSpike" app/src/main/java/ &amp;&amp; ! grep -q "THROWAWAY Wave 0b" app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt &amp;&amp; ./gradlew :app:compileDebugKotlin --quiet</automated>
  </verify>
  <acceptance_criteria>
    - `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt` does NOT exist
    - No file in `app/src/` contains `EglSpike` (case-sensitive grep)
    - `VideoSurfaceRenderer.kt` does NOT contain `THROWAWAY Wave 0b`
    - `VideoSurfaceRenderer.kt` does NOT contain `END SPIKE BLOCK`
    - `VideoSurfaceRenderer.kt` does NOT contain `spikeRan`
    - `./gradlew :app:assembleDebug` exits 0
  </acceptance_criteria>
  <done>Spike deleted; baseline restored.</done>
</task>

<task type="auto">
  <name>Task 2: Create EglCore.kt — EGL14 context + window-surface lifecycle</name>
  <files>app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt</files>
  <read_first>
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (§"Pattern 5: EGL14 Window Surface from a Car App Surface" — config attribs verbatim; §"Pattern 6: Teardown Order" — strict ordering; §"Code Examples → EGL Core Skeleton" — reference Kotlin port)
    - Grafika EglCore.java reference: https://github.com/google/grafika/blob/master/app/src/main/java/com/android/grafika/gles/EglCore.java
  </read_first>
  <action>
    Create new file `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt`.

    The class manages three resources separately so `GLVideoPipeline` can preserve the EGL context across Car App surface destruction/availability cycles (Pitfall 6 + Pattern 6 from RESEARCH.md).

    File contents (verbatim — derived from RESEARCH.md §"Code Examples → EGL Core Skeleton" with explicit lifecycle methods):

    ```kotlin
    package com.pscholer.autoplayer.car.surface.gl

    import android.opengl.EGL14
    import android.opengl.EGLConfig
    import android.opengl.EGLContext
    import android.opengl.EGLDisplay
    import android.opengl.EGLSurface
    import android.util.Log
    import android.view.Surface

    /**
     * EGL14 context + window-surface lifecycle helper. Single instance per GLVideoPipeline.
     *
     * Lifecycle:
     *   1. initContext() — creates display, chooses config, creates context (one-time)
     *   2. createWindowSurface(carSurface) — creates window surface against Car App Surface
     *      (recreated on every onSurfaceAvailable; previous one destroyed first)
     *   3. makeCurrent() — binds context+surface to current thread (must be called from
     *      the GL thread before any GLES20 call)
     *   4. swapBuffers() — presents the rendered frame to the Car App Surface
     *   5. destroyWindowSurface() — on Car App onSurfaceDestroyed; KEEPS context alive
     *   6. release() — full teardown (display, context, surface) on pipeline destroy
     *
     * Source: RESEARCH.md Pattern 5; derived from Grafika EglCore.java (Apache 2.0).
     */
    class EglCore {
        companion object { private const val TAG = "EglCore" }

        private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var context: EGLContext = EGL14.EGL_NO_CONTEXT
        private var config: EGLConfig? = null
        private var windowSurface: EGLSurface = EGL14.EGL_NO_SURFACE

        fun initContext() {
            check(display === EGL14.EGL_NO_DISPLAY) { "initContext called twice" }
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display !== EGL14.EGL_NO_DISPLAY) {
                "eglGetDisplay returned EGL_NO_DISPLAY (err=0x${EGL14.eglGetError().toString(16)})"
            }
            val v = IntArray(2)
            check(EGL14.eglInitialize(display, v, 0, v, 1)) {
                "eglInitialize failed (err=0x${EGL14.eglGetError().toString(16)})"
            }
            Log.i(TAG, "EGL initialized v${v[0]}.${v[1]}")

            val attribs = intArrayOf(
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
            check(EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, n, 0) && n[0] > 0) {
                "eglChooseConfig failed (err=0x${EGL14.eglGetError().toString(16)})"
            }
            config = configs[0]

            val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
            check(context !== EGL14.EGL_NO_CONTEXT) {
                "eglCreateContext failed (err=0x${EGL14.eglGetError().toString(16)})"
            }
        }

        /** Creates a window surface targeting the Car App Surface. Destroys any previous one first. */
        fun createWindowSurface(carAppSurface: Surface) {
            check(display !== EGL14.EGL_NO_DISPLAY) { "initContext not called" }
            destroyWindowSurface()
            val attribs = intArrayOf(EGL14.EGL_NONE)
            windowSurface = EGL14.eglCreateWindowSurface(display, config, carAppSurface, attribs, 0)
            check(windowSurface !== EGL14.EGL_NO_SURFACE) {
                "eglCreateWindowSurface failed on Car App Surface (err=0x${EGL14.eglGetError().toString(16)})"
            }
            Log.i(TAG, "Window surface created on Car App Surface")
        }

        /** Binds context+window surface to the calling thread. */
        fun makeCurrent() {
            check(EGL14.eglMakeCurrent(display, windowSurface, windowSurface, context)) {
                "eglMakeCurrent failed (err=0x${EGL14.eglGetError().toString(16)})"
            }
        }

        fun swapBuffers(): Boolean = EGL14.eglSwapBuffers(display, windowSurface)

        /** Tears down ONLY the window surface; keeps display+context alive for next attach. */
        fun destroyWindowSurface() {
            if (windowSurface !== EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(display, windowSurface)
                windowSurface = EGL14.EGL_NO_SURFACE
                Log.d(TAG, "Window surface destroyed (context preserved)")
            }
        }

        /** Full teardown: window surface + context + display. Idempotent. */
        fun release() {
            destroyWindowSurface()
            if (context !== EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(display, context)
                context = EGL14.EGL_NO_CONTEXT
            }
            if (display !== EGL14.EGL_NO_DISPLAY) {
                EGL14.eglTerminate(display)
                display = EGL14.EGL_NO_DISPLAY
            }
            config = null
            Log.i(TAG, "EglCore released")
        }
    }
    ```

    Critical requirements:
    - `initContext` and `release` are paired; intermediate `createWindowSurface`/`destroyWindowSurface` cycles must work.
    - All EGL error codes are logged in hex (binary debugging convention).
    - `release()` is idempotent (can be called multiple times safely).
  </action>
  <verify>
    <automated>test -f app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt &amp;&amp; grep -q "EGL14.eglCreateContext" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt &amp;&amp; grep -q "EGL14.eglCreateWindowSurface" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt &amp;&amp; grep -q "fun destroyWindowSurface" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt &amp;&amp; grep -q "fun release" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt &amp;&amp; ./gradlew :app:compileDebugKotlin --quiet</automated>
  </verify>
  <acceptance_criteria>
    - `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt` exists
    - File contains `EGL14.eglCreateContext`
    - File contains `EGL14.eglCreateWindowSurface`
    - File contains separate functions: `initContext`, `createWindowSurface`, `makeCurrent`, `swapBuffers`, `destroyWindowSurface`, `release` (six member functions)
    - File uses `EGL_OPENGL_ES2_BIT` (ES2, not ES3, per RESEARCH.md)
    - File uses RGBA8888 config (RED_SIZE=8, GREEN_SIZE=8, BLUE_SIZE=8, ALPHA_SIZE=8) and depth/stencil = 0
    - `./gradlew :app:compileDebugKotlin` exits 0
  </acceptance_criteria>
  <done>EglCore.kt compiled into the app; ready for OesTextureProgram and Wave 3 to use.</done>
</task>

<task type="auto">
  <name>Task 3: Create OesTextureProgram.kt — vertex+fragment shaders + uniform locations</name>
  <files>app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt</files>
  <read_first>
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (§"Pattern 4: OES External Texture Sampling" — exact GLSL pair to ship; texture binding setup)
    - Grafika Texture2dProgram.java reference: https://github.com/google/grafika/blob/master/app/src/main/java/com/android/grafika/gles/Texture2dProgram.java
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt (just-created)
  </read_first>
  <action>
    Create `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt`.

    File contents (the GLSL shaders are verbatim from RESEARCH.md Pattern 4):

    ```kotlin
    package com.pscholer.autoplayer.car.surface.gl

    import android.opengl.GLES20
    import android.util.Log

    /**
     * Compiles and owns the vertex + fragment shader pair for sampling a SurfaceTexture-backed
     * GL_TEXTURE_EXTERNAL_OES texture. Exposes attribute and uniform locations as fields so the
     * draw loop can bind them without re-querying.
     *
     * Source: RESEARCH.md Pattern 4 (derived from Grafika Texture2dProgram.java, Apache 2.0).
     */
    class OesTextureProgram {
        companion object {
            private const val TAG = "OesTextureProgram"
            // GL_TEXTURE_EXTERNAL_OES is not in GLES20; defined by the OES extension.
            const val GL_TEXTURE_EXTERNAL_OES = 0x8D65

            private const val VERTEX_SHADER = """
                uniform mat4 uMVPMatrix;
                uniform mat4 uTexMatrix;
                attribute vec4 aPosition;
                attribute vec4 aTextureCoord;
                varying vec2 vTextureCoord;
                void main() {
                    gl_Position = uMVPMatrix * aPosition;
                    vTextureCoord = (uTexMatrix * aTextureCoord).xy;
                }
            """

            private const val FRAGMENT_SHADER = """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;
                varying vec2 vTextureCoord;
                uniform samplerExternalOES sTexture;
                void main() {
                    gl_FragColor = texture2D(sTexture, vTextureCoord);
                }
            """
        }

        var programId: Int = 0; private set
        var aPositionLoc: Int = -1; private set
        var aTextureCoordLoc: Int = -1; private set
        var uMvpMatrixLoc: Int = -1; private set
        var uTexMatrixLoc: Int = -1; private set
        var uTextureLoc: Int = -1; private set

        var oesTextureId: Int = 0; private set

        /** Compile shaders + link program. Must be called on a thread with EGL context current. */
        fun compile() {
            check(programId == 0) { "compile() called twice" }
            val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
            val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
            programId = GLES20.glCreateProgram()
            check(programId != 0) { "glCreateProgram failed" }
            GLES20.glAttachShader(programId, vs)
            GLES20.glAttachShader(programId, fs)
            GLES20.glLinkProgram(programId)
            val link = IntArray(1)
            GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, link, 0)
            check(link[0] == GLES20.GL_TRUE) {
                "Program link failed: ${GLES20.glGetProgramInfoLog(programId)}"
            }
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)

            aPositionLoc      = GLES20.glGetAttribLocation(programId, "aPosition")
            aTextureCoordLoc  = GLES20.glGetAttribLocation(programId, "aTextureCoord")
            uMvpMatrixLoc     = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
            uTexMatrixLoc     = GLES20.glGetUniformLocation(programId, "uTexMatrix")
            uTextureLoc       = GLES20.glGetUniformLocation(programId, "sTexture")
            check(aPositionLoc >= 0 && aTextureCoordLoc >= 0) { "attribute locations missing" }
            check(uMvpMatrixLoc >= 0 && uTexMatrixLoc >= 0 && uTextureLoc >= 0) { "uniform locations missing" }

            // Generate the OES texture that SurfaceTexture will be bound to (GLVideoPipeline
            // creates the SurfaceTexture from this id).
            val tex = IntArray(1)
            GLES20.glGenTextures(1, tex, 0)
            oesTextureId = tex[0]
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, oesTextureId)
            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0)

            Log.i(TAG, "Program compiled (id=$programId, oesTex=$oesTextureId)")
        }

        fun release() {
            if (programId != 0) {
                GLES20.glDeleteProgram(programId)
                programId = 0
            }
            if (oesTextureId != 0) {
                val arr = intArrayOf(oesTextureId)
                GLES20.glDeleteTextures(1, arr, 0)
                oesTextureId = 0
            }
        }

        private fun compileShader(type: Int, src: String): Int {
            val s = GLES20.glCreateShader(type)
            check(s != 0) { "glCreateShader($type) failed" }
            GLES20.glShaderSource(s, src)
            GLES20.glCompileShader(s)
            val status = IntArray(1)
            GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) {
                val log = GLES20.glGetShaderInfoLog(s)
                GLES20.glDeleteShader(s)
                "Shader compile failed (type=$type): $log"
            }
            return s
        }
    }
    ```

    Critical: the GLSL fragment shader must contain `#extension GL_OES_EGL_image_external : require` AND `samplerExternalOES`. The vertex shader must contain both `uMVPMatrix` and `uTexMatrix` uniforms. The compile-status check on every shader is REQUIRED (RESEARCH.md "Don't Hand-Roll" — silent black screens come from skipped checks).
  </action>
  <verify>
    <automated>test -f app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt &amp;&amp; grep -q "samplerExternalOES" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt &amp;&amp; grep -q "GL_OES_EGL_image_external : require" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt &amp;&amp; grep -q "uMVPMatrix" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt &amp;&amp; grep -q "uTexMatrix" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt &amp;&amp; grep -q "GL_COMPILE_STATUS" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt &amp;&amp; ./gradlew :app:compileDebugKotlin --quiet</automated>
  </verify>
  <acceptance_criteria>
    - `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt` exists
    - File contains literal `#extension GL_OES_EGL_image_external : require`
    - File contains literal `samplerExternalOES`
    - File contains both `uMVPMatrix` and `uTexMatrix` uniform names
    - File contains `GL_COMPILE_STATUS` check (silent-black-screen prevention)
    - File defines `GL_TEXTURE_EXTERNAL_OES = 0x8D65` constant
    - File exposes uniform/attribute location fields: `aPositionLoc`, `aTextureCoordLoc`, `uMvpMatrixLoc`, `uTexMatrixLoc`, `uTextureLoc`, `oesTextureId`
    - File has `compile()` and `release()` functions
    - `./gradlew :app:compileDebugKotlin` exits 0
  </acceptance_criteria>
  <done>Shader program class compiled; uniform locations exposed for the draw loop.</done>
</task>

<task type="auto">
  <name>Task 4: Create GeometryQuad.kt — unit-quad vertex+texcoord buffers</name>
  <files>app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt</files>
  <read_first>
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (§"Pattern 3" notes about unit quad vertex coords; §"Code Examples → Per-Frame Draw Sketch" — quadGeometry.draw() usage)
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt (uses `aPosition` and `aTextureCoord` attribs — Quad must match)
  </read_first>
  <action>
    Create `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt`.

    File contents:

    ```kotlin
    package com.pscholer.autoplayer.car.surface.gl

    import android.opengl.GLES20
    import java.nio.ByteBuffer
    import java.nio.ByteOrder
    import java.nio.FloatBuffer

    /**
     * Pre-allocated direct buffers for a unit quad in NDC ([-1,1]²) with matching
     * texture coordinates. Drawn as GL_TRIANGLE_STRIP, 4 vertices.
     *
     * Vertex order matches tex-coord order:
     *   v0 = (-1,-1) tex (0,0)   bottom-left
     *   v1 = ( 1,-1) tex (1,0)   bottom-right
     *   v2 = (-1, 1) tex (0,1)   top-left
     *   v3 = ( 1, 1) tex (1,1)   top-right
     *
     * Source: RESEARCH.md Pattern 3 + Pattern 4 (vertex/tex-coord pairing).
     */
    class GeometryQuad {
        companion object {
            private const val FLOAT_SIZE = 4
            private const val POSITION_COMPONENTS = 2
            private const val TEXCOORD_COMPONENTS = 2

            private val VERTEX_DATA = floatArrayOf(
                -1f, -1f,
                 1f, -1f,
                -1f,  1f,
                 1f,  1f,
            )
            private val TEXCOORD_DATA = floatArrayOf(
                0f, 0f,
                1f, 0f,
                0f, 1f,
                1f, 1f,
            )
        }

        private val vertexBuffer: FloatBuffer = ByteBuffer
            .allocateDirect(VERTEX_DATA.size * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(VERTEX_DATA); position(0) }

        private val texCoordBuffer: FloatBuffer = ByteBuffer
            .allocateDirect(TEXCOORD_DATA.size * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(TEXCOORD_DATA); position(0) }

        /**
         * Bind buffers to the given attribute locations and issue the draw call.
         * Caller must have already called glUseProgram and bound textures/uniforms.
         */
        fun draw(aPositionLoc: Int, aTextureCoordLoc: Int) {
            GLES20.glEnableVertexAttribArray(aPositionLoc)
            GLES20.glVertexAttribPointer(
                aPositionLoc, POSITION_COMPONENTS, GLES20.GL_FLOAT, false,
                POSITION_COMPONENTS * FLOAT_SIZE, vertexBuffer
            )
            GLES20.glEnableVertexAttribArray(aTextureCoordLoc)
            GLES20.glVertexAttribPointer(
                aTextureCoordLoc, TEXCOORD_COMPONENTS, GLES20.GL_FLOAT, false,
                TEXCOORD_COMPONENTS * FLOAT_SIZE, texCoordBuffer
            )
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(aPositionLoc)
            GLES20.glDisableVertexAttribArray(aTextureCoordLoc)
        }
    }
    ```

    Critical:
    - Buffers are direct (`allocateDirect` + `nativeOrder()`) — required by GLES20 for `glVertexAttribPointer` to read them.
    - Vertex and texcoord arrays are paired index-by-index — vertex i goes with texcoord i.
    - 4 verts drawn as `GL_TRIANGLE_STRIP` — most efficient way to draw a quad in ES.
  </action>
  <verify>
    <automated>test -f app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt &amp;&amp; grep -q "FloatBuffer" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt &amp;&amp; grep -q "allocateDirect" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt &amp;&amp; grep -q "GL_TRIANGLE_STRIP" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt &amp;&amp; grep -q "fun draw" app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt &amp;&amp; ./gradlew :app:compileDebugKotlin --quiet</automated>
  </verify>
  <acceptance_criteria>
    - `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt` exists
    - File contains `FloatBuffer` and `ByteBuffer.allocateDirect`
    - File contains `nativeOrder()`
    - File contains `GL_TRIANGLE_STRIP`
    - File contains a `draw(aPositionLoc: Int, aTextureCoordLoc: Int)` function signature
    - File defines vertex coords spanning `[-1, 1]²` (literal `-1f, -1f` and `1f, 1f` present)
    - File defines tex coords spanning `[0, 1]²` (literal `0f, 0f` and `1f, 1f` present)
    - `./gradlew :app:assembleDebug` exits 0
  </acceptance_criteria>
  <done>Geometry helper compiled; ready for orchestrator.</done>
</task>

</tasks>

<verification>
- Spike code fully removed (EglSpike.kt deleted; VideoSurfaceRenderer markers gone).
- Three new GL files exist and compile.
- `./gradlew :app:assembleDebug` exits 0.
- All grep-able acceptance markers (samplerExternalOES, uMVPMatrix, GL_TRIANGLE_STRIP, etc.) present.
</verification>

<success_criteria>
- `EglCore`, `OesTextureProgram`, `GeometryQuad` are independently usable building blocks.
- Wave 3 (Plan 06) can compose them in `GLVideoPipeline` without writing fresh GL.
- App still builds and runs (no behavior change yet — wiring happens in Wave 3).
</success_criteria>

<output>
Create `.planning/phases/3-aspect-ratio/3-05-SUMMARY.md` summarizing:
- Spike deletion (file removed, markers gone, no remaining `EglSpike` references in source tree)
- New file inventory (3 files in gl/ package; rough line counts)
- Confirmation that grep markers from acceptance criteria are present
- Note that Plan 06 will compose these into `GLVideoPipeline`
</output>
