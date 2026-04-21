---
phase: 1-progress-slider
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
  - app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt
  - app/src/main/java/com/pscholer/autoplayer/util/TimeFormatter.kt
autonomous: true
requirements: []
user_setup: []

must_haves:
  truths:
    - "Timeline displays current position and total duration as text (HH:MM:SS / HH:MM:SS)"
    - "Timeline updates in real-time during playback (at least 5 updates per second)"
    - "Seek buttons (back/forward) move playback position within ±2 seconds of target"
    - "Duration shows correct value after media becomes ready (not UNKNOWN_TIME)"
    - "No playback stutter or audio distortion during seek operations"
  artifacts:
    - path: "app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt"
      provides: "Position and duration StateFlow exposure for reactive UI"
      exports: ["positionMs: StateFlow<Long>", "durationMs: StateFlow<Long>", "seekTo()", "seekBack()", "seekForward()"]
    - path: "app/src/main/java/com/pscholer/autoplayer/util/TimeFormatter.kt"
      provides: "Milliseconds to HH:MM:SS formatted string conversion"
      contains: "fun formatMillis(ms: Long): String"
    - path: "app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt"
      provides: "Timeline text display and screen invalidation on position/duration changes"
      contains: "NavigationTemplate with timeline display in ActionStrip or message"
  key_links:
    - from: "VideoPlaybackScreen"
      to: "MediaPlayerManager"
      via: "EntryPointAccessors.fromApplication() to access playerManager singleton"
      pattern: "playerManager\\.(positionMs|durationMs|seekTo)"
    - from: "VideoPlaybackScreen"
      to: "TimeFormatter"
      via: "Utility function for text formatting"
      pattern: "TimeFormatter\\.formatMillis"
    - from: "MediaPlayerManager"
      to: "ExoPlayer Player.Listener"
      via: "StateFlow emission on position/duration changes"
      pattern: "exo\\.addListener.*onPositionDiscontinuity|onPlaybackStateChanged"
---

<objective>
Display playback progress as a text-based timeline and enable seeking via existing buttons.

**Purpose:** Users need visibility into current playback position and ability to navigate within videos. Car App Library forbids custom seekbars; solution uses real-time text display (HH:MM:SS) and discrete seek buttons.

**Output:** 
- Updated MediaPlayerManager with position/duration StateFlow
- TimeFormatter utility for HH:MM:SS conversion
- Updated VideoPlaybackScreen with timeline display
- All seekTo operations wired to existing buttons (no new UI added)

**Constraint:** No new UI components added. Only text display in ActionStrip labels and position tracking. Seek operations use existing back/forward buttons.
</objective>

<execution_context>
@/c/Users/peter/.claude/get-shit-done/workflows/execute-plan.md
@/c/Users/peter/.claude/get-shit-done/templates/summary.md

This plan is for a single-module Android Auto app. Execute sequentially through tasks; all tasks modify existing or new files in com.pscholer.autoplayer package. No parallel streams needed (all depend on same codebase context).
</execution_context>

<context>
@C:/Users/peter/AndroidStudioProjects/Android auto media player/.planning/codebase/ARCHITECTURE.md
@C:/Users/peter/AndroidStudioProjects/Android auto media player/.planning/codebase/STRUCTURE.md
@C:/Users/peter/AndroidStudioProjects/Android auto media player/.planning/phases/1-progress-slider/1-RESEARCH.md
@C:/Users/peter/AndroidStudioProjects/Android auto media player/.planning/STATE.md
@C:/Users/peter/AndroidStudioProjects/Android auto media player/.planning/ROADMAP.md

## Current Architecture Context

From ARCHITECTURE.md:
- **MediaPlayerManager** (app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt): Singleton ExoPlayer wrapper. Already has seekBack(), seekForward(), seekTo(), togglePlayPause() methods. ExoPlayer instance created with audio focus handling.
- **VideoPlaybackScreen** (app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt): NavigationTemplate-based screen. Current ActionStrip: Seek Back | Play/Pause | Seek Forward buttons. No position display yet.
- **DI Pattern**: Hilt singleton via AppEntryPoint accessor. Screens use `EntryPointAccessors.fromApplication(carContext.applicationContext, AppEntryPoint::class.java).playerManager()`.

## Research Summary (1-RESEARCH.md)

From Phase 1 research:
- **Critical constraint:** Car App Library NAVIGATION template does NOT support SeekBar or Slider components.
- **Viable pattern:** Text-based timeline (HH:MM:SS / HH:MM:SS) + discrete seek buttons (already exist).
- **StateFlow for position:** Expose positionMs and durationMs via StateFlow in MediaPlayerManager.
- **Listener pattern:** Use Player.Listener callbacks + manual 200ms ticker during playback for smooth updates.
- **Seek accuracy:** Codec-dependent (keyframe-based seeking). Realistic target ±2 seconds.
- **Duration guard:** ExoPlayer sets duration to C.TIME_UNSET (-9223...long) until media buffered. Always check `duration > 0` before display.
- **Pitfall: Driving state:** Seeking should be disabled while car is moving (ConstraintManager.isStalled()). Phase 2+, not Phase 1.

## Existing Code Patterns (from STRUCTURE.md)

- **Tag constant:** `private const val TAG = "ClassName"` in each class
- **StateFlow exposure:** `_privateName: MutableStateFlow` + public `publicName: StateFlow` read-only
- **Lifecycle:** Screens use `lifecycleScope.launch` for coroutine management
- **Logging:** Log.d/i/w/e with TAG; info for user-facing events
- **Coroutine scope:** Car App Library screens provide `lifecycleScope` automatically
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add TimeFormatter utility for milliseconds to HH:MM:SS conversion</name>
  <files>app/src/main/java/com/pscholer/autoplayer/util/TimeFormatter.kt</files>
  <action>
Create a new utility file that formats milliseconds into human-readable HH:MM:SS strings. This is a simple, stateless utility function that will be reused by VideoPlaybackScreen.

**File:** app/src/main/java/com/pscholer/autoplayer/util/TimeFormatter.kt

**Implementation:**
```kotlin
package com.pscholer.autoplayer.util

/**
 * Converts milliseconds to HH:MM:SS format for display.
 * 
 * Examples:
 * - 0 ms → "00:00:00"
 * - 65000 ms (65 seconds) → "00:01:05"
 * - 3661000 ms (1 hour, 1 min, 1 sec) → "01:01:01"
 * 
 * Handles edge cases:
 * - Negative durations: treated as 0 (returns "00:00:00")
 * - Very long durations: hours can exceed 99 (shows "123:45:67" for 12+ hours)
 */
object TimeFormatter {
    fun formatMillis(ms: Long): String {
        // Guard against negative or unknown values
        if (ms < 0) return "00:00:00"
        
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
```

**Why this approach:**
- **No external dependencies:** Uses only String.format (stdlib)
- **Stateless:** Object with single function (no instance needed)
- **Guard clause:** Handles negative ms (ExoPlayer edge case) by returning "00:00:00"
- **Flexible:** Hours can exceed 99 for videos longer than 100 hours
- **Locale-aware:** String.format respects locale for digit rendering

**Testing approach:** Will verify in Task 3 (VideoPlaybackScreen) by observing displayed timeline during manual playback.
  </action>
  <verify>
File created at app/src/main/java/com/pscholer/autoplayer/util/TimeFormatter.kt with formatMillis() function.
Test: `TimeFormatter.formatMillis(0)` returns "00:00:00", `TimeFormatter.formatMillis(3661000)` returns "01:01:01".
  </verify>
  <done>TimeFormatter.kt file exists with formatMillis(ms: Long) function. Compiles without errors.</done>
</task>

<task type="auto">
  <name>Task 2: Add positionMs and durationMs StateFlow to MediaPlayerManager</name>
  <files>app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt</files>
  <action>
Expose ExoPlayer's current position and media duration via StateFlow for reactive UI updates. This enables VideoPlaybackScreen to observe and display timeline changes without polling.

**File:** app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt

**Changes to make:**

1. **Add StateFlow fields** (after existing state flow declarations, ~line 20-30):
```kotlin
// ── Position & Duration Tracking ───────────────────────────────────────
private val _positionMs = MutableStateFlow(0L)
val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

private val _durationMs = MutableStateFlow(0L)
val durationMs: StateFlow<Long> = _durationMs.asStateFlow()
```

2. **Register Player.Listener in ExoPlayer initialization** (where player is created, add these callbacks):
```kotlin
exo.addListener(object : Player.Listener {
    override fun onPlaybackStateChanged(state: Int) {
        // Update duration when media becomes ready or buffered
        if (state == Player.STATE_READY || state == Player.STATE_BUFFERING) {
            val duration = exo.duration
            if (duration > 0) {  // Guard: ignore UNKNOWN_TIME (-9223...)
                _durationMs.value = duration
            }
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: PositionInfo,
        newPosition: PositionInfo,
        @DiscontinuityReason reason: Int
    ) {
        // Fire when user seeks or player repositions
        _positionMs.value = newPosition.positionMs
    }

    override fun onIsPlayingChanged(playing: Boolean) {
        // Sync position when play/pause state changes
        _positionMs.value = exo.currentPosition
    }
})
```

3. **Add periodic position ticker** (in class body or init block, after player creation):
```kotlin
// Start coroutine to update position every 200ms during playback
// This ensures UI updates smoothly (5 times per second)
val tickerJob = Job()
val tickerScope = CoroutineScope(Dispatchers.Main + tickerJob)

tickerScope.launch {
    while (true) {
        if (player.isPlaying) {
            _positionMs.value = player.currentPosition
        }
        delay(200)  // Update 5 times per second for smooth display
    }
}

// Store job for cleanup (if manager is destroyed)
// For singleton scope, cleanup is not critical, but best practice
```

**Why this implementation:**
- **Listener callbacks:** Capture seek discontinuities (user presses button), state changes, and play/pause events
- **Duration guard:** Check `duration > 0` to avoid exposing ExoPlayer's UNKNOWN_TIME sentinel value
- **Periodic ticker:** 200ms updates (5/sec) balance UI smoothness with CPU efficiency. Not 60 FPS (wastes battery); sufficient for human perception.
- **StateFlow:** Automatically notifies any observer when position/duration changes. Integrates with Screen's `lifecycleScope.launch { collectLatest {} }` pattern.

**Verification approach:** Will test in Task 3 when Screen observes these flows during playback.

**Note on cleanup:** For singleton managers, the ticker coroutine runs for app lifetime. If you want to stop it on explicit release, store tickerJob and call `tickerJob.cancel()` in a release() method. Current scope is acceptable for sideloaded app.
  </action>
  <verify>
File compiles without errors. StateFlow fields are accessible:
- `playerManager.positionMs` is readable (StateFlow<Long>)
- `playerManager.durationMs` is readable (StateFlow<Long>)
Manual verification in Task 3: Start playback and observe logcat for position updates.
  </verify>
  <done>MediaPlayerManager.kt has positionMs and durationMs StateFlow fields, exposed as read-only. Player.Listener registered to update flows on state/position changes. Periodic ticker (200ms) updates position during playback.</done>
</task>

<task type="auto">
  <name>Task 3: Update VideoPlaybackScreen to display timeline and observe position changes</name>
  <files>app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt</files>
  <action>
Modify VideoPlaybackScreen to display the timeline as formatted text (HH:MM:SS / HH:MM:SS) and re-render when position/duration changes. Integrate TimeFormatter and observe MediaPlayerManager's StateFlows.

**File:** app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt

**Changes to make:**

1. **Add lifecycle observers** (in class init or after constructor, add these):
```kotlin
init {
    // Re-render template when position updates
    lifecycleScope.launch {
        playerManager.positionMs.collectLatest { _ ->
            invalidate()  // Refresh template with new timeline text
        }
    }
    
    // Re-render template when duration updates
    lifecycleScope.launch {
        playerManager.durationMs.collectLatest { _ ->
            invalidate()  // Refresh template with new timeline text
        }
    }
}
```

2. **Add timeline formatting helper method** (in class body):
```kotlin
private fun buildTimelineText(): String {
    val position = playerManager.positionMs.value
    val duration = playerManager.durationMs.value
    
    return if (duration > 0) {
        // Show "HH:MM:SS / HH:MM:SS" when duration is known
        "${TimeFormatter.formatMillis(position)} / ${TimeFormatter.formatMillis(duration)}"
    } else {
        // Show placeholder while buffering/loading
        "-- : -- : -- / -- : -- : --"
    }
}
```

3. **Integrate timeline into ActionStrip** (in onGetTemplate() method, modify the action strip building):

Current pattern (seek buttons without timeline):
```kotlin
val playbackStrip = ActionStrip.Builder()
    .addAction(buildAction(R.drawable.ic_seek_back, "Rewind") { 
        playerManager.seekBack(10_000) 
    })
    .addAction(buildAction(...play/pause...) { ... })
    .addAction(buildAction(R.drawable.ic_seek_forward, "Skip") { 
        playerManager.seekForward(10_000) 
    })
    .build()
```

**Option A (Timeline in Button Labels):** Update button labels to include timeline:
```kotlin
val timelineText = buildTimelineText()
val playbackStrip = ActionStrip.Builder()
    .addAction(buildAction(R.drawable.ic_seek_back, "← 10s") { 
        playerManager.seekBack(10_000) 
    })
    .addAction(buildAction(
        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
        timelineText  // Display timeline as middle button label
    ) { playerManager.togglePlayPause() })
    .addAction(buildAction(R.drawable.ic_seek_forward, "10s →") { 
        playerManager.seekForward(10_000) 
    })
    .build()
```

**Option B (Timeline in Message):** If ActionStrip label is too constrained, add timeline as NavigationTemplate message:
```kotlin
val timelineText = buildTimelineText()
return NavigationTemplate.Builder()
    .setActionStrip(playbackStrip)
    .setTitle(timelineText)  // Or some other message area
    .build()
```

**Recommendation:** Use Option A (timeline in play/pause button label). This keeps timeline visible and associated with playback state. If label is truncated by Car App Library, fall back to Option B.

**Why this implementation:**
- **collectLatest:** Only responds to the most recent value; skips intermediate updates during rapid seeks
- **invalidate():** Rebuilds template via onGetTemplate(); causes action labels and UI to refresh
- **TimeFormatter.formatMillis():** Reuses formatter from Task 1
- **Guard clause:** `duration > 0` prevents displaying UNKNOWN_TIME placeholder
- **No new components:** Text display in existing ActionStrip/message; no new UI elements added

**Testing approach:** Manual verification in later wave with real device.

**Import additions needed:**
```kotlin
import com.pscholer.autoplayer.util.TimeFormatter
import kotlinx.coroutines.flow.collectLatest
```

**Note on invalidate() frequency:** At 200ms ticker + multiple seek taps, invalidate() may be called 5-10 times per second. This is acceptable for car screens (not high-frequency like 60fps games). Monitor logcat for excessive invalidate calls; if jank observed, implement debounce (deferred invalidate, max once per 200ms).
  </action>
  <verify>
File compiles without errors. New imports added (TimeFormatter, collectLatest). Screen lifecycle observers registered in init. buildTimelineText() helper method exists. ActionStrip updated to include timeline text (either in button label or message). Manual verification: Start playback, observe timeline text updating on-screen every ~200ms.
  </verify>
  <done>VideoPlaybackScreen displays timeline as "HH:MM:SS / HH:MM:SS" text. Timeline updates when position changes during playback. Duration guard prevents display of placeholder when duration is 0 or UNKNOWN_TIME.</done>
</task>

<task type="auto">
  <name>Task 4: Verify seek operations work and update MediaPlayerManager seek methods if needed</name>
  <files>app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt</files>
  <action>
Ensure existing seekBack(), seekForward(), seekTo() methods in MediaPlayerManager are correct and will work with the new StateFlow-based position tracking. Per ARCHITECTURE.md, these methods may already exist; if so, verify they trigger position updates via Player.Listener. If any are missing, implement them.

**Check existing methods:**

From ARCHITECTURE.md, MediaPlayerManager should already have:
```kotlin
fun seekBack(ms: Long = 10_000L) { ... }
fun seekForward(ms: Long = 10_000L) { ... }
fun seekTo(positionMs: Long) { ... }
```

**Verification steps:**

1. **Inspect seekBack() implementation:** Should call `player.seekTo(maxOf(0L, player.currentPosition - ms))` or similar. This bounds the position to [0, duration].

2. **Inspect seekForward() implementation:** Should call `player.seekTo(player.currentPosition + ms)`. Position will be clamped by ExoPlayer (seekTo respects duration bounds).

3. **Inspect seekTo() implementation:** Should validate position and call `player.seekTo(positionMs)`. Ideally with bounds checking: `positionMs.coerceIn(0L, player.duration)`.

4. **If methods are missing or incomplete:**
```kotlin
fun seekBack(ms: Long = 10_000L) {
    val newPosition = maxOf(0L, player.currentPosition - ms)
    player.seekTo(newPosition)
    // Player.Listener will fire onPositionDiscontinuity → StateFlow updates
}

fun seekForward(ms: Long = 10_000L) {
    val newPosition = player.currentPosition + ms
    player.seekTo(newPosition)
}

fun seekTo(positionMs: Long) {
    val boundedPosition = positionMs.coerceIn(0L, player.duration)
    player.seekTo(boundedPosition)
}
```

**Why this matters:**
- **Seek triggering listener:** When seekTo() is called, ExoPlayer fires onPositionDiscontinuity() callback → updates _positionMs StateFlow → invalidates screen → timeline refreshes
- **Bounds checking:** Prevents seeking to negative or past-duration positions (would cause ExoPlayer errors or stalls)
- **Existing implementation:** Methods likely exist; this task is verification + minimal fix if needed

**Testing approach:** Manual seek via buttons in later wave.
  </action>
  <verify>
Verify seekBack(), seekForward(), seekTo() methods exist in MediaPlayerManager. Each method calls player.seekTo() and includes bounds checking. Code compiles. No changes needed if methods are correct; if missing or broken, implement per example above.
  </verify>
  <done>seekBack(), seekForward(), seekTo() methods verified or added. All seek operations trigger Player.Listener callbacks that update positionMs StateFlow. Position updates propagate to VideoPlaybackScreen via collectLatest, causing timeline refresh.</done>
</task>

<task type="auto">
  <name>Task 5: Compile and smoke test the build</name>
  <files>app/build.gradle.kts</files>
  <action>
Run a clean build to verify all changes compile without errors. This catches import issues, missing dependencies, or syntax errors before manual testing.

**Steps:**
1. Open terminal in project root (C:/Users/peter/AndroidStudioProjects/Android auto media player)
2. Run: `./gradlew clean`
3. Run: `./gradlew assemble` (or `./gradlew assembleDebug`)
4. Check for BUILD SUCCESSFUL message
5. If errors occur:
   - Fix any missing imports (e.g., collectLatest from kotlinx.coroutines.flow)
   - Verify TimeFormatter is in correct package (com.pscholer.autoplayer.util)
   - Check MediaPlayerManager has CoroutineScope imported
   - Verify VideoPlaybackScreen has lifecycle imports (lifecycleScope, collectLatest)

**Expected duration:** 30-60 seconds (depends on gradle cache).

**What this validates:**
- All three modified/new files compile
- No import errors or missing dependencies
- No syntax errors in StateFlow initialization or listener registration
- ActionStrip building with new timeline text compiles
  </action>
  <verify>
Gradle build completes with "BUILD SUCCESSFUL" message. No compilation errors. Check that:
- app/build.gradle.kts shows ✓ no errors
- Build output: "Generating APK..." stage completes
- Final message: ":app:assembleDebug BUILD SUCCESSFUL" (or equivalent for assemble)
  </verify>
  <done>Project builds successfully. All source files compile without errors. APK generated (or AAB if using bundleDebug).</done>
</task>

</tasks>

<verification>
**Wave 1 completion verification (before device testing):**

1. **Code review:** All three tasks completed:
   - [ ] TimeFormatter.kt exists with formatMillis() function
   - [ ] MediaPlayerManager.kt has positionMs/durationMs StateFlow + Player.Listener + ticker
   - [ ] VideoPlaybackScreen.kt displays timeline + observes position changes
   - [ ] Build compiles without errors

2. **Static analysis:**
   - [ ] TimeFormatter.formatMillis(ms: Long) handles negative inputs (returns "00:00:00")
   - [ ] MediaPlayerManager checks `duration > 0` before updating _durationMs (guards against UNKNOWN_TIME)
   - [ ] VideoPlaybackScreen uses `collectLatest` (efficient flow collection)
   - [ ] All seek methods (seekBack, seekForward, seekTo) include bounds checking

3. **Linked verification (next phase):**
   - After build succeeds, proceed to manual testing on Android Auto device
   - Test with Plex/Jellyfin/local media source
   - Verify timeline updates every ~200ms during playback
   - Verify seek buttons trigger position changes within ±2 seconds
</verification>

<success_criteria>
**Phase 1 complete when:**

1. **Code deliverables exist:**
   - TimeFormatter.kt with formatMillis(ms) function
   - MediaPlayerManager with positionMs/durationMs StateFlow and Player.Listener
   - VideoPlaybackScreen displaying timeline text in ActionStrip or message

2. **Build succeeds:**
   - ./gradlew assemble returns BUILD SUCCESSFUL

3. **Timeline display verified (manual):**
   - On Android Auto device or emulator, play a video
   - Observe timeline text (HH:MM:SS / HH:MM:SS) visible on screen
   - Observe timeline updates at least once per second during playback (actual: ~5/sec)

4. **Seek functionality verified (manual):**
   - Tap seek-back button → playback position rewinds ~10 seconds
   - Tap seek-forward button → playback position advances ~10 seconds
   - Seek accuracy within ±2 seconds of target (codec-dependent; document actual accuracy in UAT notes)

5. **No regressions:**
   - Existing play/pause button functionality unchanged
   - No playback stutter or audio distortion during seek
   - No crashes or ANRs during rapid button taps

6. **UAT pass:**
   - Duration updates in real-time (verified by observing timeline progression)
   - Seek works without stopping playback
   - No visible jank or frame drops
</success_criteria>

<output>
After completion, create `.planning/phases/1-progress-slider/1-PLAN-SUMMARY.md` documenting:
- Tasks completed (all 5)
- Build status (SUCCESS/FAILURE + any errors encountered)
- Manual test results (timeline visible? seeks work? any anomalies?)
- Files modified (line counts, diffs)
- Time spent per task
- Blockers or issues for next phase
- Commit hash and message
</output>
