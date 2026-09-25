package com.pscholer.autoplayer.util

import java.util.Locale

object TimeFormatter {
    /**
     * Format milliseconds as HH:MM:SS string.
     * Handles edge cases: negative values, UNKNOWN_TIME (-9223372036854775807).
     * Hours do not wrap at 24, and digits are always ASCII (Locale.ROOT).
     */
    fun formatMillis(ms: Long): String {
        if (ms <= 0) return "00:00:00"

        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds / 60) % 60
        val seconds = totalSeconds % 60

        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Format the playback timeline as "HH:MM:SS / HH:MM:SS" (position / duration).
     */
    fun formatTimeline(positionMs: Long, durationMs: Long): String =
        "${formatMillis(positionMs)} / ${formatMillis(durationMs)}"
}
