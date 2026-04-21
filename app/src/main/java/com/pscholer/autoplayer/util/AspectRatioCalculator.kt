package com.pscholer.autoplayer.util

object AspectRatioCalculator {
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
