---
phase: 3-aspect-ratio
plan: 04
type: execute
wave: 1
depends_on: ["01"]
files_modified:
  - app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt
  - app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt
autonomous: true
requirements: [Feature-4.2, Feature-4.4]
must_haves:
  truths:
    - "AspectRatioCalculator can emit a column-major 4x4 vertex transform matrix for FIT/FILL/STRETCH × rotation × PAR inputs"
    - "Pure-math correctness is proven by JUnit 4 unit tests covering all listed aspect ratios and edge cases"
    - "ScalingMode enum exists in AspectRatioCalculator (canonical home; Wave 3 wiring uses it)"
    - "Existing calculateScaling() function still works (used in VideoSurfaceRenderer for logging)"
  artifacts:
    - path: "app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt"
      provides: "computeVertexTransform() pure function + ScalingMode enum"
      contains: "fun computeVertexTransform"
    - path: "app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt"
      provides: "JUnit 4 unit tests proving transform correctness"
      contains: "@Test"
  key_links:
    - from: "AspectRatioCalculator.computeVertexTransform"
      to: "android.opengl.Matrix"
      via: "Matrix.setIdentityM / rotateM / scaleM"
      pattern: "Matrix\\.(setIdentityM|rotateM|scaleM)"
    - from: "AspectRatioCalculatorTest"
      to: "AspectRatioCalculator.computeVertexTransform"
      via: "Direct invocation + assertArrayEquals on FloatArray output"
      pattern: "computeVertexTransform\\("
---

<objective>
Wave 1 (parallel with Plan 05): Implement the pure-math vertex transform that the GL pipeline (Wave 2) will upload as `uMVPMatrix`, AND prove its correctness with JUnit 4 unit tests covering every aspect ratio in REQUIREMENTS.md Feature 4.4 (16:9, 4:3, 21:9, 9:16) plus PAR ≠ 1.0 anamorphic, all four rotation values (0/90/180/270), all three scaling modes (FIT/FILL/STRETCH), and edge cases.

Purpose: Pattern 3 of RESEARCH.md is the only project-specific piece of math in Option B. The GL plumbing is well-trodden Grafika. This math IS the correctness criterion for Feature 4 acceptance. Unit-testing it on the JVM (no head unit needed) lets us catch matrix bugs (transposed math, wrong rotation order, missing PAR multiply, sign errors) before Wave 3 spends head-unit cycles on integration.

Output:
- `AspectRatioCalculator.kt` extended with `enum class ScalingMode { FIT, FILL, STRETCH }` (canonical home — Wave 3 imports from here) and `computeVertexTransform(...)` returning a 16-float column-major matrix.
- `app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt` with at least 12 `@Test` methods covering all listed cases.
- All tests pass: `./gradlew :app:testDebugUnitTest --tests "*AspectRatioCalculatorTest*"` exits 0.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/3-aspect-ratio/3-RESEARCH.md
@CLAUDE.md
@app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt
@app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Write failing AspectRatioCalculatorTest.kt covering all required cases</name>
  <files>app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt</files>
  <read_first>
    - app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt (existing API — calculateScaling stays)
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (§"Pattern 3: Vertex Transform for FIT / FILL / STRETCH" — math derivation; §"Code Examples → Vertex Transform (Pure Kotlin)" — reference Kotlin signature; §"Validation Architecture → Phase Requirements → Test Map" — required cases)
    - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt (current ScalingMode enum location — being moved)
  </read_first>
  <behavior>
    Tests must cover, at minimum (one @Test method each unless noted):
    1. STRETCH mode → identity scale (sx=1, sy=1) regardless of inputs (one test, multiple assertions)
    2. FIT mode 16:9 video in 16:9 container → near-identity scale (sx≈1, sy≈1; floating-point tolerance 1e-5)
    3. FIT mode 4:3 video in 16:9 container → pillarbox: sx = (4/3) / (16/9) = 0.75; sy = 1
    4. FIT mode 21:9 video (≈2.333) in 16:9 container (≈1.778) → letterbox: sx = 1; sy = (16/9) / (21/9) ≈ 0.762
    5. FIT mode 9:16 vertical video in 16:9 container → wide pillarbox: sx = (9/16) / (16/9) ≈ 0.316; sy = 1
    6. FILL mode 4:3 in 16:9 → sx=1, sy = (16/9)/(4/3) ≈ 1.333 (extends beyond NDC; OES clamp crops)
    7. FILL mode 21:9 in 16:9 → sx = (21/9)/(16/9) ≈ 1.313; sy = 1
    8. PAR ≠ 1.0 anamorphic 720×480 with par=1.45 in 16:9 container, FIT → effective video aspect = 720*1.45/480 ≈ 2.175; produces letterbox roughly matching the prior 21:9 case (sy ≈ 0.817)
    9. Rotation 90° on a 1920×1080 source in 16:9 container, FIT → effective dims swap to 1080×1920, video aspect ≈ 0.5625, container ≈ 1.778, so wide pillarbox: sx ≈ 0.316; sy = 1
    10. Rotation 180° preserves aspect (no swap); same scale as rotation=0
    11. Rotation 270° same as 90° (effective dim swap); same scale as case 9
    12. Edge case: zero/negative inputs → identity matrix (videoW=0, or containerW=0, or videoH=-1, etc.)
    13. PAR=0 (invalid stream metadata) → treated as PAR=1.0 (no NaN, no crash)

    Each test asserts on the resulting `FloatArray(16)` using `assertArrayEquals(expected, actual, delta)` with delta = 1e-5f. Build the expected array using `android.opengl.Matrix` calls in the test itself (the reference implementation IS the implementation under test, so use Matrix to set up expected values rather than hardcoding 16 floats). Where rotation is involved, verify specific elements (e.g. m[0]≈cos(angle)*sx, m[5]≈cos(angle)*sy) instead of full-array equality, because Matrix.rotateM order interacts with scaleM.

    For STRETCH and FIT-identity, assert full-array near-equality with `Matrix.setIdentityM` baseline (with scale applied).

    Tests use JUnit 4 (`@Test` from `org.junit.Test`, `Assert.assertArrayEquals` from `org.junit.Assert`).
  </behavior>
  <action>
    Create directory if needed: `app/src/test/java/com/pscholer/autoplayer/util/`.
    Create file `app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt`.

    File skeleton (fill in all 13 test methods — show 3 below as concrete examples to anchor the style):

    ```kotlin
    package com.pscholer.autoplayer.util

    import android.opengl.Matrix
    import org.junit.Assert.assertArrayEquals
    import org.junit.Assert.assertEquals
    import org.junit.Test
    import com.pscholer.autoplayer.util.AspectRatioCalculator.ScalingMode

    class AspectRatioCalculatorTest {

        private val DELTA = 1e-5f

        // Builds the expected matrix the same way the production code would, so any divergence
        // means production deviated from the spec, not that the test got the spec wrong.
        private fun buildExpected(rotation: Int, sx: Float, sy: Float): FloatArray {
            val m = FloatArray(16)
            Matrix.setIdentityM(m, 0)
            if (rotation != 0) Matrix.rotateM(m, 0, rotation.toFloat(), 0f, 0f, 1f)
            Matrix.scaleM(m, 0, sx, sy, 1f)
            return m
        }

        @Test
        fun stretch_returnsIdentityScale_regardlessOfAspect() {
            val m = AspectRatioCalculator.computeVertexTransform(
                videoWidth = 1920, videoHeight = 1080,
                pixelAspectRatio = 1f, rotationDegrees = 0,
                containerWidth = 800, containerHeight = 600,
                mode = ScalingMode.STRETCH
            )
            assertArrayEquals(buildExpected(0, 1f, 1f), m, DELTA)
        }

        @Test
        fun fit_16x9_in_16x9_container_isIdentity() {
            val m = AspectRatioCalculator.computeVertexTransform(
                videoWidth = 1920, videoHeight = 1080,
                pixelAspectRatio = 1f, rotationDegrees = 0,
                containerWidth = 1920, containerHeight = 1080,
                mode = ScalingMode.FIT
            )
            assertArrayEquals(buildExpected(0, 1f, 1f), m, DELTA)
        }

        @Test
        fun fit_4x3_in_16x9_pillarbox() {
            val m = AspectRatioCalculator.computeVertexTransform(
                videoWidth = 640, videoHeight = 480,
                pixelAspectRatio = 1f, rotationDegrees = 0,
                containerWidth = 1920, containerHeight = 1080,
                mode = ScalingMode.FIT
            )
            // videoAspect = 4/3 ≈ 1.333; containerAspect = 16/9 ≈ 1.778; videoAspect < containerAspect
            // → sx = videoAspect / containerAspect = 0.75; sy = 1
            assertArrayEquals(buildExpected(0, 0.75f, 1f), m, DELTA)
        }

        // ... 10 more @Test methods following the same pattern, covering:
        //  - fit_21x9_in_16x9_letterbox
        //  - fit_9x16_in_16x9_wide_pillarbox
        //  - fill_4x3_in_16x9_extends_vertically
        //  - fill_21x9_in_16x9_extends_horizontally
        //  - fit_anamorphic_par_1_45_in_16x9
        //  - fit_rotation_90_swaps_effective_dims
        //  - fit_rotation_180_preserves_aspect
        //  - fit_rotation_270_swaps_effective_dims
        //  - edgeCase_zeroVideoWidth_returnsIdentity
        //  - edgeCase_zeroContainerWidth_returnsIdentity
        //  - edgeCase_parZero_treatedAsOne
    }
    ```

    Implement ALL 13 test methods. The file should be roughly 150-220 lines including blank lines and comments.

    Run `./gradlew :app:testDebugUnitTest --tests "*AspectRatioCalculatorTest*"`. Tests MUST FAIL at this point because `computeVertexTransform` and `ScalingMode` don't exist on `AspectRatioCalculator` yet — this is the RED step of TDD. Expected failure message: "Unresolved reference: computeVertexTransform" or "Unresolved reference: ScalingMode".
  </action>
  <verify>
    <automated>test -f app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt && grep -c "@Test" app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt | awk '{exit ($1 >= 12 ? 0 : 1)}'</automated>
  </verify>
  <acceptance_criteria>
    - `app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt` exists
    - File contains at least 12 `@Test` annotations (count via `grep -c "@Test"`)
    - File contains test method names matching pattern `fit_16x9`, `fit_4x3`, `fit_21x9`, `fit_9x16`, `fill_`, `anamorphic`, `rotation_90`, `rotation_180`, `rotation_270`, `stretch`, `edgeCase` (verify via grep — at least one match per concept)
    - File imports `org.junit.Test` and `org.junit.Assert.assertArrayEquals`
    - File imports `android.opengl.Matrix` (for buildExpected helper)
    - `./gradlew :app:testDebugUnitTest --tests "*AspectRatioCalculatorTest*"` FAILS with unresolved-reference errors (RED step — confirms tests actually exercise code that doesn't yet exist)
  </acceptance_criteria>
  <done>13 failing tests committed; RED step complete.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Implement computeVertexTransform + ScalingMode in AspectRatioCalculator (GREEN)</name>
  <files>app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt</files>
  <read_first>
    - app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt (current state — the existing `calculateScaling` and `ScaledDimensions` stay)
    - app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt (just-written tests — they define the expected behavior)
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (§"Pattern 3" — exact math; §"Code Examples → Vertex Transform" — reference impl)
  </read_first>
  <behavior>
    Implementation must satisfy every test from Task 1. Specifically:
    - `enum class ScalingMode { FIT, FILL, STRETCH }` is a top-level member of `object AspectRatioCalculator`
    - `computeVertexTransform` returns a 16-float column-major matrix
    - For invalid inputs (any of videoW, videoH, containerW, containerH ≤ 0): return identity
    - For PAR ≤ 0: treat as PAR = 1.0
    - For rotation in {90, 270}: swap effective video width/height before computing aspect
    - For rotation in {0, 180}: do not swap (180 preserves aspect)
    - Rotation is applied via `Matrix.rotateM` BEFORE scale is applied via `Matrix.scaleM`
  </behavior>
  <action>
    Edit `app/src/main/java/com/pscholer/autoplayer/util/AspectRatioCalculator.kt`. KEEP the existing `data class ScaledDimensions` and `fun calculateScaling(...)` exactly as they are (VideoSurfaceRenderer uses them for logging — do not break).

    Add at the top of the file (below the package declaration):
    ```kotlin
    import android.opengl.Matrix
    ```

    Inside `object AspectRatioCalculator`, add:

    ```kotlin
        enum class ScalingMode { FIT, FILL, STRETCH }

        /**
         * Compute the vertex transform for a unit quad in NDC ([-1,1]²) given video and container metrics.
         *
         * Encodes (in order): rotation around Z (clockwise as Android reports), then aspect-correcting
         * scale. Designed to be uploaded via glUniformMatrix4fv to the GL `uMVPMatrix` uniform.
         *
         * For invalid inputs (any dimension ≤ 0) returns identity. For PAR ≤ 0 treats PAR as 1.0.
         *
         * Source: Phase 3 RESEARCH.md Pattern 3 (derived from AspectRatioFrameLayout scaling logic).
         */
        fun computeVertexTransform(
            videoWidth: Int,
            videoHeight: Int,
            pixelAspectRatio: Float,
            rotationDegrees: Int,
            containerWidth: Int,
            containerHeight: Int,
            mode: ScalingMode
        ): FloatArray {
            val m = FloatArray(16)
            Matrix.setIdentityM(m, 0)
            if (videoWidth <= 0 || videoHeight <= 0 || containerWidth <= 0 || containerHeight <= 0) {
                return m
            }
            val (effW, effH) = if (rotationDegrees == 90 || rotationDegrees == 270) {
                videoHeight to videoWidth
            } else {
                videoWidth to videoHeight
            }
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

            if (rotationDegrees != 0) {
                Matrix.rotateM(m, 0, rotationDegrees.toFloat(), 0f, 0f, 1f)
            }
            Matrix.scaleM(m, 0, sx, sy, 1f)
            return m
        }
    ```

    Run `./gradlew :app:testDebugUnitTest --tests "*AspectRatioCalculatorTest*"`. ALL tests should now pass (GREEN step). If any fail, debug the math against the RESEARCH.md Pattern 3 worked examples — common bugs:
    - Forgetting to swap effW/effH on rotation 90/270 (cases 9, 11 fail)
    - Multiplying PAR into wrong term (case 8 fails)
    - Applying rotation AFTER scale (rotation cases produce subtly wrong matrix elements)
    - Returning identity for STRETCH only by accident (case 1 passes for the wrong reason)

    Do NOT modify or remove the existing `calculateScaling()` or `ScaledDimensions` — they are still used by `VideoSurfaceRenderer.onVideoSizeChanged` for logging.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "*AspectRatioCalculatorTest*" --quiet</automated>
  </verify>
  <acceptance_criteria>
    - `AspectRatioCalculator.kt` contains `enum class ScalingMode { FIT, FILL, STRETCH }`
    - `AspectRatioCalculator.kt` contains `fun computeVertexTransform(`
    - `AspectRatioCalculator.kt` contains `import android.opengl.Matrix`
    - `AspectRatioCalculator.kt` STILL contains `fun calculateScaling(` (old API preserved)
    - `AspectRatioCalculator.kt` STILL contains `data class ScaledDimensions(` (old API preserved)
    - `./gradlew :app:testDebugUnitTest --tests "*AspectRatioCalculatorTest*"` exits 0 with "BUILD SUCCESSFUL"
    - Test report shows ≥ 12 tests passed, 0 failed
    - `./gradlew :app:assembleDebug` exits 0 (no regressions in main source)
  </acceptance_criteria>
  <done>computeVertexTransform implemented; all unit tests pass.</done>
</task>

</tasks>

<verification>
- `./gradlew :app:testDebugUnitTest --tests "*AspectRatioCalculatorTest*"` exits 0.
- All 13+ unit tests pass.
- Existing `calculateScaling()` API unchanged (no regressions in `VideoSurfaceRenderer.onVideoSizeChanged` logging).
- `enum class ScalingMode` lives in `AspectRatioCalculator` and is importable from anywhere in the project.
</verification>

<success_criteria>
- Pure-math correctness for FIT/FILL/STRETCH × rotation × PAR is proven on the JVM, no head unit required.
- Wave 3 (Plan 06) can rely on `computeVertexTransform` returning correct output for any input combination.
- The math IS the spec — any future regression is caught immediately by these tests.
</success_criteria>

<output>
Create `.planning/phases/3-aspect-ratio/3-04-SUMMARY.md` summarizing:
- Test count (should be ≥ 13)
- Key edge cases proven (PAR=0, rotation 90/270, zero dims)
- Confirmation `calculateScaling` API was preserved
- That `ScalingMode` is now canonically in `AspectRatioCalculator` (Wave 3 will update MediaPlayerManager to import from here)
</output>
