# Phase 3: Media Aspect Ratio Preservation - Research (Option B: GL Intermediary)

**Researched:** 2026-04-25
**Domain:** Custom OpenGL ES intermediate pipeline (SurfaceTexture + EGL14 + OES) on Android Auto NAVIGATION-category Surface
**Confidence:** HIGH on architecture and APIs; MEDIUM on Car App remote-Surface EGL behavior (verifiable in Wave 0 spike); LOW on protected-content (DRM) — out of scope for this phase
**Supersedes:** `.archive/3-RESEARCH.outdated.md` (assumed Media3 effects worked; empirically disproven on AA head unit)

## Summary

The previous Media3 `setVideoEffects(Presentation)` approach is empirically broken on Android Auto: the Car App `Surface` is a remote `IGraphicBufferProducer` proxy whose `setFrameRate()` binder method returns errno -38 ("Function not implemented"), which `Media3 DefaultVideoFrameProcessor` calls during its output-surface handshake — leaving `FinalShaderWrapper` with no output surface and dropping every decoded frame. There is no flag, callback ordering, or version bump in Media3 1.3.1 that works around this; the failure is structural to the Car App `Surface` implementation.

**Option B replaces the broken effects pipeline with a custom OpenGL ES 2.0 intermediary.** The shape is well-trodden: ExoPlayer's own `VideoProcessingGLSurfaceView` (now deprecated only because Media3 effects is its successor — the very pipeline that doesn't work for us) is the canonical reference. The architecture:

1. Create an EGL14 `EGLContext` + window surface targeting the Car App `Surface` (the remote-Surface concern only affects `setFrameRate`, not core EGL window creation — verifiable via Grafika-style spike).
2. Generate an OES external texture; wrap it in a `SurfaceTexture`; wrap that in `Surface(surfaceTexture)`.
3. Hand the **intermediate** Surface to ExoPlayer via `player.setVideoSurface(...)`. ExoPlayer's renderer sees a normal local Surface — `setFrameRate` becomes a no-op hint (not a hard error), matching the path TextureView and every GL video player on Android use.
4. On `OnFrameAvailableListener` → `updateTexImage()` (on GL thread) → compute vertex transform for FIT/FILL/STRETCH given video size, PAR, rotation, container size → draw textured quad applying `SurfaceTexture.getTransformMatrix()` × scaling → `eglSwapBuffers()` to push to the Car App's Surface.

This bypasses Media3's GL pipeline entirely — we only use ExoPlayer for decode and pass frames through our own GL stage.

**Primary recommendation:** Implement `GLVideoPipeline` as a self-contained class owning EGL state, OES texture, intermediate `SurfaceTexture`/`Surface`, and a dedicated `HandlerThread`. Wire it from `VideoSurfaceRenderer` (passes Car App Surface in/out) and `MediaPlayerManager` (gets the intermediate Surface). Pure-math (vertex transform & scaling) goes in `AspectRatioCalculator` and is unit-testable. Run a small EGL spike in Wave 0 before committing to the design — verify `eglCreateWindowSurface` succeeds on a Car App `Surface`. High prior confidence yes (every GL renderer on Android relies on this and the empirically observed failure is narrowly `setFrameRate`), but cheap to confirm.

<user_constraints>
## User Constraints (from CONTEXT.md)

No CONTEXT.md exists for this phase (replanning under integrated `/gsd:plan-phase` flow after the prior approach was invalidated). Constraints come from the phase prompt and project rules:

### Locked Decisions (from prompt and project)
- **Approach:** Option B — custom OpenGL/SurfaceTexture intermediary pipeline. Media3 `Presentation` effects are out (empirically broken on AA Surface).
- **CarAppService category:** NAVIGATION (non-negotiable per CLAUDE.md — MEDIA category sandboxes Surface access).
- **Versions pinned:** Media3 1.3.1, Kotlin 2.0.0, AGP 8.7.3, minSdk 29, compileSdk 35.
- **Scaling modes to support:** FIT (default, letterbox/pillarbox), FILL (crop), STRETCH. Already modeled as `MediaPlayerManager.ScalingMode` enum.
- **Surface lifecycle source of truth:** `VideoSurfaceRenderer` (it owns the Car App `SurfaceCallback`). The GL pipeline is driven by it.
- **`MediaPlayerManager.attachSurfaceAndEffects()` and `applyPresentationEffect()` are to be removed.** `ENABLE_PRESENTATION_EFFECTS` toggle and `APPLY_EFFECTS_BEFORE_SURFACE` toggle become dead code once Option B lands.

### Claude's Discretion
- Threading model for GL: dedicated `HandlerThread` vs. driving directly from `onFrameAvailable` callback. **Recommend HandlerThread** (see Pattern 2 below).
- Whether `AspectRatioCalculator` is extended (emit transform matrix) or a sibling util added (`VertexTransform`). **Recommend extending** — same input set, just a new output shape; keeps the existing CPU-rect output for logging/debug.
- EGL config exact specs (RGBA8888, depth/stencil 0, ES2 vs ES3). **Recommend ES2 + RGBA8888 + no depth/stencil** — ES2 ubiquitously available on AA hardware and OES external texture extension is ES2-defined.
- File layout under `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/` for the new GL classes. Discretion on naming.

### Deferred Ideas (OUT OF SCOPE for Phase 3)
- DRM/Widevine protected content. The OES path requires `EGL_PROTECTED_CONTENT_EXT` and a protected SurfaceTexture; not all hardware supports it; AA-specific behavior is unknown. Document as a known limitation; do not implement.
- HDR/wide-gamut color pipeline (HLG/PQ tone mapping). Beyond phase scope.
- Performance optimization beyond "drops no frames at the observed surface size 1816×1056." 4K@60fps margin is a future concern.
- Manual user-toggle UI for FIT/FILL/STRETCH. Phase 3 ships FIT only as default. Other modes are wired in code but no UI yet (per REQUIREMENTS.md Feature 4 "Out of Scope: Fit-to-screen modes").
- Replacing `MediaPlayerManager.clearVideoSurface()` Qualcomm 2s-block workaround. Option B inherently sidesteps it (see Pattern 6) but does not require the suppression code to be removed in this phase.
</user_constraints>

<phase_requirements>
## Phase Requirements

REQUIREMENTS.md uses Feature numbering, not REQ-IDs. Phase 3 implements **Feature 4: Media Aspect Ratio Preservation**.

| Feature | Description (from REQUIREMENTS.md) | Research Support |
|---------|------------------------------------|------------------|
| 4.1 | Detect video aspect ratio from ExoPlayer metadata | Already implemented — `MediaPlayerManager.onVideoSizeChanged` exposes `VideoSize` StateFlow with width/height/PAR/rotation. Reused as-is. |
| 4.2 | Calculate VideoSurfaceRenderer surface size based on aspect ratio | **Pattern 3** below — extend `AspectRatioCalculator` to emit a 4×4 vertex transform usable by GL (in addition to its existing CPU-rect output for logging). |
| 4.3 | Apply scaling to SurfaceTexture (or adjust surface bounds) | **Pattern 1 + 2** — intermediate SurfaceTexture is the receiving end; the **Car App Surface** is the displayed end. Scaling is a vertex matrix applied during the GL draw, not a SurfaceTexture resize. |
| 4.4 | Test with common formats: 16:9, 4:3, 21:9, 9:16 | **Validation Architecture** below — pure math verifiable in unit tests for all four; visual verification on head unit. |
| 4.5 | No black bars or pillarboxing visible (FILL mode) / native ratio (FIT) | FIT default ships with letterbox/pillarbox bars (the "no distortion" guarantee). FILL mode (no bars, content cropped) is a vertex-transform variant on the same pipeline. |

The acceptance criterion "no visible distortion or stretching" is the hard requirement; everything else is implementation detail.
</phase_requirements>

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Media3 ExoPlayer | 1.3.1 | Video decode + frame production. Reused unchanged. | Project-pinned. The intermediate Surface is a normal local Surface to ExoPlayer; no API changes. |
| `android.opengl.EGL14` | API 17+ (29 minSdk satisfies) | EGL context + window surface management | Modern Android EGL API. EGL10 is legacy. Grafika and Media3 both use EGL14. |
| `android.opengl.GLES20` | API 8+ | OpenGL ES 2.0 calls (shader compile, draw, texture binding) | ES 2.0 is the OES-external-texture baseline. ES 3.0 is unnecessary for our use case (one textured quad). |
| `android.graphics.SurfaceTexture` | API 11+ | Bridge BufferQueue → OES texture | Standard Android frame-to-texture path. `setOnFrameAvailableListener` drives the render loop. |
| `android.view.Surface` | API 1+ | Wraps the SurfaceTexture for ExoPlayer; received from Car App for output | `Surface(surfaceTexture)` constructor is the documented bridge. |
| `android.os.HandlerThread` | API 1+ | Dedicated GL thread | Standard pattern. Required because EGL context is thread-bound. |
| `GL_OES_EGL_image_external` (extension) | OpenGL ES 2.0 ext | Sample from BufferQueue-backed textures | The only way to sample SurfaceTexture content. Universally supported on Android. |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.car.app:app` | 1.7.0 | `SurfaceContainer.getSurface()` provides the output Surface | Already in use via `VideoSurfaceRenderer`. |
| Kotlin Coroutines | 1.8.1 | StateFlow surface state already exposed | Reused unchanged. |
| JUnit 4 (`testImplementation`) | 4.13.2 | Pure-math unit tests for vertex transform | **NEW** — no test framework currently configured. See Validation Architecture & Wave 0. |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Manual EGL14 setup | `GLSurfaceView.Renderer` | Doesn't apply — Car App doesn't expose a `View` hierarchy; we only get a `Surface`. `GLSurfaceView` requires being inside a window. |
| EGL14 + raw OpenGL | Filament | Filament is a sledgehammer for one quad. Adds 5+ MB to APK. No win for a single video pipeline. |
| Manual shader code | `Texture2dProgram.java` (Grafika, Apache 2) | Could vendor it. **Recommend rolling our own** — the shader is ~15 lines of GLSL; clearer to own it than vendor a 250-line Grafika class. |
| HandlerThread for GL | Driving `updateTexImage` from `onFrameAvailable` directly | `onFrameAvailable` is on an arbitrary thread; `updateTexImage` MUST run on the EGL-current thread. Calling from listener forces awkward thread hopping or makes the listener thread the EGL thread (fragile). HandlerThread is the documented pattern. |
| OpenGL ES 2.0 | OpenGL ES 3.0 | ES 3.0 buys us nothing for one textured quad. ES 2.0 is more universally tested on AA hardware. |
| OES external texture | Copy to GL_TEXTURE_2D via FBO | Pointless extra copy. Direct OES sampling is the canonical path. |
| Custom GL pipeline | Reverting to MEDIA category | MEDIA category breaks SurfaceCallback access entirely — even broken-effects no longer applies because there is no Surface. Locked decision, not a real alternative. |

**Installation:**

```bash
# No new runtime dependencies. All required APIs ship with Android.
# Test framework needs to be added (Wave 0):
#   testImplementation 'junit:junit:4.13.2'
#   testImplementation 'org.jetbrains.kotlin:kotlin-test-junit:2.0.0'
```

**Version verification:**

| Package | Stated version | Verified | Notes |
|---------|---------------|----------|-------|
| `androidx.media3:media3-exoplayer` | 1.3.1 | Pinned, do not upgrade | 1.5.x changes effects pipeline but doesn't fix Car App `setFrameRate` gap. |
| `androidx.car.app:app` | 1.7.0 | Pinned, do not upgrade | 1.7.0 is the AA-stable release. |
| EGL14 / GLES20 | API ≥17/8, all on 29+ | Built into platform | No version constraint. |
| `junit:junit` | 4.13.2 | Latest stable JUnit 4 | Skipping JUnit 5 (Android Gradle Plugin support is rougher; not worth it for ~5 test methods). |

## Architecture Patterns

### Recommended Project Structure

```
app/src/main/java/com/pscholer/autoplayer/
├── car/
│   └── surface/
│       ├── VideoSurfaceRenderer.kt        # MODIFIED: owns GLVideoPipeline lifecycle
│       └── gl/                             # NEW package
│           ├── GLVideoPipeline.kt          # Top-level orchestrator: EGL+texture+thread
│           ├── EglCore.kt                  # EGL14 context+display+window-surface helper
│           ├── OesTextureProgram.kt        # Shader compile + draw call (vertex+fragment)
│           └── GeometryQuad.kt             # FloatBuffer for the unit quad [-1,1]²
├── player/
│   └── MediaPlayerManager.kt               # MODIFIED: receives intermediate Surface, drops attachSurfaceAndEffects
└── util/
    └── AspectRatioCalculator.kt            # MODIFIED: adds vertex transform output
app/src/test/java/com/pscholer/autoplayer/
└── util/
    └── AspectRatioCalculatorTest.kt        # NEW: pure-math unit tests
```

### Pattern 1: SurfaceTexture as Intermediate Frame Sink

**What:** ExoPlayer writes decoded frames to a `Surface` that wraps a `SurfaceTexture` we own. The SurfaceTexture is bound to an OES external texture in our EGL context. Our render loop pulls each frame off the BufferQueue via `updateTexImage()` and draws it to the Car App's Surface.

**When to use:** Always, in this phase. This is the entire reason Option B works — ExoPlayer's renderer sees a local Surface with a working BufferQueue, sidestepping the Car App `Surface.setFrameRate` -38 errno.

**Example:**

```kotlin
// Source: derived from ExoPlayer VideoProcessingGLSurfaceView (release-v2, deprecated but
// architecturally identical) and Grafika EglCore patterns.
// https://github.com/google/ExoPlayer/blob/release-v2/demos/gl/src/main/java/com/google/android/exoplayer2/gldemo/VideoProcessingGLSurfaceView.java

// Inside GLVideoPipeline, on the GL thread, after EGL context is current:
val texId = createOesTexture()                // gen + bind GL_TEXTURE_EXTERNAL_OES
val surfaceTexture = SurfaceTexture(texId)
surfaceTexture.setOnFrameAvailableListener { st ->
    // Called on an arbitrary thread (frequently the codec's output thread).
    // Post a message to the GL HandlerThread; do NOT call updateTexImage here.
    glHandler.post { drawFrame() }
}
val intermediateSurface = Surface(surfaceTexture)
// Hand to ExoPlayer (on whatever thread we already use; player.setVideoSurface is
// thread-safe per Media3 docs):
playerManager.setVideoSurface(intermediateSurface)
```

**Critical sizing note:** `surfaceTexture.setDefaultBufferSize(w, h)` is **ignored by video producers** — ExoPlayer's video decoder will override the buffer size to the actual decoded video dimensions. Don't try to size the SurfaceTexture; it auto-sizes to the video. We size the *output* (the Car App Surface's EGLSurface viewport) and use a vertex transform to fit the video into it.

### Pattern 2: Dedicated GL HandlerThread

**What:** A `HandlerThread` named "GLVideoPipeline" hosts the EGL context. All GL calls — context creation, shader compile, `updateTexImage`, draw, `eglSwapBuffers`, teardown — run on it. `OnFrameAvailableListener` posts to it; `VideoSurfaceRenderer` callbacks (Car App main thread) post lifecycle messages to it.

**When to use:** Always for this kind of pipeline. EGL contexts are thread-bound (`eglMakeCurrent` ties them to one thread); `updateTexImage()` requires the EGL-current thread; running GL on the main thread blocks the Car App framework callbacks. There is no good reason to deviate.

**Example:**

```kotlin
// Source: Grafika ContinuousCaptureActivity / ExoPlayer VideoProcessingGLSurfaceView pattern.
class GLVideoPipeline {
    private val thread = HandlerThread("GLVideoPipeline").apply { start() }
    private val handler = Handler(thread.looper)

    fun start(carSurface: Surface, width: Int, height: Int, onIntermediateReady: (Surface) -> Unit) {
        handler.post {
            eglCore.makeCurrent(carSurface, width, height)   // create EGLDisplay/Context/WindowSurface
            program.compile()                                 // compile shaders, gen texture
            val intermediate = createIntermediateSurface()    // SurfaceTexture + Surface
            // Post back to caller's thread via Handler/main-thread:
            mainHandler.post { onIntermediateReady(intermediate) }
        }
    }

    fun release() {
        handler.post {
            program.release()
            eglCore.release()
        }
        thread.quitSafely()
    }
}
```

### Pattern 3: Vertex Transform for FIT / FILL / STRETCH

**What:** Compute a single 4×4 matrix applied in the vertex shader to a unit quad spanning `[-1,1]²` (NDC). The matrix encodes: rotation (0/90/180/270 degrees of `unappliedRotationDegrees`) → pixel-aspect-ratio adjustment → fit-or-fill scaling. Combined with `SurfaceTexture.getTransformMatrix()` (which handles OEM Y-flip and tex-coord remapping) applied in the texture-coordinate path, this produces correctly oriented, correctly aspected output.

**When to use:** On `onVideoSizeChanged`, `onVisibleAreaChanged`, or `setScalingMode` — anytime any of the inputs change. The matrix is pushed as a `glUniformMatrix4fv` and reused for every subsequent frame until inputs change again.

**Math:**

```
Inputs:
  videoW, videoH           (from ExoPlayer VideoSize.width/height)
  par                      (pixelWidthHeightRatio; 1.0 normal, ≠1 anamorphic)
  rotation                 (unappliedRotationDegrees ∈ {0,90,180,270})
  containerW, containerH   (Car App visible-area width/height)
  mode                     (FIT | FILL | STRETCH)

Step 1 — apply rotation to effective video dimensions:
  if rotation in (90, 270): effectiveVideoW, effectiveVideoH = videoH, videoW
  else:                     effectiveVideoW, effectiveVideoH = videoW, videoH

Step 2 — compute display aspect ratio (PAR-corrected):
  videoAspect     = (effectiveVideoW * par) / effectiveVideoH
  containerAspect = containerW / containerH

Step 3 — compute scale factors per mode:
  STRETCH: sx = 1.0,  sy = 1.0                         (fill the whole NDC quad — no aspect correction)
  FIT:     if videoAspect > containerAspect:           (video wider — fit width, letterbox top/bottom)
             sx = 1.0
             sy = containerAspect / videoAspect        (< 1 → vertical letterbox)
           else:                                       (video taller — fit height, pillarbox sides)
             sx = videoAspect / containerAspect
             sy = 1.0
  FILL:    if videoAspect > containerAspect:           (video wider — fill height, crop horizontally)
             sx = videoAspect / containerAspect        (> 1 → quad extends beyond NDC; OES texture clamps)
             sy = 1.0
           else:
             sx = 1.0
             sy = containerAspect / videoAspect

Step 4 — assemble matrix (column-major, OpenGL convention):
  // Rotation matrix (around Z, in degrees, clockwise as Android reports):
  R = rotateZ(rotation)
  // Scale matrix:
  S = scale(sx, sy, 1.0)
  // Final vertex matrix:
  M = S * R    (rotate first, then scale to fit)

Output: M as a 16-float column-major array → glUniformMatrix4fv(uMVPMatrix, ...)
```

**Notes:**
- The unit quad in attribute buffer is fixed: `(-1,-1), (1,-1), (-1,1), (1,1)` with tex-coords `(0,0), (1,0), (0,1), (1,1)`.
- The texture matrix `uTexMatrix` comes from `surfaceTexture.getTransformMatrix(...)` — DO NOT skip applying it. Some OEMs flip Y or use sub-rectangle sampling; not applying it gives upside-down or color-shifted output.
- For 21:9 in a 16:9 container under FIT: `sx=1.0, sy ≈ 0.762` → ~12% vertical letterbox top + ~12% bottom. Correct.
- For 9:16 (vertical) in a 16:9 container under FIT: `sx ≈ 0.316, sy=1.0` → wide pillarbox bars. Correct.
- For PAR=1.45 anamorphic 720×480 (NTSC widescreen) in 16:9: the `par` factor is what stretches it back to the intended ~16:9 display aspect. Without PAR, you'd get a squeezed 4:3-ish image.

**Example (Kotlin):**

```kotlin
// Source: derived from AspectRatioFrameLayout scaling logic + standard NDC vertex transform.
// Pure function — unit testable without any Android dependency.
fun computeVertexTransform(
    videoW: Int, videoH: Int,
    par: Float, rotation: Int,
    containerW: Int, containerH: Int,
    mode: ScalingMode
): FloatArray {
    val (effW, effH) = if (rotation == 90 || rotation == 270) videoH to videoW else videoW to videoH
    val safePar = if (par <= 0f) 1f else par
    val videoAspect = (effW * safePar) / effH.toFloat()
    val containerAspect = containerW.toFloat() / containerH

    val (sx, sy) = when (mode) {
        ScalingMode.STRETCH -> 1f to 1f
        ScalingMode.FIT -> if (videoAspect > containerAspect) {
            1f to (containerAspect / videoAspect)
        } else {
            (videoAspect / containerAspect) to 1f
        }
        ScalingMode.FILL -> if (videoAspect > containerAspect) {
            (videoAspect / containerAspect) to 1f
        } else {
            1f to (containerAspect / videoAspect)
        }
    }

    val m = FloatArray(16)
    Matrix.setIdentityM(m, 0)
    if (rotation != 0) Matrix.rotateM(m, 0, rotation.toFloat(), 0f, 0f, 1f)
    Matrix.scaleM(m, 0, sx, sy, 1f)
    return m
}
```

### Pattern 4: OES External Texture Sampling

**What:** Sample the SurfaceTexture-backed BufferQueue via the `samplerExternalOES` GLSL type, declared via the `GL_OES_EGL_image_external` extension. Bind via `GL_TEXTURE_EXTERNAL_OES` target.

**When to use:** Always, for any SurfaceTexture-fed frame. There is no alternative for direct video frame sampling.

**Example (the exact shader pair to ship):**

```glsl
// Source: Grafika Texture2dProgram.java FRAGMENT_SHADER_EXT
// https://github.com/google/grafika/blob/master/app/src/main/java/com/android/grafika/gles/Texture2dProgram.java

// Vertex shader (GL_VERTEX_SHADER):
uniform mat4 uMVPMatrix;     // our scaling+rotation matrix (Pattern 3)
uniform mat4 uTexMatrix;     // SurfaceTexture.getTransformMatrix() result
attribute vec4 aPosition;    // unit quad in NDC
attribute vec4 aTextureCoord;
varying vec2 vTextureCoord;
void main() {
    gl_Position = uMVPMatrix * aPosition;
    vTextureCoord = (uTexMatrix * aTextureCoord).xy;
}

// Fragment shader (GL_FRAGMENT_SHADER):
#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
void main() {
    gl_FragColor = texture2D(sTexture, vTextureCoord);
}
```

**Texture binding setup (one-time, after EGL context current):**

```kotlin
// Source: Grafika gen+bind pattern, ported.
val tex = IntArray(1)
GLES20.glGenTextures(1, tex, 0)
GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, tex[0])
GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
// Note: GL_TEXTURE_EXTERNAL_OES = 0x8D65 (constant, not in GLES20 class — define it locally).
```

### Pattern 5: EGL14 Window Surface from a Car App Surface

**What:** Use EGL14 to create a display + context + window surface targeting the Car App's `Surface` (handed to us via `SurfaceCallback.onSurfaceAvailable`). EGL config: RGBA8888, no depth, no stencil, ES 2 renderable. Recreate the window surface (not the context) on Car App surface destruction/availability cycles.

**When to use:** On `onSurfaceAvailable` (create/recreate window surface), on `onSurfaceDestroyed` (destroy window surface, retain context), on full pipeline release (destroy context too).

**Example:**

```kotlin
// Source: Grafika EglCore.java pattern, adapted for Kotlin.
// https://github.com/google/grafika/blob/master/app/src/main/java/com/android/grafika/gles/EglCore.java

private val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
EGL14.eglInitialize(display, IntArray(2), 0, IntArray(2), 0)

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
val numConfigs = IntArray(1)
EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0)

val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
val context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
val eglSurface = EGL14.eglCreateWindowSurface(display, configs[0], carAppSurface, surfaceAttribs, 0)
// ↑ This is the call that *might* misbehave on a remote AA Surface. Verify in Wave 0 spike.

EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)
GLES20.glViewport(0, 0, containerW, containerH)
```

**Verification spike (Wave 0):** Before committing to the full architecture, write a 30-line GL test that creates an EGL window surface from a Car App `Surface`, clears to red, swaps buffers. If the entire screen turns red, `eglCreateWindowSurface` works on AA's remote Surface and the rest of the architecture follows. If it errors out or shows nothing, we have a deeper problem and need to escalate (most likely: install custom EGLContextFactory like ExoPlayer's gl demo, or pursue a SurfaceView attached to an embedded child layer — neither well-trodden on AA).

### Pattern 6: Teardown Order

**What:** Strict ordering on shutdown to prevent Qualcomm codec hangs and EGL leaks.

**Order:**

```
1. (Main thread, from VideoSurfaceRenderer.onSurfaceDestroyed):
   playerManager.setVideoSurface(null)        // detach intermediate from ExoPlayer FIRST
   playerManager.player.stop()                // transition to IDLE — codec releases buffers

2. (Post to GL thread):
   surfaceTexture.setOnFrameAvailableListener(null)
   surfaceTexture.release()
   intermediateSurface.release()

3. (Still on GL thread):
   eglMakeCurrent(NO_DISPLAY, NO_SURFACE, NO_SURFACE, NO_CONTEXT)
   eglDestroySurface(display, eglSurface)     // destroy WINDOW surface (Car App side)
   // KEEP eglContext if Car App surface might come back (visible-area changes)

4. (On full release only, e.g. session end):
   eglDestroyContext(display, context)
   eglTerminate(display)
   handlerThread.quitSafely()
```

**Why this order matters (from empirical AA findings):** The 2-second Qualcomm `c2.qti.avc.decoder` cleanup block is triggered by `player.clearVideoSurface()` running while the codec still has buffers queued against the Car App Surface. **In Option B, `player.clearVideoSurface()` is never called on the Car App Surface** — only on our intermediate Surface (which is a normal local SurfaceTexture, not a remote BufferQueue). The Qualcomm cleanup path *might* still fire on the intermediate Surface; but observation in Grafika and Media3 GL demos suggests it does not (intermediate teardown is sub-100ms in practice). Verify in Wave 0; if the hang reappears, the existing `lastSurfaceTeardownMs` timeout-suppression in `MediaPlayerManager.onPlayerError` already handles it.

### Pattern 7: Reactive Recompute on Visible-Area / Video-Size Changes

**What:** The vertex transform (Pattern 3) depends on five inputs. When any changes, recompute. Don't recompute on every frame; cache the matrix and update via `glUniformMatrix4fv` only on input change.

**When to use:**

| Trigger | Source | Handler |
|---------|--------|---------|
| `onVideoSizeChanged` | `MediaPlayerManager.videoSize` StateFlow | Recompute matrix; push to GL thread |
| `onVisibleAreaChanged` | `VideoSurfaceRenderer.onVisibleAreaChanged` | Update `glViewport` AND recompute matrix |
| `setScalingMode` | User toggle (future) | Recompute matrix |
| `onSurfaceAvailable` (re-attach) | `VideoSurfaceRenderer.onSurfaceAvailable` | Recreate EGL window surface; recompute matrix |

**Example wiring (in `VideoSurfaceRenderer`):**

```kotlin
// Already has both: lastVideoSize and surfaceWidth/Height
// New: pipe both to GLVideoPipeline whenever either changes.
override fun onVisibleAreaChanged(visibleArea: Rect) {
    // ... existing visible-area handling ...
    glPipeline?.updateContainerSize(newW, newH)   // posts to GL thread
}

fun onVideoSizeChanged(videoSize: MediaPlayerManager.VideoSize) {
    // ... existing log + lastVideoSize cache ...
    glPipeline?.updateVideoSize(videoSize)        // posts to GL thread
}
```

### Anti-Patterns to Avoid

- **Calling `updateTexImage()` from `OnFrameAvailableListener` directly.** The listener fires on an arbitrary thread (often the codec thread). `updateTexImage` is GL-thread-only. Always post to the GL handler.
- **Using TextureView transformation matrices.** Car App doesn't expose a TextureView. Setting transforms on a phantom TextureView does nothing. (Anti-goal #1 from prompt.)
- **Wrapping in AspectRatioFrameLayout.** PlayerView is not used; AspectRatioFrameLayout requires the Android `View` hierarchy that AA's `Surface`-only model doesn't provide. (Anti-goal #2.)
- **Re-enabling Media3 `Presentation` effects in any code path.** Empirically broken; the `ENABLE_PRESENTATION_EFFECTS` toggle should be deleted, not flipped. (Anti-goal #3.)
- **Sizing the SurfaceTexture via `setDefaultBufferSize`.** Video producers ignore it; the codec auto-sizes. Sizing happens at the *vertex transform* (Pattern 3), not the texture.
- **Recreating the EGL context on every visible-area change.** Context is expensive (~100ms); only recreate the EGL **window surface**. Context survives.
- **Ignoring `getTransformMatrix()`.** Some OEMs deliver Y-flipped frames or sub-rectangle BufferQueue regions. Skipping the texture matrix produces upside-down or shifted output on those devices. Always apply it.
- **Re-rendering on every `requestRender()`-style tick.** Drive draws from `OnFrameAvailableListener` only — one render per produced frame. Drawing more wastes power; drawing less drops frames.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| OpenGL matrix math | Custom 4×4 multiply | `android.opengl.Matrix` (`setIdentityM`, `rotateM`, `scaleM`, `multiplyMM`) | Hardware-correct row/column ordering; avoids transpose bugs. |
| EGL boilerplate | Hand-written `eglChooseConfig` configs without reference | Copy Grafika `EglCore.java` config attributes verbatim | Subtle bugs (missing `EGL_RENDERABLE_TYPE`, wrong `EGL_NONE` terminator) silently produce broken contexts. |
| Shader compilation error checking | Skipping `glGetShaderiv` checks | Always `glGetShaderiv(shader, GL_COMPILE_STATUS, ...)` and log `glGetShaderInfoLog` on failure | A typo in the shader becomes a silent black screen. The compile-status check catches it at startup. |
| Frame-rate management | Manual frame timing / VSync loops | Driving render from `OnFrameAvailableListener` | Producer (codec) drives consumption naturally. Trying to add a timer creates dropped or duplicated frames. |
| Aspect-ratio math reinvention | Pixel-by-pixel viewport calculation | Pattern 3 vertex transform (NDC-space) | NDC math is dimensionless and survives viewport resizes for free. |
| `samplerExternalOES` workarounds | Trying to copy OES → 2D via FBO and sample 2D | Sample OES directly | Pointless extra texture copy. OES sampling is the canonical path used by every GL video player. |
| Thread-safe SurfaceTexture handoff | Locks, semaphores, double-buffered SurfaceTextures | One SurfaceTexture, listener posts to GL Handler | The handler queue *is* the synchronization. Adding more is a deadlock generator. |

**Key insight:** The Option B architecture is a 30-year-old well-trodden path (Grafika dates to 2013, ExoPlayer's GL demo to 2017). Every Android camera app, every video editor, every AR app does some variant of this. The novelty is **applying it on Android Auto's remote Surface**, not in any of the GL pieces. Roll-your-own beyond Pattern 3 (the only project-specific piece) is a recipe for subtle bugs.

## Runtime State Inventory

This phase introduces a new rendering pipeline; it does not migrate stored data, change service registrations, or alter secrets/env vars. State inventory categories:

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None — phase touches only in-process rendering state. No DataStore, ChromaDB, or SQLite changes. | None. |
| Live service config | None — Car App service manifest, intent filter, NAVIGATION category all unchanged. | None. |
| OS-registered state | None — no Task Scheduler / launchd / pm2 / systemd registrations. | None. |
| Secrets/env vars | None. | None. |
| Build artifacts | None — pure Kotlin/Gradle additions. AGP/Gradle caches will rebuild on next compile (incremental). | None — `./gradlew build` handles. |

**Code-level state to remove (not "data" but worth flagging):**
- `MediaPlayerManager.ENABLE_PRESENTATION_EFFECTS` — delete the const, delete the branches it gates.
- `MediaPlayerManager.APPLY_EFFECTS_BEFORE_SURFACE` — delete.
- `MediaPlayerManager.attachSurfaceAndEffects()` — delete; replace call sites with direct `player.setVideoSurface(intermediateSurface)`.
- `MediaPlayerManager.applyPresentationEffect()` — delete.
- The verbose-log diagnostic comment block at the top of `ENABLE_PRESENTATION_EFFECTS` — keep a one-liner reference to "Option B replaces Media3 effects; see GLVideoPipeline.kt" so future readers don't re-attempt effects.

## Common Pitfalls

### Pitfall 1: `eglCreateWindowSurface` Fails on Remote Surface
**What goes wrong:** The Car App's `Surface` is a remote `IGraphicBufferProducer` proxy (this is exactly why `setFrameRate` fails with errno -38). Some EGL implementations historically have refused to create window surfaces on remote BufferQueues.
**Why it happens:** Remote surfaces lack some binder methods that older EGL drivers query during config validation.
**How to avoid:** Run the Wave 0 EGL spike first. If it works (high prior — every modern Android device does this for Camera2 → preview Surface routing), proceed. If it doesn't, investigate `EGL_PROTECTED_CONTENT_EXT` is OFF, try without `EGL_ALPHA_SIZE`, try ES3 instead of ES2.
**Warning signs:** `EGL14.eglCreateWindowSurface` returns `EGL_NO_SURFACE` and `eglGetError()` returns `EGL_BAD_NATIVE_WINDOW` (0x300B) or `EGL_BAD_MATCH` (0x3009).
**Reference:** [eglCreateWindowSurface fails after updating to Android 10 — glutin#1223](https://github.com/rust-windowing/glutin/issues/1223) (different root cause but similar diagnostic flow).

### Pitfall 2: SurfaceTexture Frame Drops Under Load
**What goes wrong:** Frames decode but aren't drawn — `onFrameAvailable` fires, but by the time the GL thread runs `updateTexImage`, multiple frames have been produced and the BufferQueue silently drops the older ones.
**Why it happens:** `SurfaceTexture` BufferQueue depth is small (typically 3); if the GL thread is starved (busy compiling shaders, or stuck on another draw), the producer overruns the consumer.
**How to avoid:** Keep the `drawFrame` work tight (no shader compilation, no allocations). Compile shaders once at start. Pre-allocate `FloatBuffer` for vertex/tex coords. If you see drops on the head unit, profile with `systrace` and look for >16ms GL-thread gaps.
**Warning signs:** `BufferQueueProducer: dequeueBuffer: BufferQueue is abandoned` or pulsing video / micro-stutters.
**Reference:** [Architecture: SurfaceTexture — BufferQueue overflow handling](https://source.android.com/docs/core/graphics/arch-st).

### Pitfall 3: Forgetting `getTransformMatrix()` Application
**What goes wrong:** Video appears upside-down on some hardware (most common: Mali-GPU OEMs), or has a slight color/UV shift.
**Why it happens:** `SurfaceTexture` may apply a Y-flip or sub-rectangle transform at the BufferQueue level, encoded into a transform matrix you must multiply into your tex coords. Skipping this works on Qualcomm (their drivers don't flip) but fails on Mali, Intel HD, etc.
**How to avoid:** Always call `surfaceTexture.getTransformMatrix(uTexMatrix)` after every `updateTexImage()` and pass it to the shader's `uTexMatrix` uniform. The Grafika shader pair (Pattern 4) does this correctly.
**Warning signs:** Video upside-down on one specific head unit but correct on emulator; or correct in dev but flipped in production.
**Reference:** [SurfaceTexture.getTransformMatrix — Android source](https://source.android.com/docs/core/graphics/arch-st).

### Pitfall 4: PAR Ignored / Anamorphic Squeezed Output
**What goes wrong:** A 720×480 NTSC widescreen file (PAR ≈ 1.45, intended 16:9 display) shows up as 4:3-ish horizontally squeezed.
**Why it happens:** Computing aspect as `width / height` ignores PAR. Display aspect is `width × PAR / height`. The previous `AspectRatioCalculator` already handles this correctly — Pattern 3 must preserve it.
**How to avoid:** Always include `par` in the math (Pattern 3 step 2). Default to 1.0 if PAR ≤ 0 (some streams report 0 erroneously).
**Warning signs:** 720×480 anamorphic content looks 4:3-squeezed; 16:9 broadcast with PAR≠1 looks slightly wrong.
**Reference:** [ExoPlayer #41 — anamorphic pixelWidthHeightRatio](https://github.com/google/ExoPlayer/issues/41).

### Pitfall 5: Visible-Area Race vs. EGL Window Surface
**What goes wrong:** EGL window surface is created with the raw surface dimensions (1816×1056), then the visible area arrives (1384×956), but the EGL viewport stays at the raw size — so the video draws into the obscured region behind the AA template chrome.
**Why it happens:** `VideoSurfaceRenderer` already has the 150ms visible-area-attach timeout for the old Presentation effect. The new pipeline must use the same timing — defer EGL window-surface creation (or at least viewport setup) until visible-area is known.
**How to avoid:** Reuse the existing `pendingAttachFallback` mechanism in `VideoSurfaceRenderer`; the GL pipeline just receives `(carSurface, visibleW, visibleH)` once. On subsequent `onVisibleAreaChanged`, call `glViewport(0, 0, newW, newH)` and recompute the vertex transform — but DO NOT recreate the EGL window surface (it targets the raw `Surface`, which is unchanged).
**Warning signs:** Video visible only in upper-left corner; or correctly centered in raw-surface coordinates but cut off by the action strip.

### Pitfall 6: Ending Playback Hangs ~2 Seconds (Qualcomm)
**What goes wrong:** Already a known project pitfall — `player.clearVideoSurface()` blocks on Qualcomm c2.qti.avc.decoder cleanup for ~2 seconds at end-of-video.
**Why it happens:** Qualcomm's codec impl synchronously waits for buffers to drain when the surface detaches.
**How to avoid in Option B:** **Don't call `player.clearVideoSurface()` at all** — instead call `player.setVideoSurface(null)`. Both have similar effects; the latter is slightly faster on Qualcomm because it goes through the renderer's "swap surface" path rather than the "explicit teardown" path. The existing `lastSurfaceTeardownMs` suppression in `onPlayerError` already covers either approach.
**Warning signs:** Reproduce by playing video to natural end and then `pop()`-ing back. If the back transition stutters >500ms, Qualcomm cleanup is still firing.
**Reference:** Empirical observation in `MediaPlayerManager.kt` lines 200-217.

### Pitfall 7: GL Thread Lifecycle Leaks
**What goes wrong:** `HandlerThread` not quit; `EGLContext` not destroyed; native GL resources accumulate across session restarts.
**Why it happens:** Wave-zero implementations skip `quitSafely()` on the HandlerThread or skip `eglDestroyContext` because the app process exits soon after. On AA, the process can stay alive across many session connect/disconnect cycles, accumulating leaks.
**How to avoid:** Strict teardown order (Pattern 6). Tie GL pipeline lifecycle to `VideoSurfaceRenderer` instance lifetime, which is bound to the `AutoMediaSession` (per-AA-session). Release on session destroy.
**Warning signs:** OutOfMemoryError after several connect/disconnect cycles; `adb shell dumpsys gfxinfo` showing growing texture allocations.

## Code Examples

### Vertex Transform (Pure Kotlin, Unit-Testable)

```kotlin
// File: app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt
// Source: Pattern 3 above. Pure function — no Android imports beyond android.opengl.Matrix.
// Unit tests live in app/src/test/java/.../AspectRatioCalculatorTest.kt

import android.opengl.Matrix

object AspectRatioCalculator {
    enum class ScalingMode { FIT, FILL, STRETCH }

    /**
     * Compute the vertex transform for a unit quad in NDC, given video and container metrics.
     *
     * Returns a column-major 4×4 matrix to upload via glUniformMatrix4fv.
     * The matrix encodes (in order): rotation, then aspect-correcting scale.
     */
    fun computeVertexTransform(
        videoWidth: Int, videoHeight: Int,
        pixelAspectRatio: Float, rotationDegrees: Int,
        containerWidth: Int, containerHeight: Int,
        mode: ScalingMode
    ): FloatArray {
        if (videoWidth <= 0 || videoHeight <= 0 || containerWidth <= 0 || containerHeight <= 0) {
            return FloatArray(16).also { Matrix.setIdentityM(it, 0) }
        }
        val (effW, effH) = if (rotationDegrees == 90 || rotationDegrees == 270)
            videoHeight to videoWidth else videoWidth to videoHeight
        val par = if (pixelAspectRatio <= 0f) 1f else pixelAspectRatio
        val videoAspect = (effW * par) / effH.toFloat()
        val containerAspect = containerWidth.toFloat() / containerHeight

        val (sx, sy) = when (mode) {
            ScalingMode.STRETCH -> 1f to 1f
            ScalingMode.FIT ->
                if (videoAspect > containerAspect) 1f to (containerAspect / videoAspect)
                else (videoAspect / containerAspect) to 1f
            ScalingMode.FILL ->
                if (videoAspect > containerAspect) (videoAspect / containerAspect) to 1f
                else 1f to (containerAspect / videoAspect)
        }

        val m = FloatArray(16)
        Matrix.setIdentityM(m, 0)
        if (rotationDegrees != 0) Matrix.rotateM(m, 0, rotationDegrees.toFloat(), 0f, 0f, 1f)
        Matrix.scaleM(m, 0, sx, sy, 1f)
        return m
    }

    // Existing CPU-rect scaling output retained for logging/debug.
    fun calculateScaling(/* unchanged */): ScaledDimensions { /* ... */ }
}
```

### EGL Core Skeleton

```kotlin
// File: app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt
// Source: derived from Grafika EglCore.java (Apache 2.0). Minimal port — no protected/HDR.
// https://github.com/google/grafika/blob/master/app/src/main/java/com/android/grafika/gles/EglCore.java

class EglCore {
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var config: EGLConfig? = null
    private var windowSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    fun initContext() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(display !== EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed" }
        val v = IntArray(2)
        check(EGL14.eglInitialize(display, v, 0, v, 1)) { "eglInitialize failed" }

        val attribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0, EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val n = IntArray(1)
        check(EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, n, 0) && n[0] > 0) { "eglChooseConfig failed" }
        config = configs[0]

        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        check(context !== EGL14.EGL_NO_CONTEXT) { "eglCreateContext failed (err=${EGL14.eglGetError()})" }
    }

    fun createWindowSurface(carAppSurface: Surface) {
        val attribs = intArrayOf(EGL14.EGL_NONE)
        windowSurface = EGL14.eglCreateWindowSurface(display, config, carAppSurface, attribs, 0)
        check(windowSurface !== EGL14.EGL_NO_SURFACE) {
            "eglCreateWindowSurface failed on Car App Surface (err=0x${EGL14.eglGetError().toString(16)})"
        }
    }

    fun makeCurrent() {
        check(EGL14.eglMakeCurrent(display, windowSurface, windowSurface, context)) { "eglMakeCurrent failed" }
    }

    fun swapBuffers() = EGL14.eglSwapBuffers(display, windowSurface)

    fun destroyWindowSurface() {
        if (windowSurface !== EGL14.EGL_NO_SURFACE) {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(display, windowSurface)
            windowSurface = EGL14.EGL_NO_SURFACE
        }
    }

    fun release() {
        destroyWindowSurface()
        if (context !== EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
        if (display !== EGL14.EGL_NO_DISPLAY) EGL14.eglTerminate(display)
        context = EGL14.EGL_NO_CONTEXT
        display = EGL14.EGL_NO_DISPLAY
    }
}
```

### Per-Frame Draw Sketch

```kotlin
// Inside GLVideoPipeline.drawFrame(), running on the GL HandlerThread:
private fun drawFrame() {
    if (!frameAvailable) return
    surfaceTexture.updateTexImage()
    surfaceTexture.getTransformMatrix(texMatrix)   // 16-float

    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    GLES20.glUseProgram(programId)
    GLES20.glUniformMatrix4fv(uMvpMatrixLoc, 1, false, vertexMatrix, 0)
    GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0)
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, oesTexId)
    GLES20.glUniform1i(uTextureLoc, 0)
    quadGeometry.draw()                            // GL_TRIANGLE_STRIP, 4 verts
    eglCore.swapBuffers()
    frameAvailable = false
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| ExoPlayer GL demo (`VideoProcessingGLSurfaceView`) | Media3 effects pipeline (`setVideoEffects`) | Media3 1.0 (2023) | The "modern" replacement; works on regular Surfaces; **broken on Android Auto Surface** (this project's empirical finding April 2026). |
| Custom GL intermediary (Grafika pattern) | Same — still works | — | The deprecated/older path is what we use, because it predates Media3 effects' `setFrameRate` dependency. |
| OpenGL ES 2.0 | OpenGL ES 3.0 / Vulkan | ES 3.0 mainstream by 2017; Vulkan optional | Phase 3 uses ES 2.0 — sufficient for one textured quad and universally supported on AA hardware. |
| Manual texture matrix | Same — `SurfaceTexture.getTransformMatrix()` is still required | — | No replacement; OEMs still ship Y-flip and sub-rect tex transforms. |

**Deprecated / outdated:**

- **Media3 `Presentation` effects on Android Auto NAVIGATION-template Surface.** Documented as the "right" way in Media3 1.3+; empirically non-functional on AA per April 2026 logcat capture. Status: known-broken; no workaround at the Media3 layer.
- **`ExoPlayer.VideoProcessingGLSurfaceView`** (release-v2) is `@Deprecated` in favor of Media3 effects. Architecturally still valid as a *reference*; do not import the class itself (Media3-incompatible).
- **`setDefaultBufferSize` on a video-fed SurfaceTexture.** Worked in 2014; producers override since at least Android 5.

## Open Questions

1. **Does `eglCreateWindowSurface` succeed on Car App `Surface`?**
   - What we know: Car App Surface is a remote `IGraphicBufferProducer` proxy. `setFrameRate` fails -38; many other binder methods presumably work (the surface is functional for software canvas writes today).
   - What's unclear: Does EGL need any specific binder method that's also missing on the AA proxy?
   - Recommendation: **Wave 0 spike** — 30 lines of code to clear-to-red and swap, gates the rest of the architecture. High prior confidence yes (Camera2 → preview-Surface uses identical EGL path on every Android device).

2. **Does `setOnFrameAvailableListener` thread match the codec's release thread on AA?**
   - What we know: On phones, the listener fires on the codec's output thread (typically a single `media-codec-N` thread).
   - What's unclear: AA may serialize through additional binder hops; the thread identity may differ.
   - Recommendation: Log `Thread.currentThread().name` from the listener once during the spike. Doesn't change architecture either way (we always post to GL handler), but useful for debugging.

3. **Does Qualcomm's 2-second cleanup hang reproduce with `setVideoSurface(null)` on the intermediate surface?**
   - What we know: Hang is tied to `clearVideoSurface()` on the AA Surface (current code).
   - What's unclear: The hang root cause is codec-internal. Detaching from a *local* SurfaceTexture might still trigger it, just faster.
   - Recommendation: Test end-of-video → back → replay flow on the same head unit that exhibited the original 2s block. If still slow, keep the `lastSurfaceTeardownMs` suppression (it's harmless if unused).

4. **Performance margin at observed surface size (1816×1056) — and at hypothetical 4K?**
   - What we know: One textured quad with one OES sample per pixel is the cheapest GL workload imaginable; 1080p at 60fps takes <1ms on any modern GPU.
   - What's unclear: AA head units can be older Snapdragon SoCs (820-class) with limited GPU. 4K@60 (8.3MP/frame × 60Hz = 500Mtex/s) might saturate them.
   - Recommendation: **Out of scope for Phase 3.** Project source content is mostly local files / Plex / Jellyfin streams of typical 1080p-or-less. Re-evaluate when 4K becomes a concern.

5. **Protected (Widevine/DRM) content — known-broken or potentially workable?**
   - What we know: `EGL_PROTECTED_CONTENT_EXT` exists (ExoPlayer GL demo uses it); requires `GRALLOC_USAGE_PROTECTED` SurfaceTexture; OEM support is patchy.
   - What's unclear: AA-specific behavior with protected EGL contexts. Plex/Jellyfin streams in this project are not DRM-protected (they're user-owned files).
   - Recommendation: **Document as known limitation. Do not implement.** If a user later tries to play Widevine-protected content (highly unlikely use case for this app), the player will likely show a black screen or error. Acceptable.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| `android.opengl.EGL14` | EGL context creation | ✓ | API 17+ (29 minSdk) | None — ES2 fallback to GL10 not viable; Grafika is also EGL14. |
| `android.opengl.GLES20` | OpenGL ES 2.0 calls | ✓ | API 8+ | None. |
| `android.graphics.SurfaceTexture` | Frame sink + OES bridge | ✓ | API 11+ | None. |
| `android.os.HandlerThread` | GL thread | ✓ | API 1+ | None. |
| `GL_OES_EGL_image_external` extension | OES sampling | Universal on Android GPUs | — | None — extension is mandatory on Android since OES SurfaceTexture exists. |
| Android Auto head unit with EGL-capable GPU | Verification testing | Assumed ✓ | — | None — physical device required for validation. |
| JUnit 4 (`junit:junit:4.13.2`) | Vertex transform unit tests | ✗ — no test framework currently | — | **Wave 0 task**: add `testImplementation "junit:junit:4.13.2"` to `app/build.gradle`. Trivial. |
| Android Studio Profiler / `adb shell systrace` | Performance verification | ✓ | — | Not strictly required for Phase 3 scope. |

**Missing dependencies with no fallback:**
- None at runtime.

**Missing dependencies with fallback:**
- JUnit 4: **Wave 0 install task** — `testImplementation "junit:junit:4.13.2"` + `testImplementation "org.jetbrains.kotlin:kotlin-test-junit:2.0.0"`. ~5 minute task.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (4.13.2) — to be added in Wave 0 |
| Config file | None currently. After Wave 0: `app/build.gradle` `testImplementation` block. |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "*AspectRatioCalculator*"` (after Wave 0) |
| Full suite command | `./gradlew :app:testDebugUnitTest` (after Wave 0) |
| Phase gate | Full suite green AND visual verification on AA head unit (16:9 / 4:3 / 21:9 / 9:16). |

**Pure-math vs instrumented split:**
- **Pure-math (unit-testable, no Android device):** `AspectRatioCalculator.computeVertexTransform()`, `AspectRatioCalculator.calculateScaling()`. Deterministic FloatArray output; testable with `assertArrayEquals(expected, actual, delta)`. The vertex transform math IS the correctness criterion for FIT/FILL/STRETCH at every input combo.
- **Instrumented-only (head unit required):** Everything in the `gl/` package — `EglCore`, `OesTextureProgram`, `GLVideoPipeline`. EGL context creation, shader compile, OES texture binding, `eglSwapBuffers` to a real Car App Surface. None of this can be meaningfully tested without an AA head unit or AA emulator with surface support.
- **Integration-testable on AA emulator (not in CI):** End-to-end pipeline behavior — frame-available driving frame-drawn, teardown without leak, visible output for sample videos.

### Phase Requirements → Test Map

| Req | Behavior | Test Type | Automated Command | File Exists? |
|-----|----------|-----------|-------------------|-------------|
| 4.1 | Detect video aspect ratio from ExoPlayer | unit (existing) | (already covered by `MediaPlayerManager.onVideoSizeChanged` listener — no test, observed via logcat in dev builds) | n/a |
| 4.2 | Calculate vertex transform from video metadata | unit | `./gradlew :app:testDebugUnitTest --tests "*AspectRatioCalculator.computeVertexTransform*"` | ❌ Wave 0 |
| 4.2 | FIT mode: 16:9 video in 16:9 container → identity scale | unit | (above) | ❌ Wave 0 |
| 4.2 | FIT mode: 21:9 in 16:9 → vertical letterbox (sy < 1) | unit | (above) | ❌ Wave 0 |
| 4.2 | FIT mode: 9:16 in 16:9 → wide pillarbox (sx < 1) | unit | (above) | ❌ Wave 0 |
| 4.2 | PAR ≠ 1.0 anamorphic correctly applied | unit | (above) | ❌ Wave 0 |
| 4.2 | Rotation 90/180/270 effective dim swap | unit | (above) | ❌ Wave 0 |
| 4.2 | Edge case: zero/negative inputs → identity | unit | (above) | ❌ Wave 0 |
| 4.3 | EGL context creates on Car App Surface | manual / spike | adb logcat tag `GLVideoPipeline` shows `EGL initialized` and no `EGL_BAD_*` errors | ❌ Wave 0 spike |
| 4.3 | OES texture sampler renders frame on every `onFrameAvailable` | manual | logcat `GLVideoPipeline: drawFrame` count matches `onFrameAvailable` count | ❌ Wave 0 spike |
| 4.4 | 16:9 source plays without distortion | manual on head unit | visual | n/a |
| 4.4 | 4:3 source plays with pillarbox (FIT mode) | manual on head unit | visual | n/a |
| 4.4 | 21:9 source plays with letterbox (FIT mode) | manual on head unit | visual | n/a |
| 4.4 | 9:16 vertical source plays with wide pillarbox (FIT mode) | manual on head unit | visual | n/a |
| 4.5 | FILL mode crops correctly (no bars) for 16:9 in 21:9-container hypothetical | manual on head unit | visual | n/a (FIT only ships) |

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "*AspectRatioCalculatorTest*"` (Wave 0+) — runs in <2 seconds on JVM.
- **Per wave merge:** Full unit suite + manual smoke (build, install, play one local file, verify aspect ratio).
- **Phase gate:** Unit suite green AND head-unit verification matrix (16:9 / 4:3 / 21:9 / 9:16, all FIT mode) all visually correct.

### Wave 0 Gaps

- [ ] `app/build.gradle` — add `testImplementation "junit:junit:4.13.2"` and `testImplementation "org.jetbrains.kotlin:kotlin-test-junit:2.0.0"`.
- [ ] `app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt` — covers Feature 4.2 cases.
- [ ] EGL spike: `app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglSpike.kt` (temporary) — clear-to-red on Car App Surface, verify swap. Delete after validation.
- [ ] Logcat tag scheme: `GLVideoPipeline`, `EglCore`, `OesTextureProgram` for the three new classes. Set in companion objects.
- [ ] Verify head-unit GPU supports `GL_OES_EGL_image_external` (it always does on Android; cheap to confirm via `glGetString(GL_EXTENSIONS)` in spike).

## Sources

### Primary (HIGH confidence)

- **Architecture: SurfaceTexture (Android Open Source Project)** — [https://source.android.com/docs/core/graphics/arch-st](https://source.android.com/docs/core/graphics/arch-st) — BufferQueue model, `updateTexImage` threading, `getTransformMatrix` semantics.
- **Architecture: EGLSurfaces and OpenGL ES (AOSP)** — [https://source.android.com/docs/core/graphics/arch-egl-opengl](https://source.android.com/docs/core/graphics/arch-egl-opengl) — EGL window surface creation against ANativeWindow, BufferQueue producer side.
- **Grafika `EglCore.java`** — [https://github.com/google/grafika/blob/master/app/src/main/java/com/android/grafika/gles/EglCore.java](https://github.com/google/grafika/blob/master/app/src/main/java/com/android/grafika/gles/EglCore.java) — canonical Kotlin-portable EGL14 setup pattern.
- **Grafika `Texture2dProgram.java`** — [https://github.com/google/grafika/blob/master/app/src/main/java/com/android/grafika/gles/Texture2dProgram.java](https://github.com/google/grafika/blob/master/app/src/main/java/com/android/grafika/gles/Texture2dProgram.java) — exact OES vertex+fragment shader pair (Apache 2.0).
- **ExoPlayer `VideoProcessingGLSurfaceView.java` (release-v2)** — [https://github.com/google/ExoPlayer/blob/release-v2/demos/gl/src/main/java/com/google/android/exoplayer2/gldemo/VideoProcessingGLSurfaceView.java](https://github.com/google/ExoPlayer/blob/release-v2/demos/gl/src/main/java/com/google/android/exoplayer2/gldemo/VideoProcessingGLSurfaceView.java) — direct architectural model for Option B; deprecated only in favor of Media3 effects.
- **`SurfaceTexture` API reference** — [https://developer.android.com/reference/android/graphics/SurfaceTexture](https://developer.android.com/reference/android/graphics/SurfaceTexture) — `setOnFrameAvailableListener` thread contract, `setDefaultBufferSize` producer-override note.
- **EGL14 API reference** — [https://developer.android.com/reference/android/opengl/EGL14](https://developer.android.com/reference/android/opengl/EGL14) — `eglCreateWindowSurface` accepting `Surface` or `SurfaceTexture`.
- **Project source — `MediaPlayerManager.kt`, `VideoSurfaceRenderer.kt`, `AspectRatioCalculator.kt`** — current state of pipeline; existing `ScalingMode` enum, `VideoSize` data class, `VisibleArea` handling all reusable.

### Secondary (MEDIUM confidence)

- **Media3 `setVideoSurface` documentation** — [https://developer.android.com/media/media3/ui/surface](https://developer.android.com/media/media3/ui/surface) — confirms `setVideoSurface(Surface)` accepts arbitrary Surface (we leverage this with a SurfaceTexture-backed Surface).
- **Jernej Virag — "Playing video with effects using OpenGL on Android" (2014)** — [https://www.virag.si/2014/03/playing-video-with-effects-using-opengl-on-android/](https://www.virag.si/2014/03/playing-video-with-effects-using-opengl-on-android/) — pre-Grafika reference, good architectural overview, dated but principles unchanged.
- **Maninara — "Render camera preview using OpenGL ES 2.0" (2015)** — [https://www.maninara.com/2015/03/render-camera-preview-using-opengl-es.html](https://www.maninara.com/2015/03/render-camera-preview-using-opengl-es.html) — alternative structure for the same pattern.
- **Android Developers Blog — Media3 1.5.0 release notes (Jan 2025)** — [https://android-developers.googleblog.com/2025/01/media3-150-whats-new.html](https://android-developers.googleblog.com/2025/01/media3-150-whats-new.html) — confirms Media3 effects pipeline still depends on `DefaultVideoFrameProcessor` (which still calls `setFrameRate`), so upgrading Media3 is not the fix.

### Tertiary (LOW confidence — flagged for spike-validation)

- **EGL on remote Surface compatibility** — no authoritative source confirms Car App Surface supports `eglCreateWindowSurface` end-to-end. Inferred from: (a) Camera2 preview Surface is remote and works, (b) Media3's effects pipeline got *further* than failing at EGL — it failed at `setFrameRate` mid-pipeline, implying initial EGL setup succeeded against the Car App Surface in the broken-Media3 path. **Mitigation:** Wave 0 spike.
- **Qualcomm c2.qti.avc.decoder cleanup behavior on local SurfaceTexture detach** — empirical project knowledge says it triggers on Car App Surface; intermediate-Surface behavior is extrapolation. **Mitigation:** Test end-of-video → replay flow.

## Metadata

**Confidence breakdown:**
- **Standard Stack:** HIGH — all APIs are decade-old standard Android, used in dozens of widely-deployed apps.
- **Architecture (Pattern 1, 2, 4, 5, 6, 7):** HIGH — direct adaptation of Grafika and ExoPlayer GL demo, both of which are reference implementations.
- **Pattern 3 (Vertex transform math):** HIGH — pure linear algebra, unit-testable, derived from `AspectRatioFrameLayout` source.
- **Car App remote-Surface EGL behavior:** MEDIUM — high prior yes, but no authoritative source; **Wave 0 spike resolves**.
- **Pitfalls:** HIGH — pitfalls 1-6 are documented in linked sources or empirically observed in this project's logcat.
- **Validation Architecture:** HIGH — JUnit 4 + pure-math testing is uncontroversial. The instrumented gap is honestly documented.

**Research date:** 2026-04-25
**Valid until:** 2026-07-25 (90 days). Refresh triggers: (a) Media3 upgrade attempt, (b) AA Car App Library upgrade past 1.7.x, (c) project moves to Compose / different surface model.

## RESEARCH COMPLETE

**Phase:** 3 — Media Aspect Ratio Preservation (Option B: GL Intermediary)
**Confidence:** HIGH (with one MEDIUM gap closed by a 30-line Wave 0 spike)

### Key Findings

- **Option B is well-trodden architecture.** ExoPlayer's own deprecated `VideoProcessingGLSurfaceView` is the direct model; Grafika provides the EGL/OES building blocks. We are not inventing — we are porting a known-good pattern to AA.
- **The bypass works because the broken `setFrameRate` lives on the Car App Surface side, not the intermediate Surface.** ExoPlayer writes to a normal local Surface; Media3's renderer never touches the AA remote BufferQueue directly. We do — via EGL — but EGL doesn't call `setFrameRate`.
- **Pure-math (vertex transform) is unit-testable; GL pipeline is instrumented-only.** Wave 0 must add JUnit 4 (no test framework currently exists). The transform math IS the correctness criterion for FIT/FILL/STRETCH and can be validated without a head unit.
- **One unresolved unknown — does `eglCreateWindowSurface` succeed on Car App Surface — is cheaply testable.** A 30-line spike (clear-to-red, swap) gates the rest of the architecture. High prior yes; verify before commitment.
- **Existing `MediaPlayerManager` workarounds (Qualcomm 2s suppression, surface-deferred prepare) all stay** — Option B doesn't conflict with them; it sidesteps the trigger conditions for the Qualcomm hang in the natural-flow case but keeps the suppression as defense-in-depth.

### File Created

`C:\Users\peter\AndroidStudioProjects\Android auto media player\.planning\phases\3-aspect-ratio\3-RESEARCH.md`

### Confidence Assessment

| Area | Level | Reason |
|------|-------|--------|
| Standard Stack | HIGH | EGL14, GLES20, SurfaceTexture, OES extension are decade-stable Android. |
| Architecture (Patterns 1-7) | HIGH | Direct adaptation of Grafika + ExoPlayer GL demo. |
| Vertex transform math (Pattern 3) | HIGH | Pure linear algebra; unit-testable. |
| Car App remote-Surface EGL compatibility | MEDIUM | High prior yes; **resolved by Wave 0 spike**. |
| Pitfalls catalog | HIGH | 6 of 7 from authoritative sources or this project's empirical logcat. |
| Test infrastructure | MEDIUM | No framework exists today; Wave 0 adds JUnit 4 trivially. |

### Open Questions

1. EGL window surface creation on Car App Surface — **resolved by Wave 0 spike**.
2. Qualcomm 2s cleanup behavior with intermediate Surface detach — observe in head-unit testing; existing suppression is defense-in-depth.
3. Performance margin at 4K — out of scope; current content is 1080p-class.
4. Protected content (Widevine) — out of scope; documented as known limitation.

### Ready for Planning

Research complete. Planner can now create `3-PLAN.md`.

Suggested wave structure for the planner:
- **Wave 0:** Test framework install (JUnit 4) + EGL spike + delete dead `ENABLE_PRESENTATION_EFFECTS` code paths.
- **Wave 1:** Pure-math first — extend `AspectRatioCalculator.computeVertexTransform`, full unit test suite (parallelizable with Wave 0).
- **Wave 2:** GL building blocks — `EglCore`, `OesTextureProgram`, `GeometryQuad` — sequential because each tests on the head unit independently.
- **Wave 3:** Wire-up — `GLVideoPipeline` orchestrator, `VideoSurfaceRenderer` integration, `MediaPlayerManager` simplification (remove `attachSurfaceAndEffects` and effects branches).
- **Wave 4:** Manual aspect-ratio matrix verification on head unit (16:9 / 4:3 / 21:9 / 9:16). No code; sign-off only.
