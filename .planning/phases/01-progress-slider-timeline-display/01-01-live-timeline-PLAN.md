---
phase: 01-progress-slider-timeline-display
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
  - app/src/main/java/com/pscholer/autoplayer/util/TimeFormatter.kt
  - app/src/test/java/com/pscholer/autoplayer/util/TimeFormatterTest.kt
autonomous: true
requirements: [FEAT-1-AC2, FEAT-1-AC4, FEAT-1-AC5]
must_haves:
  truths:
    - "MediaPlayerManager.positionMs emits a fresh value about once per second while the player is playing (today it only updates on state changes and seeks, so the timeline stays frozen during normal playback)"
    - "The position ticker is not running while paused, stopped, or idle, and never runs more than once at a time"
    - "TimeFormatter.formatMillis does not wrap hours at 24 and always outputs ASCII digits whatever the device locale is"
    - "TimeFormatter.formatTimeline(pos, dur) returns the locked 'HH:MM:SS / HH:MM:SS' string"
    - "JVM unit tests prove the formatter behaviour (this is the phase's one automated coverage item, per config.testing.minCoveragePerPhase)"
  artifacts:
    - path: "app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt"
      provides: "1 Hz position ticker bound to isPlaying"
      contains: "positionTickJob"
    - path: "app/src/main/java/com/pscholer/autoplayer/util/TimeFormatter.kt"
      provides: "formatMillis (fixed) + formatTimeline"
      contains: "fun formatTimeline"
    - path: "app/src/test/java/com/pscholer/autoplayer/util/TimeFormatterTest.kt"
      provides: "JUnit 4 tests for formatting edge cases"
      contains: "@Test"
  key_links:
    - from: "Player.Listener.onIsPlayingChanged"
      to: "startPositionTicker / stopPositionTicker"
      via: "playing == true starts the ticker; false cancels it"
      pattern: "PositionTicker"
---

<objective>
Wave 1: make the timeline actually move.

**Gap found while planning (not in 01-RESEARCH.md):** the research lists `positionMs: StateFlow<Long>` as "Complete", but `MediaPlayerManager` only writes `_positionMs` inside `updatePositionAndDuration()`. That function runs from `onPlaybackStateChanged` and `onPositionDiscontinuity` only. During uninterrupted playback nothing emits, so the screen's `combine(...)` loop never fires and the timeline stays frozen. This breaks FEAT-1-AC5 ("updates in real-time").

Also fixes two latent `TimeFormatter` bugs:
- `(totalSeconds / 3600) % 24` makes a 25 h stream show `01:00:00`.
- `String.format` with the default locale renders non-ASCII digits on some locales (e.g. `ar`).

Output:
- 1 Hz position ticker in `MediaPlayerManager`.
- `TimeFormatter.formatTimeline(positionMs, durationMs)`, which Plan 02 uses in `buildTimelineText()`.
- `TimeFormatterTest.kt` (JVM, no Android runtime needed).
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/01-progress-slider-timeline-display/01-RESEARCH.md
@.planning/phases/01-progress-slider-timeline-display/01-RESEARCH-ADDENDUM.md
@.planning/phases/01-progress-slider-timeline-display/01-UI-SPEC.md
@CLAUDE.md
@app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
@app/src/main/java/com/pscholer/autoplayer/util/TimeFormatter.kt
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: TimeFormatterTest (RED)</name>
  <files>app/src/test/java/com/pscholer/autoplayer/util/TimeFormatterTest.kt</files>
  <read_first>
    - app/src/main/java/com/pscholer/autoplayer/util/TimeFormatter.kt
    - app/src/test/java/com/pscholer/autoplayer/util/AspectRatioCalculatorTest.kt (test style: JUnit 4, org.junit.Assert)
  </read_first>
  <behavior>
    One @Test each:
    1. `formatMillis(0)` → "00:00:00"
    2. Negative input and `C.TIME_UNSET` (Long.MIN_VALUE + 1) → "00:00:00"
    3. 999 ms → "00:00:00" (truncates, does not round)
    4. 61_000 → "00:01:01"
    5. 3_723_000 → "01:02:03"
    6. 25 h (90_000_000) → "25:00:00" (no wrap at 24)
    7. Locale independence: set `Locale.setDefault(Locale("ar"))` in try/finally (restore the original), assert "01:02:03" with ASCII digits
    8. `formatTimeline(42_000, 5_025_000)` → "00:00:42 / 01:23:45"
    9. `formatTimeline(0, 0)` → "00:00:00 / 00:00:00" (the loading state from UI-SPEC)
  </behavior>
  <action>
    Create the test file. Run `./gradlew :app:testDebugUnitTest --tests "*TimeFormatterTest*"`.
    It must fail: `formatTimeline` does not exist yet, and tests 6 and 7 fail on the current implementation.
  </action>
  <verify>
    <automated>grep -c "@Test" app/src/test/java/com/pscholer/autoplayer/util/TimeFormatterTest.kt | awk '{exit ($1 >= 9 ? 0 : 1)}'</automated>
  </verify>
  <done>Failing tests committed (`test(01-01): add failing TimeFormatter tests`).</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Fix TimeFormatter + add formatTimeline (GREEN)</name>
  <files>app/src/main/java/com/pscholer/autoplayer/util/TimeFormatter.kt</files>
  <action>
    - Remove the `% 24` on hours.
    - Use `String.format(Locale.ROOT, "%02d:%02d:%02d", ...)`.
    - Add `fun formatTimeline(positionMs: Long, durationMs: Long): String = "${formatMillis(positionMs)} / ${formatMillis(durationMs)}"`.
    - Keep the existing KDoc style.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "*TimeFormatterTest*"</automated>
  </verify>
  <done>All TimeFormatter tests pass. Commit `fix(01-01): locale-safe, non-wrapping TimeFormatter + formatTimeline`.</done>
</task>

<task type="auto">
  <name>Task 3: 1 Hz position ticker in MediaPlayerManager</name>
  <files>app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt</files>
  <read_first>
    - MediaPlayerManager.kt: `managerScope`, `savePositionJob` / `startPeriodicSave` (the pattern to mirror), the `Player.Listener` block, `stop()`, `release()`
  </read_first>
  <action>
    - Add `private var positionTickJob: Job? = null` and a `POSITION_TICK_INTERVAL_MS = 1_000L` constant beside `PLAYBACK_SAVE_INTERVAL_MS`.
    - `startPositionTicker()`: cancel any existing job, then `managerScope.launch { while (isActive) { _positionMs.value = player.currentPosition.coerceAtLeast(0L); _durationMs.value = player.duration.coerceAtLeast(0L); delay(POSITION_TICK_INTERVAL_MS) } }`.
      Confirm `managerScope` uses `Dispatchers.Main` (ExoPlayer must be read on its application looper). If it doesn't, launch this job on `Dispatchers.Main.immediate`.
    - `stopPositionTicker()`: cancel the job and set it to null.
    - In `onIsPlayingChanged(playing)`: after `_isPlaying.value = playing`, call `startPositionTicker()` or `stopPositionTicker()`. When stopping, also do one final `updatePositionAndDuration()` so the paused position is exact.
    - `stop()` and `release()`: call `stopPositionTicker()` BEFORE the existing `_positionMs.value = 0L` reset. Otherwise a tick can overwrite the reset.
    - Do NOT touch `invalidate()` throttling here. `VideoPlaybackScreen` already de-duplicates on the formatted second.
  </action>
  <verify>
    <automated>./gradlew :app:assembleDebug && grep -q "positionTickJob" app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt</automated>
  </verify>
  <acceptance_criteria>
    - Builds cleanly.
    - Exactly one ticker job at a time (every start cancels any existing job first).
    - The ticker is cancelled in `stop()`, in `release()`, and on `onIsPlayingChanged(false)`.
  </acceptance_criteria>
  <done>Commit `feat(01-01): emit playback position at 1 Hz while playing`.</done>
</task>

</tasks>

<verification>
- `./gradlew :app:testDebugUnitTest` is green.
- `./gradlew :app:assembleDebug` is green.
- Device smoke test (optional here, required in Plan 03): the timeline counts up once per second during playback.
</verification>
