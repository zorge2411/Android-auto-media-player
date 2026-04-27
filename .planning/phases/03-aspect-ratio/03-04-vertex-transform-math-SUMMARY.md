---
phase: 03-aspect-ratio
plan: "04"
subsystem: util
tags: [math, tdd, junit, aspect-ratio]
requires: [junit4-test-classpath]
provides: [computeVertexTransform, ScalingMode-enum]
affects:
  - app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt
  - app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt
tech-stack:
  added: []
  patterns: [tdd, pure-kotlin-matrix-math]
key-files:
  created:
    - app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt
  modified:
    - app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt
key-decisions:
  - Used pure-Kotlin cos/sin instead of android.opengl.Matrix — avoids Android stub dependency in JVM tests while producing identical output
  - ScalingMode enum canonically in AspectRatioCalculator (Wave 3 will import from here)
requirements-completed: [Feature-4.2, Feature-4.4]
duration: "10 min"
completed: "2026-04-27"
---

# Phase 3 Plan 04: Vertex Transform Math Summary

Implemented `AspectRatioCalculator.computeVertexTransform()` (pure Kotlin, no Android deps) returning a 16-float column-major 4×4 matrix encoding rotation-then-scale for the GL pipeline. Added `enum class ScalingMode { FIT, FILL, STRETCH }` as the canonical home. All 13 JUnit 4 unit tests pass on JVM.

**Duration:** 10 min | **Tasks:** 2/2 (TDD RED+GREEN) | **Tests:** 13/13 passed | **Files:** 2

## Tasks Completed

| # | Task | Commit | Files |
|---|------|--------|-------|
| 1 (RED) | Write 13 failing AspectRatioCalculatorTest methods | 2744f36 | AspectRatioCalculatorTest.kt |
| 2 (GREEN) | Implement computeVertexTransform + ScalingMode | 65ed0c1 | AspectRatioCalculator.kt |

## Test coverage

- STRETCH → identity scale regardless of aspect ✓
- FIT 16:9 in 16:9 → identity ✓
- FIT 4:3 in 16:9 → pillarbox (sx=0.75) ✓
- FIT 21:9 in 16:9 → letterbox ✓
- FIT 9:16 in 16:9 → wide pillarbox ✓
- FILL 4:3 in 16:9 → extends vertically ✓
- FILL 21:9 in 16:9 → extends horizontally ✓
- PAR=1.45 anamorphic 720×480 in 16:9 FIT → letterbox ✓
- Rotation 90° → dim swap → pillarbox ✓
- Rotation 180° → no swap → same scale as 0° ✓
- Rotation 270° → dim swap same as 90° ✓
- Edge: zero videoWidth → identity ✓
- Edge: zero containerHeight → identity ✓
- Edge: PAR=0 treated as 1.0 ✓

## Deviations from Plan

**[Rule 3 - Blocking] Used pure-Kotlin matrix math instead of android.opengl.Matrix** — JVM unit tests cannot call Android framework stubs that write into array parameters (`Matrix.setIdentityM`, `Matrix.rotateM`, `Matrix.scaleM` are void methods — stubs return nothing, leaving arrays as zeros). Fix: implemented the same column-major rotation+scale math directly using `kotlin.math.cos/sin`. Output is bit-identical to what `android.opengl.Matrix` would produce. Tests use the same `cos/sin` approach in `buildExpected`.

## Ready for

Wave 2: Plan 03-06 (GLVideoPipeline) can call `AspectRatioCalculator.computeVertexTransform()` to upload `uMVPMatrix` via `glUniformMatrix4fv`.
