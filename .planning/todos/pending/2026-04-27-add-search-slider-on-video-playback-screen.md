---
created: 2026-04-27T18:14:49.827Z
title: Add search slider on video playback screen
area: ui
files:
  - app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt
---

## Problem

Video playback screen currently has limited seek/scrub controls. Users can only use forward/rewind buttons (if present) or rely on the media bar. Adding a visual search/seek slider would provide finer-grained scrubbing control and better match standard video player UX patterns on Android Auto.

## Solution

Implement a horizontal slider in the VideoPlaybackScreen mapStrip or template. Options:
- Use Car App Library's Slider widget if available in Media3/ExoPlayer integration
- Add seek buttons with time display (min/max duration)
- Investigate whether Car App 1.7.0 supports gesture-based seek or slider widgets
- Consider deferring if 1.8.0+ (next CAL version) provides native seek/progress controls
