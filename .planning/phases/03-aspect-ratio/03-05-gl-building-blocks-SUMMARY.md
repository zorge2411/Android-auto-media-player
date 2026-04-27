---
phase: 03-aspect-ratio
plan: "05"
subsystem: gl
tags: [opengl, egl, gles2, oes-texture, geometry]
requires: [egl14-api, gles20-api]
provides: [EglCore, OesTextureProgram, GeometryQuad]
affects:
  - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt
tech-stack:
  added: []
  patterns: [egl14-lifecycle, gles2-shader, oes-external-texture, triangle-strip-quad]
key-files:
  created:
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/EglCore.kt
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/OesTextureProgram.kt
    - app/src/main/java/com/pscholer/autoplayer/car/surface/gl/GeometryQuad.kt
key-decisions:
  - GL_TEXTURE_EXTERNAL_OES (0x8D65) used as literal int — GLES20 class does not export this constant
  - OES extension declared in fragment shader header (#extension GL_OES_EGL_image_external require)
  - GeometryQuad uses Java-heap FloatBuffers (no VBO) — sufficient for 4-vertex unit quad
  - Task 1 (delete EglSpike.kt) was no-op — Plan 02 checkpoint was never executed, spike never existed
requirements-completed: [Feature-4.1, Feature-4.3]
duration: "12 min"
completed: "2026-04-27"
---

# Phase 3 Plan 05: GL Building Blocks Summary

Created the three low-level GL primitives that compose into `GLVideoPipeline` (Plan 06):

- **EglCore** — EGL14 display/context/config created once in `initContext()`; window surface re-created per `onSurfaceAvailable` without context loss; idempotent `release()` for full teardown.
- **OesTextureProgram** — compiles vertex + OES fragment shaders, links program, caches attribute/uniform locations. `draw(mvp, quad, textureId)` binds the OES texture, uploads the MVP matrix from `AspectRatioCalculator`, and issues `glDrawArrays(GL_TRIANGLE_STRIP, 4)`.
- **GeometryQuad** — allocates two native `FloatBuffer`s (NDC positions + [0,1] tex coords) for a 4-vertex TRIANGLE_STRIP unit quad covering [-1,1]².

All three files compile clean against Android SDK 35 (`gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL).

**Duration:** 12 min | **Tasks:** 4/4 (Task 1 no-op) | **Files:** 3 created | **Commits:** 2

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Delete EglSpike.kt (no-op — never created) | — | — |
| 2 | Create EglCore.kt — EGL14 lifecycle | 87e58f6 | EglCore.kt |
| 3 | Create OesTextureProgram.kt — GLES2 OES shader | 54c761e | OesTextureProgram.kt |
| 4 | Create GeometryQuad.kt — unit quad buffers | 54c761e | GeometryQuad.kt |

## Deviations from Plan

**[Minor] Task 1 was a no-op** — Plan 02 (EGL spike) is a checkpoint plan that was never executed; `EglSpike.kt` does not exist. Skipped delete step, proceeded directly to Task 2.

## Ready for

Wave 2: Plan 03-06 (GLVideoPipeline) composes these three primitives into the full render loop driven by `SurfaceTexture.OnFrameAvailableListener`.
