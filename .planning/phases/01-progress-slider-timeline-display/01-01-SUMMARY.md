---
phase: 01-progress-slider-timeline-display
plan: 01
status: complete
completed: 2026-09-25
commits: [b39f81d, 1aa7488, f06d650]
---

# 01-01 Summary — Live timeline

**Delivered**
- `MediaPlayerManager`: 1 Hz `positionTickJob` on `managerScope` (Main). It starts on `onIsPlayingChanged(true)` and is cancelled on `false`, which also publishes the exact paused position. It is also cancelled in `stop()` (before the position reset) and in `release()`.
- `TimeFormatter`: hours no longer wrap at 24, `Locale.ROOT` formatting, and a new `formatTimeline(pos, dur)`.
- `TimeFormatterTest`: 9 JUnit 4 tests.

**Verification**
- RED → GREEN: 9/9 pass. They were run in a standalone JVM Gradle harness that compiles the real source files, because the cloud session cannot download the Android SDK (`dl.google.com` is blocked by network policy).
- Android build: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` passed on the dev PC on 2026-09-25 (28/28 unit tests).

**Deviations:** none.
