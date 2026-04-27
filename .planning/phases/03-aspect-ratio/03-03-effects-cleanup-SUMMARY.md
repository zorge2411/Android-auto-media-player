---
phase: 03-aspect-ratio
plan: "03"
subsystem: player
tags: [refactor, cleanup, exoplayer]
requires: []
provides: [clean-mediaplayer-baseline]
affects:
  - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
tech-stack:
  added: []
  patterns: [direct-surface-attachment]
key-files:
  created: []
  modified:
    - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
key-decisions:
  - setScalingMode() stubbed with log-only body — GLVideoPipeline wiring deferred to Plan 06
requirements-completed: []
duration: "8 min"
completed: "2026-04-27"
---

# Phase 3 Plan 03: Effects Cleanup Summary

Deleted all dead Media3 `Presentation` effects scaffolding from `MediaPlayerManager.kt`: removed the `ENABLE_PRESENTATION_EFFECTS` / `APPLY_EFFECTS_BEFORE_SURFACE` constants and their 12-line explanatory comment, removed the `attachSurfaceAndEffects()` helper and `applyPresentationEffect()` function (~60 lines total), inlined direct `player.setVideoSurface(surface)` at 3 call sites, and replaced `setScalingMode()` with a 3-line stub. File compiles clean.

**Duration:** 8 min | **Tasks:** 2/2 | **Files:** 1 modified (~-70 net lines)

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 | Remove consts, comment block, Presentation import | 6d49ae9 | MediaPlayerManager.kt |
| 2 | Delete helper functions, inline call sites, clean clearVideoSurface/setOutputSize | 6d49ae9 | MediaPlayerManager.kt |

## Preserved invariants

- `lastSurfaceTeardownMs` + `TEARDOWN_TIMEOUT_WINDOW_MS` suppression in `onPlayerError` — intact
- `pendingPlay` / `pendingSurface` deferred-prepare path — intact
- `enum class ScalingMode { FIT, FILL, STRETCH }` — intact (Wave 3 reuses it)
- `setScalingMode()` — present as stub logging "no-op until Wave 3 GLVideoPipeline wires it"
- `surfaceWidth` / `surfaceHeight` fields — intact (Wave 3 GLVideoPipeline reads these)

## Deleted symbols

- `ENABLE_PRESENTATION_EFFECTS` const
- `APPLY_EFFECTS_BEFORE_SURFACE` const
- `attachSurfaceAndEffects(surface: Surface)` private fun
- `applyPresentationEffect()` private fun
- `import androidx.media3.effect.Presentation`
- Dead `if (ENABLE_PRESENTATION_EFFECTS)` branches in `clearVideoSurface()` and `setOutputSize()`

## Deviations from Plan

None — plan executed exactly as written.

## Ready for

Wave 2: Plan 03-06 (GLVideoPipeline integration) has a clean baseline with no dead branches.
