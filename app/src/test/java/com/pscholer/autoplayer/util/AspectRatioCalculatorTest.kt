package com.pscholer.autoplayer.util

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import com.pscholer.autoplayer.util.AspectRatioCalculator.ScalingMode
import kotlin.math.cos
import kotlin.math.sin

class AspectRatioCalculatorTest {

    private val DELTA = 1e-5f

    /**
     * Builds the expected column-major 4×4 matrix for rotation then uniform scale.
     * Replicates what android.opengl.Matrix.rotateM + scaleM would produce, without
     * requiring the Android runtime.
     *
     * Column-major layout: index = col*4 + row
     *   col0: [c*sx,  s*sx, 0, 0]
     *   col1: [-s*sy, c*sy, 0, 0]
     *   col2: [0,     0,    1, 0]
     *   col3: [0,     0,    0, 1]
     * where c = cos(rotation), s = sin(rotation).
     */
    private fun buildExpected(rotationDegrees: Int, sx: Float, sy: Float): FloatArray {
        val angle = Math.toRadians(rotationDegrees.toDouble())
        val c = cos(angle).toFloat()
        val s = sin(angle).toFloat()
        return floatArrayOf(
            c * sx,  s * sx,  0f, 0f,  // col 0
            -s * sy, c * sy,  0f, 0f,  // col 1
            0f,      0f,      1f, 0f,  // col 2
            0f,      0f,      0f, 1f   // col 3
        )
    }

    // ── 1. STRETCH ───────────────────────────────────────────────────────────

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

    // ── 2. FIT — same aspect (identity) ──────────────────────────────────────

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

    // ── 3. FIT — 4:3 in 16:9 → pillarbox ────────────────────────────────────

    @Test
    fun fit_4x3_in_16x9_pillarbox() {
        val m = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 640, videoHeight = 480,
            pixelAspectRatio = 1f, rotationDegrees = 0,
            containerWidth = 1920, containerHeight = 1080,
            mode = ScalingMode.FIT
        )
        // videoAspect = 4/3 ≈ 1.333; containerAspect = 16/9 ≈ 1.778
        // videoAspect < containerAspect → pillarbox: sx = (4/3)/(16/9) = 0.75; sy = 1
        assertArrayEquals(buildExpected(0, 0.75f, 1f), m, DELTA)
    }

    // ── 4. FIT — 21:9 in 16:9 → letterbox ───────────────────────────────────

    @Test
    fun fit_21x9_in_16x9_letterbox() {
        val m = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 2560, videoHeight = 1080,
            pixelAspectRatio = 1f, rotationDegrees = 0,
            containerWidth = 1920, containerHeight = 1080,
            mode = ScalingMode.FIT
        )
        // videoAspect = 2560/1080 ≈ 2.370; containerAspect = 16/9 ≈ 1.778
        // videoAspect > containerAspect → letterbox: sx = 1; sy = (16/9)/(2560/1080)
        val videoAspect = 2560f / 1080f
        val containerAspect = 1920f / 1080f
        val sy = containerAspect / videoAspect
        assertArrayEquals(buildExpected(0, 1f, sy), m, DELTA)
    }

    // ── 5. FIT — 9:16 vertical in 16:9 → wide pillarbox ─────────────────────

    @Test
    fun fit_9x16_in_16x9_wide_pillarbox() {
        val m = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 1080, videoHeight = 1920,
            pixelAspectRatio = 1f, rotationDegrees = 0,
            containerWidth = 1920, containerHeight = 1080,
            mode = ScalingMode.FIT
        )
        // videoAspect = 1080/1920 ≈ 0.5625; containerAspect = 16/9 ≈ 1.778
        // videoAspect < containerAspect → pillarbox: sx = (9/16)/(16/9) = 81/256; sy = 1
        val videoAspect = 1080f / 1920f
        val containerAspect = 1920f / 1080f
        val sx = videoAspect / containerAspect
        assertArrayEquals(buildExpected(0, sx, 1f), m, DELTA)
    }

    // ── 6. FILL — 4:3 in 16:9 → fills, crops sides ───────────────────────────

    @Test
    fun fill_4x3_in_16x9_extends_vertically() {
        val m = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 640, videoHeight = 480,
            pixelAspectRatio = 1f, rotationDegrees = 0,
            containerWidth = 1920, containerHeight = 1080,
            mode = ScalingMode.FILL
        )
        // videoAspect < containerAspect → sx=1; sy = containerAspect/videoAspect = (16/9)/(4/3) = 4/3
        val videoAspect = 640f / 480f
        val containerAspect = 1920f / 1080f
        val sy = containerAspect / videoAspect
        assertArrayEquals(buildExpected(0, 1f, sy), m, DELTA)
    }

    // ── 7. FILL — 21:9 in 16:9 → fills, crops top/bottom ────────────────────

    @Test
    fun fill_21x9_in_16x9_extends_horizontally() {
        val m = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 2560, videoHeight = 1080,
            pixelAspectRatio = 1f, rotationDegrees = 0,
            containerWidth = 1920, containerHeight = 1080,
            mode = ScalingMode.FILL
        )
        // videoAspect > containerAspect → sx = videoAspect/containerAspect; sy=1
        val videoAspect = 2560f / 1080f
        val containerAspect = 1920f / 1080f
        val sx = videoAspect / containerAspect
        assertArrayEquals(buildExpected(0, sx, 1f), m, DELTA)
    }

    // ── 8. PAR ≠ 1.0 anamorphic ──────────────────────────────────────────────

    @Test
    fun fit_anamorphic_par_1_45_in_16x9() {
        val m = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 720, videoHeight = 480,
            pixelAspectRatio = 1.45f, rotationDegrees = 0,
            containerWidth = 1920, containerHeight = 1080,
            mode = ScalingMode.FIT
        )
        // effectiveAspect = (720 * 1.45) / 480 = 2.175; containerAspect = 16/9 ≈ 1.778
        // videoAspect > containerAspect → letterbox: sx=1; sy = containerAspect/effectiveAspect
        val effectiveAspect = (720f * 1.45f) / 480f
        val containerAspect = 1920f / 1080f
        val sy = containerAspect / effectiveAspect
        assertArrayEquals(buildExpected(0, 1f, sy), m, DELTA)
    }

    // ── 9. Rotation 90° — swaps effective dims ────────────────────────────────

    @Test
    fun fit_rotation90_swaps_effective_dims() {
        val m = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 1920, videoHeight = 1080,
            pixelAspectRatio = 1f, rotationDegrees = 90,
            containerWidth = 1920, containerHeight = 1080,
            mode = ScalingMode.FIT
        )
        // rotation=90 → effW=1080, effH=1920; videoAspect=1080/1920≈0.5625
        // videoAspect < containerAspect → pillarbox: sx=videoAspect/containerAspect≈0.3164; sy=1
        val videoAspect = 1080f / 1920f    // swapped
        val containerAspect = 1920f / 1080f
        val sx = videoAspect / containerAspect
        assertArrayEquals(buildExpected(90, sx, 1f), m, DELTA)
    }

    // ── 10. Rotation 180° — preserves aspect ─────────────────────────────────

    @Test
    fun fit_rotation180_preserves_aspect() {
        val m180 = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 640, videoHeight = 480,
            pixelAspectRatio = 1f, rotationDegrees = 180,
            containerWidth = 1920, containerHeight = 1080,
            mode = ScalingMode.FIT
        )
        // No dimension swap for 180°; same sx/sy as rotation=0
        val videoAspect = 640f / 480f
        val containerAspect = 1920f / 1080f
        val sx = videoAspect / containerAspect
        assertArrayEquals(buildExpected(180, sx, 1f), m180, DELTA)
    }

    // ── 11. Rotation 270° — same dim swap as 90° ──────────────────────────────

    @Test
    fun fit_rotation270_swaps_effective_dims_same_as_90() {
        val m = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 1920, videoHeight = 1080,
            pixelAspectRatio = 1f, rotationDegrees = 270,
            containerWidth = 1920, containerHeight = 1080,
            mode = ScalingMode.FIT
        )
        val videoAspect = 1080f / 1920f    // swapped (270 swaps same as 90)
        val containerAspect = 1920f / 1080f
        val sx = videoAspect / containerAspect
        assertArrayEquals(buildExpected(270, sx, 1f), m, DELTA)
    }

    // ── 12. Edge case — zero/negative dimensions → identity ───────────────────

    @Test
    fun edgeCase_zeroVideoWidth_returnsIdentity() {
        val m = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 0, videoHeight = 1080,
            pixelAspectRatio = 1f, rotationDegrees = 0,
            containerWidth = 1920, containerHeight = 1080,
            mode = ScalingMode.FIT
        )
        assertArrayEquals(buildExpected(0, 1f, 1f), m, DELTA)
    }

    @Test
    fun edgeCase_zeroContainerHeight_returnsIdentity() {
        val m = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 1920, videoHeight = 1080,
            pixelAspectRatio = 1f, rotationDegrees = 0,
            containerWidth = 1920, containerHeight = 0,
            mode = ScalingMode.FIT
        )
        assertArrayEquals(buildExpected(0, 1f, 1f), m, DELTA)
    }

    // ── 13. Edge case — PAR = 0 treated as 1.0 ───────────────────────────────

    @Test
    fun edgeCase_parZero_treatedAsOne() {
        val mZeroPar = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 1920, videoHeight = 1080,
            pixelAspectRatio = 0f, rotationDegrees = 0,
            containerWidth = 1920, containerHeight = 1080,
            mode = ScalingMode.FIT
        )
        val mOnePar = AspectRatioCalculator.computeVertexTransform(
            videoWidth = 1920, videoHeight = 1080,
            pixelAspectRatio = 1f, rotationDegrees = 0,
            containerWidth = 1920, containerHeight = 1080,
            mode = ScalingMode.FIT
        )
        assertArrayEquals(mOnePar, mZeroPar, DELTA)
    }
}
