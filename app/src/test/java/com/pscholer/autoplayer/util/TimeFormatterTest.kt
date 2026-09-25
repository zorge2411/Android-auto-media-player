package com.pscholer.autoplayer.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TimeFormatterTest {

    @Test
    fun formatMillis_zero_isAllZeros() {
        assertEquals("00:00:00", TimeFormatter.formatMillis(0))
    }

    @Test
    fun formatMillis_negativeAndTimeUnset_areAllZeros() {
        assertEquals("00:00:00", TimeFormatter.formatMillis(-1))
        // Media3 C.TIME_UNSET
        assertEquals("00:00:00", TimeFormatter.formatMillis(Long.MIN_VALUE + 1))
    }

    @Test
    fun formatMillis_subSecond_truncates() {
        assertEquals("00:00:00", TimeFormatter.formatMillis(999))
    }

    @Test
    fun formatMillis_minutesAndSeconds() {
        assertEquals("00:01:01", TimeFormatter.formatMillis(61_000))
    }

    @Test
    fun formatMillis_hoursMinutesSeconds() {
        assertEquals("01:02:03", TimeFormatter.formatMillis(3_723_000))
    }

    @Test
    fun formatMillis_over24Hours_doesNotWrap() {
        assertEquals("25:00:00", TimeFormatter.formatMillis(90_000_000))
    }

    @Test
    fun formatMillis_usesAsciiDigits_regardlessOfDefaultLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("ar"))
            assertEquals("01:02:03", TimeFormatter.formatMillis(3_723_000))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun formatTimeline_positionAndDuration() {
        assertEquals("00:00:42 / 01:23:45", TimeFormatter.formatTimeline(42_000, 5_025_000))
    }

    @Test
    fun formatTimeline_loadingState_isAllZeros() {
        assertEquals("00:00:00 / 00:00:00", TimeFormatter.formatTimeline(0, 0))
    }
}
