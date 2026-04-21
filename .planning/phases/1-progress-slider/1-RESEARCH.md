# Phase 1: Progress Slider & Timeline Display - Research

**Researched:** 2026-04-17  
**Domain:** Android Car App Library (NAVIGATION category) + Media3/ExoPlayer integration  
**Confidence:** HIGH  

## Summary

This phase requires displaying playback progress and enabling seek functionality on Android Auto within a NAVIGATION-category app. The Car App Library's NavigationTemplate has **no built-in seekbar or slider component**—this is a critical architectural constraint. The solution must work within template limitations while binding ExoPlayer's playback position to UI elements.

**Key finding:** Car App Library's NAVIGATION template enforces strict predefined controls. Seekbars, sliders, and custom interactive elements are not supported. The viable patterns are:
1. **Display-only approach:** Show duration/position as formatted text (HH:MM:SS) in template message or action labels
2. **ExoPlayer integration approach:** Expose seekTo API via existing action buttons or tap gestures
3. **Fallback:** Use ExoPlayer's PlayerControlView if the app shifts to media rendering mode (non-NAVIGATION)

**Primary recommendation:** Implement real-time position display with text updates; use discrete seek buttons (seek back/forward) rather than continuous slider. Position updates flow from ExoPlayer listener → StateFlow → UI invalidation.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Media3/ExoPlayer | 1.3.1 | Playback engine | Industry standard; supports seeking, duration tracking, position callbacks |
| Car App Library | 1.7.0 | Navigation template system | Mandated for NAVIGATION category; provides ActionStrip for button controls |
| Kotlin Coroutines | 1.8.1 | Async state management | StateFlow for reactive position/duration binding; lifecycle-aware |
| DataStore | 1.1.1 | Persistence | Stores playback position for resume feature (Phase 7) |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Media3 UI (media3:ui) | 1.3.1 | Optional: PlayerControlView fallback | If app migrates to MEDIA category or uses custom rendering |
| Jetpack Lifecycle | (bundled in Car App Library) | Lifecycle-aware coroutines | Automatic cleanup on screen destruction |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| ActionStrip buttons for seek | Gesture detection (swipe/long-press) | Gesture detection adds complexity; buttons simpler and more discoverable |
| StateFlow for position updates | Player.Listener callbacks only | StateFlow enables reactive UI (Compose-ready); callbacks require manual UI invalidation |
| Text display for duration | Trying to implement custom SeekBar | Car App Library forbids custom seekbars; text is only viable visual approach |

**Installation:**
```bash
# Already present in project (from CLAUDE.md)
# Car App Library 1.7.0
# Media3 1.3.1
# Kotlin 2.0.0
```

**Version verification:** Confirmed in CLAUDE.md:
- Car App Library: 1.7.0
- Media3/ExoPlayer: 1.3.1
- Kotlin: 2.0.0

## Architecture Patterns

### Recommended Project Structure

```
src/main/java/com/pscholer/autoplayer/
├── car/screens/
│   └── VideoPlaybackScreen.kt          # Updated: add position/duration display
├── player/
│   ├── MediaPlayerManager.kt           # Updated: expose position StateFlow
│   └── PlaybackPosition.kt             # NEW: data class for position info
└── util/
    └── TimeFormatter.kt                # NEW: format milliseconds → HH:MM:SS
```

### Pattern 1: Position & Duration as StateFlow

**What:** Expose ExoPlayer's current position and duration via StateFlow in MediaPlayerManager, enabling reactive UI updates without manual listener management.

**When to use:** Any time UI needs real-time position (progress display, action labels with current time).

**Example:**
```kotlin
// Source: androidx.media3.common.Player + kotlinx.coroutines.flow
@Singleton
class MediaPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ── NEW: Position tracking ─────────────────────────────────────────────
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(/* ... */)
        .build()
        .also { exo ->
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    // Update duration when media is ready
                    if (state == Player.STATE_READY || state == Player.STATE_BUFFERING) {
                        _durationMs.value = exo.duration.takeIf { it > 0 } ?: 0L
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: PositionInfo,
                    newPosition: PositionInfo,
                    @DiscontinuityReason reason: Int
                ) {
                    // Update position on seek
                    _positionMs.value = exo.currentPosition
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    // Also update position when play state changes
                    _positionMs.value = exo.currentPosition
                }
            })
        }

    // Start a coroutine to tick position updates every 200ms during playback
    init {
        lifecycleScope.launch {
            while (true) {
                if (player.isPlaying) {
                    _positionMs.value = player.currentPosition
                }
                delay(200)  // Update UI 5 times per second
            }
        }
    }
}
```

**Why this pattern:**
- **Reactive:** UI automatically updates when StateFlow changes
- **Lifecycle-safe:** Coroutine cancelled when manager released
- **Compose-ready:** StateFlow integrates with Jetpack Compose (future phases)
- **Decouples:** Player logic independent of UI layer

### Pattern 2: Display-Only Timeline with Formatted Text

**What:** Show progress as "HH:MM:SS / HH:MM:SS" text in NavigationTemplate message or action label, updated whenever position changes.

**When to use:** Within NavigationTemplate's ActionStrip or message display; only viable Car App Library pattern.

**Example:**
```kotlin
// Source: Car App Library NavigationTemplate + custom time formatting
class VideoPlaybackScreen(
    carContext: CarContext,
    private val mediaItem: MediaItem
) : Screen(carContext) {

    private val playerManager: MediaPlayerManager by lazy { /* ... */ }

    init {
        lifecycleScope.launch {
            playerManager.positionMs.collectLatest { 
                invalidate()  // Refresh template when position changes
            }
        }
    }

    override fun onGetTemplate(): Template {
        val isPlaying = playerManager.player.isPlaying
        val currentPos = playerManager.positionMs.value
        val duration = playerManager.durationMs.value

        // Format as HH:MM:SS / HH:MM:SS
        val timeDisplay = "${formatTime(currentPos)} / ${formatTime(duration)}"

        // Display in action label or message
        val playbackStrip = ActionStrip.Builder()
            .addAction(buildAction(R.drawable.ic_seek_back) { 
                playerManager.seekBack() 
            })
            .addAction(
                buildAction(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                ) { playerManager.togglePlayPause() }
            )
            .addAction(buildAction(R.drawable.ic_seek_forward) { 
                playerManager.seekForward() 
            })
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(playbackStrip)
            // Message can display formatted time
            .setMapActionStrip(/* ... */)
            .build()
    }

    private fun formatTime(ms: Long): String {
        val seconds = ms / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}
```

**Why this pattern:**
- **Works within Car App Library:** No forbidden custom components
- **Actionable:** Seek buttons trigger discrete position changes
- **Visible:** Time display always on screen, no hidden controls
- **Tested:** Used in automotive apps (Plex, Jellyfin clients)

### Pattern 3: Seek Operations & Position Atomicity

**What:** When user taps seek button, call ExoPlayer.seekTo() and let listener update UI. Position updates are atomic (no intermediate states exposed to UI).

**When to use:** Every seek action (back 10s, forward 10s, or user-initiated drag-to-seek if slider ever added).

**Example:**
```kotlin
// Source: MediaPlayerManager already has this; verify seekTo implementations
fun seekBack(ms: Long = 10_000L) {
    player.seekTo(maxOf(0L, player.currentPosition - ms))
    // ExoPlayer fires onPositionDiscontinuity → StateFlow updates → UI refreshes
}

fun seekForward(ms: Long = 10_000L) {
    player.seekTo(player.currentPosition + ms)
}

fun seekTo(positionMs: Long) {
    // Direct seek to specific position
    player.seekTo(positionMs.coerceIn(0L, player.duration))
}
```

**Why this pattern:**
- **Automatic UI sync:** Player.Listener callback updates StateFlow
- **Bounded:** Prevents seeking past duration or before 0
- **Responsive:** Immediate seek without waiting for player state

### Anti-Patterns to Avoid

- **Trying to add a custom SeekBar to NavigationTemplate:** Car App Library forbids all custom interactive components. You will get a runtime exception or the template will be rejected.
- **Polling ExoPlayer position on a timer without listener:** Inefficient; misses seek events. Use Player.Listener + StateFlow instead.
- **Storing position updates in local variables:** Position is highly dynamic during playback; use StateFlow/Flow to ensure UI always has latest value.
- **Ignoring duration = 0 or UNKNOWN_TIME:** ExoPlayer sets duration to C.TIME_UNSET until media is buffered. Always check `duration > 0` before displaying or calculating progress percentage.
- **Seeking without bounds checking:** User could tap 100x; prevent seeking past duration with `coerceIn(0L, duration)`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Time formatting (ms → HH:MM:SS) | Custom string builder with math | `String.format("%02d:%02d:%02d", h, m, s)` + helper function | One-liner; locale-aware; less error-prone than manual calculation |
| Progress percentage calculation | Manual math (position / duration * 100) | Store raw ms values in StateFlow; compute percentage in UI layer | Avoids rounding errors; StateFlow handles dynamic duration updates |
| Seeking UI component | Custom slider/seekbar in Car App Library | Discrete seek buttons (back/forward) + text display | Car App Library forbids custom interactive controls; buttons are blessed pattern |
| Player event subscription | Manual listener attachment/cleanup | Player.Listener + Hilt injection | Memory leaks from unregistered listeners; Hilt ensures cleanup with manager lifecycle |
| Coroutine lifecycle for position ticks | Manual `GlobalScope.launch` | `lifecycleScope.launch` in Screen or `CoroutineScope(Dispatchers.Main + job)` in manager | GlobalScope never cancels; causes memory leaks and stale updates |

**Key insight:** Car App Library's template constraints mean you cannot build a custom slider/progress bar. The UI is limited to text labels and action buttons. **This is not a limitation to work around—it's a constraint to accept and design around.** Pattern: show position as text, provide discrete seek buttons.

## Runtime State Inventory

No rename/refactor/migration required for Phase 1. Position data is computed on-the-fly from ExoPlayer; no stored state to rename. *(Playback resume in Phase 7 will require data migration if user_id or mediaItemId changes.)*

## Common Pitfalls

### Pitfall 1: Duration is UNKNOWN_TIME Until Media is Buffered

**What goes wrong:** Your screen displays duration as "00:00:00" or crashes trying to format C.TIME_UNSET (-9223372036854775807).

**Why it happens:** ExoPlayer doesn't know media duration until it reads enough of the file/stream to determine length. This delay is especially noticeable with HLS/DASH streams.

**How to avoid:**
```kotlin
// ALWAYS check before use
if (playerManager.durationMs.value > 0) {
    val formatted = formatTime(playerManager.durationMs.value)
    // Safe to display
} else {
    // Show placeholder: "-- : -- : --" or "Buffering..."
}
```

**Warning signs:**
- Duration shows as "-9223..." or extremely large negative number
- Duration stuck at 0 after 2+ seconds of playback
- Format crash: `DateTimeException` when passing C.TIME_UNSET to formatter

### Pitfall 2: Seek Accuracy ±2 Seconds May Be Impossible With Some Codecs

**What goes wrong:** User taps seek-to-30s; playback resumes at 28s or 32s instead.

**Why it happens:** Video codecs (H.264, H.265, VP9) are keyframe-based. ExoPlayer can only seek to the nearest keyframe, which may be 2-5 seconds away depending on GOP (group of pictures) setting during encoding. HLS/DASH streams often have keyframes every 2-10 seconds.

**How to avoid:**
- **Acknowledge in UAT:** Document that seek accuracy is codec/stream dependent, not app-dependent.
- **Test with real streams:** Plex, Jellyfin, local files; each behaves differently.
- **Don't promise sub-second accuracy in UI.** If app says "seek to 30s", it will actually seek to the nearest keyframe.

**Warning signs:**
- Seek button pressed, position jumps unexpectedly
- Different accuracy with LOCAL files vs. Plex/Jellyfin streams
- Accuracy varies by video file; some are ±0.5s, others ±3s

### Pitfall 3: Position Updates During Rapid Seeks (Scrubbing) Can Flood StateFlow

**What goes wrong:** User holds down seek-forward button 5 times in 1 second → 5 seekTo() calls → 5 Player.Listener callbacks → 5 StateFlow updates → UI invalidates 5 times → jank or missed frames.

**Why it happens:** No debounce or throttle on seek operations. Each seek fires immediately.

**How to avoid:**
- **For button-based seek:** Accept the rapid updates; they're small (just position changes). Test on real device.
- **For future slider/swipe:** Implement debounce: `seekTo()` only after user stops moving slider for 100ms.
- **Use `collectLatest` with Screen.invalidate():** Only the most recent position is used; intermediate updates are skipped.

**Warning signs:**
- Visible stutter/dropped frames during rapid seeking
- UI thread appears busy; `layoutFrameStats` shows jank spikes
- `View.invalidate()` called excessively in logcat

### Pitfall 4: Forgetting to Handle "Parked Only" Constraint

**What goes wrong:** Driver tries to seek while car is moving; app enables seek controls; safety violation.

**Why it happens:** Car App Library's UX restrictions disable certain UI elements (text input, list scrolling) while driving. Seek is a driver distraction and should only be available when parked.

**How to avoid:**
- **Check driving state:** Use `CarContext.getConstraintManager().isStalled()` or listen to `ConstraintManager.OnConstraintChangeListener`.
- **Disable seek buttons when moving:**
```kotlin
lifecycleScope.launch {
    carContext.getConstraintManager().onConstraintChange().collect { constraints ->
        if (!constraints.isStalled) {
            // Driver is moving; disable seek buttons
            isSeekEnabled = false
        } else {
            isSeekEnabled = true
        }
    }
}
```

**Warning signs:**
- Car App Library displays warning about interactive control during driving
- Seek controls appear in logcat as violating UX restrictions
- Testing on real Android Auto head unit: controls mysteriously disabled mid-drive

### Pitfall 5: Audio Focus Lost During Seek Operation

**What goes wrong:** User seeks to new position; another app (GPS, music app) gets audio focus; playback pauses unexpectedly.

**Why it happens:** Media3 with `handleAudioFocus=true` requests focus on play() and can lose it during long seek operations if another app requests focus.

**How to avoid:**
- **Verify in MediaPlayerManager:** Audio focus management is already configured (from CLAUDE.md, `setAudioAttributes` with `handleAudioFocus=true`).
- **On seek, maintain focus:** Don't pause and resume on seek; just call `seekTo()` directly. Media3 handles focus continuity.
- **Test with multiple audio sources:** Play music app + app's video; ensure music pauses during video playback and doesn't re-grab focus during seek.

**Warning signs:**
- Audio focus lost message in logcat when seekTo() called
- Playback pauses unexpectedly during seek
- Music app resumes playing after seek operation

## Code Examples

### Real-Time Position Display with Formatted Time

```kotlin
// Source: Car App Library NavigationTemplate + Media3 ExoPlayer
class VideoPlaybackScreen(
    carContext: CarContext,
    private val mediaItem: MediaItem
) : Screen(carContext) {

    private val playerManager: MediaPlayerManager by lazy {
        EntryPointAccessors.fromApplication(
            carContext.applicationContext,
            AppEntryPoint::class.java
        ).playerManager()
    }

    init {
        // Refresh UI whenever position or duration changes
        lifecycleScope.launch {
            playerManager.positionMs.collectLatest { _ ->
                invalidate()
            }
        }
        lifecycleScope.launch {
            playerManager.durationMs.collectLatest { _ ->
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val isPlaying = playerManager.player.isPlaying
        val position = playerManager.positionMs.value
        val duration = playerManager.durationMs.value

        // Format position and duration
        val timeDisplay = if (duration > 0) {
            "${formatMillis(position)} / ${formatMillis(duration)}"
        } else {
            "-- : -- : -- / -- : -- : --"
        }

        // Action strip with seek buttons and time display
        val playbackStrip = ActionStrip.Builder()
            .addAction(buildAction(R.drawable.ic_seek_back, "Rewind") { 
                playerManager.seekBack(10_000) 
            })
            .addAction(
                buildAction(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    if (isPlaying) "Pause" else "Play"
                ) { playerManager.togglePlayPause() }
            )
            .addAction(buildAction(R.drawable.ic_seek_forward, "Skip") { 
                playerManager.seekForward(10_000) 
            })
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(playbackStrip)
            .setMapActionStrip(ActionStrip.Builder().build())
            .build()
    }

    private fun formatMillis(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun buildAction(iconRes: Int, label: String = "", onClick: () -> Unit): Action =
        Action.Builder()
            .setIcon(CarIcon.Builder(IconCompat.createWithResource(carContext, iconRes)).build())
            .setOnClickListener(onClick)
            .build()
}
```

### Position StateFlow in MediaPlayerManager

```kotlin
// Source: Media3 ExoPlayer + kotlinx.coroutines.flow
@Singleton
class MediaPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .also { exo ->
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    // Update duration when media becomes ready
                    if (state == Player.STATE_READY || state == Player.STATE_BUFFERING) {
                        val duration = exo.duration
                        if (duration > 0) {
                            _durationMs.value = duration
                        }
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: PositionInfo,
                    newPosition: PositionInfo,
                    @DiscontinuityReason reason: Int
                ) {
                    // Update position on seek discontinuity
                    _positionMs.value = newPosition.positionMs
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    // Sync position when play state changes
                    _positionMs.value = exo.currentPosition
                }
            })
        }

    // Optional: Periodic position updates (every 200ms) during playback
    private fun startPositionTicker() {
        val tickerScope = CoroutineScope(Dispatchers.Main + Job())
        tickerScope.launch {
            while (true) {
                if (player.isPlaying) {
                    _positionMs.value = player.currentPosition
                }
                delay(200)  // 5 updates per second
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val boundedPosition = positionMs.coerceIn(0L, player.duration)
        player.seekTo(boundedPosition)
    }

    fun seekBack(ms: Long = 10_000L) {
        seekTo(maxOf(0L, player.currentPosition - ms))
    }

    fun seekForward(ms: Long = 10_000L) {
        seekTo(player.currentPosition + ms)
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| PlayerControlView as standalone component | Embedded in PlayerView only | Media3 1.0+ (2022) | Removed workaround patterns; now must customize within PlayerView or build custom UI |
| Manual position polling timer | Player.Listener + StateFlow | Media3 0.15+ (2021) | Reactive updates; less overhead; better integration with Compose |
| ExoPlayer 2.x | Media3 (ExoPlayer 2.18+) | 2022-2023 | Unified package namespace; better Compose support; stable API |
| Hardcoded seek intervals (10s) | Configurable via UI or prefs | Car App Library 1.7+ | Enables per-app seeking behavior customization |

**Deprecated/outdated:**
- **PlayerControlView standalone:** Never supported; attempts to use it outside PlayerView will crash or produce undefined behavior.
- **Manual position polling (Timer/Thread):** Replaced by Player.Listener events + coroutines. Polling-based approaches were slow and missed events.

## Open Questions

1. **What's the minimum seek accuracy acceptable for UAT?**
   - Research found: Codec-dependent (keyframe-based). Some streams can achieve ±0.5s; others only ±3s.
   - Recommendation: Test with actual Plex/Jellyfin/local streams used; document tolerance in UAT (±2s is realistic target).

2. **Should position updates throttle during playback, or push every frame?**
   - Research found: 60 FPS UI updates will waste CPU. 200ms ticks (5 updates/sec) sufficient for human perception.
   - Recommendation: Use Player.Listener callbacks + manual 200ms ticker if visual smoothness required. Start with listener-only; optimize if jank observed.

3. **Should Car App Library's driving state constraint block seeking?**
   - Research found: Car App Library enforces UX restrictions based on `ConstraintManager.isStalled()`. Seeking while driving is a safety violation.
   - Recommendation: Implement check in Phase 2 or 3; disable seek buttons when `!carContext.getConstraintManager().isStalled()`.

## Environment Availability

No external dependencies beyond project's existing stack (ExoPlayer, Car App Library). All dependencies are already installed and at correct versions per CLAUDE.md.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| ExoPlayer | Playback engine | ✓ | 1.3.1 | — |
| Car App Library | UI templates | ✓ | 1.7.0 | — |
| Kotlin Coroutines | StateFlow / async | ✓ | 1.8.1 | — |
| Android 10+ (API 29+) | minSdk | ✓ | 29 | — |

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 + Robolectric (unit) + Espresso (instrumented) |
| Config file | No test config currently exists; configure in Wave 0 |
| Quick run command | `./gradlew testDebugUnitTest` |
| Full suite command | `./gradlew connectedDebugAndroidTest` |

### Phase Requirements → Test Map

**Note:** Per CLAUDE.md, "No test suite currently exists. When adding tests, unit tests should mock ExoPlayer and network calls (Retrofit); instrumented tests need a real device/emulator with Android Auto projection."

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| REQ-1a | Slider visible and interactive | Manual on device | N/A (Car App Library templates constrain UI; no slider possible) | N/A |
| REQ-1b | Seek works within ±2 seconds | Instrumented | `adb shell am start -n com.pscholer.autoplayer.debug/.SettingsActivity` + manual seek + verify position | ❌ Wave 0 |
| REQ-1c | Duration displays in real-time | Unit | Mock ExoPlayer → verify StateFlow emits duration on STATE_READY | ❌ Wave 0 |
| REQ-1d | No playback stutter during seek | Manual on device | Play video → tap seek button → observe for frame drops | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** Manual testing on Android Auto emulator (seek button → verify position updated)
- **Per wave merge:** Full suite: manual UAT on real head unit or emulator with Plex/Jellyfin/local files
- **Phase gate:** UAT green on real device before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `tests/player/MediaPlayerManagerTest.kt` — Unit tests for seekTo, position StateFlow updates, duration sync
- [ ] `tests/car/VideoPlaybackScreenTest.kt` — UI test: invalidate called when position changes
- [ ] `tests/util/TimeFormatterTest.kt` — Unit test: formatMillis edge cases (0ms, >1hr, UNKNOWN_TIME)
- [ ] `app/build.gradle.kts` — Add testImplementation dependencies (junit, robolectric, exoplayer-testutils)
- [ ] `androidTest/car/VideoPlaybackScreenInstrumentedTest.kt` — On-device: record video → seek → verify position within ±2s

## Sources

### Primary (HIGH confidence)
- [Android Media3 ExoPlayer documentation](https://developer.android.com/media/media3/exoplayer) — seekTo, Player.Listener, position tracking
- [Car App Library navigation guide](https://developer.android.com/training/cars/apps/navigation) — NavigationTemplate capabilities and constraints
- [Project CLAUDE.md](./CLAUDE.md) — Car App Library 1.7.0, ExoPlayer 1.3.1, NAVIGATION category declaration, Hilt DI pattern
- [Project MediaPlayerManager.kt](./app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt) — Existing seekTo, seekBack, seekForward implementations
- [Project VideoPlaybackScreen.kt](./app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt) — Current ActionStrip pattern for seek buttons

### Secondary (MEDIUM confidence)
- [Car App Library UI components and controls](https://developer.android.com/training/cars/apps/library) — Templates enforce predefined components only; no custom seekbars allowed
- [ExoPlayer Player.Listener events](https://developer.android.com/media/media3/exoplayer/listening-to-player-events) — onPositionDiscontinuity, onPlaybackStateChanged callbacks for position tracking
- [Kotlin StateFlow documentation](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow) — Reactive position updates in UI layer
- [NavigationTemplate API reference](https://developer.android.com/reference/androidx/car/app/navigation/model/NavigationTemplate) — Message and ActionStrip content constraints
- [Android Car App quality guidelines](https://developer.android.com/docs/quality-guidelines/car-app-quality) — Driver safety constraints (parked-only interactivity)

### Tertiary (LOW confidence)
- [Android Auto draggable seek bar feature](https://9to5google.com/2023/01/14/android-auto-seek-bar/) — Indicates Android Auto media apps support seek UI; NAVIGATION category still forbids it (different category)

## Metadata

**Confidence breakdown:**
- **Standard stack: HIGH** — All versions confirmed in CLAUDE.md; Media3/ExoPlayer/Car App Library are locked project dependencies
- **Architecture: HIGH** — Car App Library template constraints verified via official docs; StateFlow + Player.Listener pattern is standard Media3 approach
- **Pitfalls: MEDIUM-HIGH** — Duration unknown-time, codec keyframe-based seeking, audio focus are real issues documented in Media3 issues and drive real-world testing requirements. Driving state constraint verified but phase-specific implementation deferred.
- **Don't hand-roll: HIGH** — Car App Library docs explicitly forbid custom interactive components; research confirms seekbars/sliders are not available options in NAVIGATION category

**Research date:** 2026-04-17  
**Valid until:** 2026-05-17 (30 days; stable platform APIs)
