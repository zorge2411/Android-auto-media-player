package com.pscholer.autoplayer.util

import kotlin.math.cos
import kotlin.math.sin

object AspectRatioCalculator {

    enum class ScalingMode { FIT, FILL, STRETCH }

    /**
     * Compute the vertex transform for a unit quad in NDC ([-1,1]²) given video and container metrics.
     *
     * Returns a 16-float column-major 4×4 matrix encoding rotation around Z then aspect-correcting
     * scale. Designed to be uploaded via glUniformMatrix4fv to the GL `uMVPMatrix` uniform.
     *
     * For invalid inputs (any dimension ≤ 0) returns identity. For PAR ≤ 0 treats PAR as 1.0.
     *
     * Source: Phase 3 RESEARCH.md Pattern 3.
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
        val m = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
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

        // Encode rotation then scale into column-major 4×4 matrix (matches android.opengl.Matrix output)
        val angle = Math.toRadians(rotationDegrees.toDouble())
        val c = cos(angle).toFloat()
        val s = sin(angle).toFloat()
        m[0]  = c * sx;  m[1]  = s * sx;  m[2]  = 0f; m[3]  = 0f
        m[4]  = -s * sy; m[5]  = c * sy;  m[6]  = 0f; m[7]  = 0f
        m[8]  = 0f;      m[9]  = 0f;      m[10] = 1f; m[11] = 0f
        m[12] = 0f;      m[13] = 0f;      m[14] = 0f; m[15] = 1f
        return m
    }

    data class ScaledDimensions(
        val targetWidth: Float,
        val targetHeight: Float,
        val offsetX: Float,
        val offsetY: Float
    )

    /**
     * Calculate how to scale video to fit container while preserving aspect ratio.
     *
     * Returns target dimensions and center offsets for letterboxing/pillarboxing.
     * If video is wider than container, it fills width with padding on top/bottom.
     * If video is taller than container, it fills height with padding on left/right.
     */
    fun calculateScaling(
        videoWidth: Int,
        videoHeight: Int,
        pixelAspectRatio: Float = 1.0f,
        rotationDegrees: Int = 0,
        containerWidth: Int,
        containerHeight: Int
    ): ScaledDimensions {
        if (videoWidth <= 0 || videoHeight <= 0 || containerWidth <= 0 || containerHeight <= 0) {
            return ScaledDimensions(containerWidth.toFloat(), containerHeight.toFloat(), 0f, 0f)
        }

        // Account for rotation: if 90 or 270 degrees, swap width and height
        val effectiveWidth: Float
        val effectiveHeight: Float
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            effectiveWidth = videoHeight.toFloat()
            effectiveHeight = videoWidth.toFloat()
        } else {
            effectiveWidth = videoWidth.toFloat()
            effectiveHeight = videoHeight.toFloat()
        }

        // Apply pixel aspect ratio (PAR) to the width to get the display aspect ratio
        // If PAR is 0 or invalid, default to 1.0
        val safePAR = if (pixelAspectRatio <= 0f) 1.0f else pixelAspectRatio
        val videoAspectRatio = (effectiveWidth * safePAR) / effectiveHeight
        val containerAspectRatio = containerWidth.toFloat() / containerHeight

        val (targetWidth, targetHeight) = if (videoAspectRatio > containerAspectRatio) {
            // Video is wider than container — fit to width (letterbox: bars on top/bottom)
            val w = containerWidth.toFloat()
            val h = w / videoAspectRatio
            Pair(w, h)
        } else {
            // Video is taller or equal aspect — fit to height (pillarbox: bars on sides)
            val h = containerHeight.toFloat()
            val w = h * videoAspectRatio
            Pair(w, h)
        }

        val offsetX = (containerWidth - targetWidth) / 2
        val offsetY = (containerHeight - targetHeight) / 2

        return ScaledDimensions(targetWidth, targetHeight, offsetX, offsetY)
    }
}
