package com.pscholer.autoplayer.util

object TimeFormatter {
    /**
     * Format milliseconds as HH:MM:SS string.
     * Handles edge cases: negative values, UNKNOWN_TIME (-9223372036854775807).
     */
    fun formatMillis(ms: Long): String {
        if (ms <= 0) return "00:00:00"

        val totalSeconds = ms / 1000
        val hours = (totalSeconds / 3600) % 24
        val minutes = (totalSeconds / 60) % 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
