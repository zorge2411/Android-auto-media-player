---
phase: 03-aspect-ratio
plan: "06"
subsystem: gl
tags: [opengl, pipeline, surfacetexture, exoplayer, aspect-ratio]
requires: [EglCore, OesTextureProgram, GeometryQuad, AspectRatioCalculator]
provides: [GLVideoPipeline, VideoSurfaceRenderer-wired, ScalingMode-canonical]
affects:
  - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt
  - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
tech-stack:
  added: []
  patterns: [gl-handlerthread, surfacetexture-intermediary, oes-tex-matrix, reactive-recompute]
key-files:
  created:
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GLVideoPipeline.kt
  modified:
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt
    - app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt
    - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
key-decisions:
  - OesTextureProgram updated to own OES texture name (passed to SurfaceTexture constructor) and expose public location fields
  - uTexMatrix added to vertex shader — SurfaceTexture.getTransformMatrix() applied per-frame for correct UV mapping
  - GeometryQuad.draw(posLoc, texCoordLoc) added — encapsulates bind + DrawArrays + disable in one call
  - MediaPlayerManager.ScalingMode local enum deleted; canonical home is AspectRatioCalculator.ScalingMode
  - setScalingMode UI-wiring (MediaPlayerManager → VideoSurfaceRenderer → GLVideoPipeline) deferred — FIT default ships
requirements-completed: [Feature-4.2, Feature-4.3, Feature-4.4, Feature-4.5]
duration: "25 min"
completed: "2026-04-27"
---

# Phase 3 Plan 06: GL Pipeline Integration Summary

Composed the Wave 1 building blocks into the live render pipeline. ExoPlayer now renders into a `SurfaceTexture`-backed intermediate `Surface`; `GLVideoPipeline` draws each available frame via GLES2 + `AspectRatioCalculator.computeVertexTransform` (FIT mode default) onto the Car App Surface via `eglSwapBuffers`.

**Build:** green (`assembleDebug`) | **Unit tests:** 13/13 passed (AspectRatioCalculatorTest) | **Files:** 1 created, 4 modified | **Commit:** b053103

## GLVideoPipeline shape

~175 lines. Key fields: `HandlerThread`, `EglCore`, `OesTextureProgram`, `GeometryQuad`, `SurfaceTexture`, `vertexMatrix[16]`, `texMatrix[16]`.

Six lifecycle methods, all `glHandler.post { ... }`:
- `attach(carSurface, w, h, onIntermediateReady)` — init on first call, replace window surface on reattach
- `setVideoSize(videoSize)` — recompute vertex matrix + redraw if frame pending
- `setVisibleArea(w, h)` — recompute + redraw if frame pending
- `setScalingMode(mode)` — recompute + redraw if frame pending
- `detach()` — destroy window surface only (context preserved)
- `release()` — full teardown, then `thread.quitSafely()`

## VideoSurfaceRenderer wiring

Every Car App Surface lifecycle event now routes through `glPipeline`:

| Call site | Before | After |
|-----------|--------|-------|
| `attachPendingSurface` | `playerManager.setVideoSurface(carSurface)` | `glPipeline.attach(carSurface) { intermediate → playerManager.setVideoSurface(intermediate) }` |
| `onVisibleAreaChanged` (else branch) | — | `glPipeline.setVisibleArea(w, h)` |
| `onSurfaceDestroyed` | — | `glPipeline.detach()` before clearVideoSurface |
| `onVideoSizeChanged` | — | `glPipeline.setVideoSize(videoSize)` |
| `onConfigurationChanged` | `playerManager.setVideoSurface(carSurface)` | `glPipeline.attach(carSurface) { ... }` |
| new `release()` | — | `glPipeline.release()` |

ExoPlayer never receives the Car App Surface directly.

## MediaPlayerManager changes

- Removed local `enum class ScalingMode { FIT, FILL, STRETCH }`
- Added `import com.pscholer.autoplayer.util.AspectRatioCalculator`
- `_scalingMode` / `scalingMode` / `setScalingMode` now typed `AspectRatioCalculator.ScalingMode`
- All surface-lifecycle invariants preserved: `lastSurfaceTeardownMs`, `pendingPlay`/`pendingSurface`, `TEARDOWN_TIMEOUT_WINDOW_MS`

## Ready for

Plan 03-07: Head unit verification — install build, play video, confirm aspect ratio is preserved (no stretch/squish on head unit display).
