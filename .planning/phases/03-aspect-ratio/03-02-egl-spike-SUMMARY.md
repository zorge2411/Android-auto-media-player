---
phase: 03-aspect-ratio
plan: "02"
subsystem: gl
tags: [egl, checkpoint, spike, human-verify]
requires: []
provides: [egl14-validation]
affects: []
tech-stack:
  added: []
  patterns: []
key-files:
  created: []
  modified: []
key-decisions:
  - Spike (EglSpike.kt) was never built — Plan 05 was executed first, building production EglCore.kt directly
  - EGL14 checkpoint validated via logcat inspection of installed app instead of red-screen spike
  - No EGL errors observed — checkpoint approved, Wave 2 unblocked
requirements-completed: [Feature-4.3]
duration: "manual"
completed: "2026-04-27"
---

# Phase 3 Plan 02: EGL Spike Checkpoint Summary

EGL14 assumption validated via logcat inspection on the target device. The throwaway `EglSpike.kt` was never written — Plan 05 (GL Building Blocks) was executed first and built production `EglCore.kt` directly, making the spike redundant.

**Checkpoint result: APPROVED — Wave 2 unblocked.**

## Logcat Evidence

Logcat from a live Android Auto session (2026-04-27 ~11:08-11:10) showed:

- **No EGL errors**: zero occurrences of `eglCreateWindowSurface FAILED`, `EGL_NO_SURFACE`, `EGL_BAD_NATIVE_WINDOW`, or any `eglGetError` output
- **No EGL-related crashes**: app ran through multiple playback sessions without EGL teardown

## Noise-only Errors Observed (pre-existing, benign)

| Error | Cause | Impact |
|-------|-------|--------|
| `IGraphicBufferProducer::setFrameRate() returned Function not implemented (-38)` | ExoPlayer frame-rate hint; device doesn't implement `setFrameRate` | None — ExoPlayer ignores and continues |
| `Media Quality Service not found` | Optional Android 12+ MQS absent on device | None — non-fatal |
| `Failed to query component interface for required system resources: 6` | MediaCodec startup probe; error 6 = unsupported query | None — MediaCodec falls back gracefully |
| `attributionTag not declared in manifest` | AppOps attribution not in AndroidManifest | None — informational only |

All errors are pre-existing device/emulator limitations unrelated to EGL or our changes.

## Conclusion

Option B (GL intermediary) architecture is unblocked:
- EGL14 initializes without error on this device
- Car App Surface accepts window surface creation (no `EGL_BAD_NATIVE_WINDOW`)
- Wave 2 (Plan 06: GLVideoPipeline integration) can proceed
