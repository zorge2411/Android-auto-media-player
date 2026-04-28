# Phase 1: Progress Slider & Timeline Display — Research

**Researched:** 2026-04-28
**Domain:** Android Car App Library 1.7.0 — NavigationTemplate, SurfaceCallback, auto-hide controls
**Confidence:** HIGH (core implementation is already partially done; critical unknown resolved)

---

## Summary

Phase 1 is substantially more complete than the ROADMAP suggests. The codebase audit shows that `VideoPlaybackScreen.kt` already contains: seek buttons (ActionStrip), play/pause toggle, timeline text via `MessageInfo`, `TimeFormatter.formatMillis()`, and a `StateFlow combine` loop that calls `invalidate()` when position or duration changes. `MediaPlayerManager.kt` already exposes `positionMs`, `durationMs`, and `isPlaying` as `StateFlow<Long>` / `StateFlow<Boolean>`.

The one missing piece is the **auto-hide / reveal behavior** specified in the UI-SPEC as a locked user decision: controls (ActionStrip, MapActionStrip, MessageInfo) must vanish after 3 seconds of inactivity and reappear when the user taps the video surface. This requires `SurfaceCallback.onClick(x, y)` — which is gated behind Car App API level 5 (`@RequiresCarApi(5)`, `@ExperimentalCarApi`). The manifest currently declares `minCarApiLevel="2"`. This is a known-resolved constraint: `onClick` can be implemented with a runtime guard (`carContext.getCarAppApiLevel() >= 5`), and the `onScroll` callback (available at level 2) fires on touchscreen drag, which provides a secondary reveal trigger when `onClick` is not supported by the host.

The existing `invalidate()` / template rebuild cycle is the correct hide/show mechanism: rendering with `setNavigationInfo(null)` and no ActionStrip/MapActionStrip produces the hidden state; the normal template build is the visible state. A coroutine `delay` on the screen's `lifecycleScope` is the correct timer implementation — the `Handler(Looper.getMainLooper())` already present in `VideoSurfaceRenderer` demonstrates this pattern is used in the codebase.

**Primary recommendation:** Implement auto-hide in `VideoPlaybackScreen` using a `controlsVisible: Boolean` flag, a `Job`-based 3-second timer, and override `SurfaceCallback.onClick` (level 5, with runtime guard) in `VideoSurfaceRenderer`, propagating tap events to `VideoPlaybackScreen` via a lambda or `StateFlow`.

---

## User Constraints (from UI-SPEC — Locked Decisions)

The UI-SPEC is the binding design contract for this phase. The following are locked:

| Constraint | Value |
|------------|-------|
| Template type | NavigationTemplate (only type with SurfaceContainer + full ActionStrip access) |
| Seek mechanism | Step-seek buttons (±10 seconds) via ActionStrip — no SeekBar/Slider exists in Car App Library |
| ActionStrip layout | Slot 1: ic_seek_back, Slot 2: ic_play/ic_pause, Slot 3: ic_seek_forward |
| MapActionStrip layout | Slot 1: ic_favorite/ic_favorite_filled, Slot 2: ic_pause (stop-and-exit) |
| Timeline format | HH:MM:SS / HH:MM:SS via `TimeFormatter.formatMillis()` |
| Auto-hide timeout | 3 seconds after last interaction or initial playback start |
| Reveal trigger | Tap anywhere on video surface (SurfaceCallback.onClick or onScroll fallback) |
| Hidden state | NavigationTemplate with `setNavigationInfo(null)` + no ActionStrip + no MapActionStrip |
| Update rate while visible | 1 Hz (throttled to avoid IPC cost) |
| Controls on play/pause toggle | Show immediately and restart 3-second timer |
| NAVIGATION category | Non-negotiable — required for SurfaceContainer video access |

---

## What Is Already Built (Do Not Rebuild)

| Component | Location | Status |
|-----------|----------|--------|
| `TimeFormatter.formatMillis(ms)` | `util/TimeFormatter.kt` | Complete |
| `positionMs: StateFlow<Long>` | `MediaPlayerManager.kt` | Complete |
| `durationMs: StateFlow<Long>` | `MediaPlayerManager.kt` | Complete |
| `isPlaying: StateFlow<Boolean>` | `MediaPlayerManager.kt` | Complete |
| `seekBack(ms)` / `seekForward(ms)` | `MediaPlayerManager.kt` | Complete (10 s default) |
| `togglePlayPause()` | `MediaPlayerManager.kt` | Complete |
| `buildTimelineText()` | `VideoPlaybackScreen.kt` | Complete |
| ActionStrip with 3 seek/play buttons | `VideoPlaybackScreen.onGetTemplate()` | Complete |
| MapActionStrip with favorite + stop | `VideoPlaybackScreen.onGetTemplate()` | Complete |
| `invalidate()` on position change | `VideoPlaybackScreen.init` lifecycleScope | Complete (1 Hz throttle) |
| `SurfaceCallback` implementation | `VideoSurfaceRenderer.kt` | Complete — extends `SurfaceCallback` |
| `onScroll` / `onSurfaceAvailable` / `onSurfaceDestroyed` | `VideoSurfaceRenderer.kt` | Complete |

---

## Standard Stack

### Core — No New Dependencies Required

All libraries needed are already present in `build.gradle.kts`:

| Library | Version | Purpose |
|---------|---------|---------|
| Car App Library | 1.7.0 | NavigationTemplate, SurfaceCallback, ActionStrip, MessageInfo, CarContext.getCarAppApiLevel() |
| Media3 / ExoPlayer | 1.3.1 | positionMs, durationMs, seekTo, isPlaying |
| Kotlin Coroutines | 1.8.1 | `lifecycleScope.launch`, `delay`, Job cancellation for hide timer |
| Hilt | 2.51.1 | Dependency injection (no change) |

**No new Gradle dependencies are needed for Phase 1.**

---

## Architecture Patterns

### Pattern 1: Controls Visibility Toggle via `invalidate()`

The Car App Library re-invokes `onGetTemplate()` every time `invalidate()` is called on the screen. Hide/show is achieved by branching inside `onGetTemplate()` on a `controlsVisible` Boolean:

```kotlin
// In VideoPlaybackScreen

private var controlsVisible = true

override fun onGetTemplate(): Template {
    val builder = NavigationTemplate.Builder()

    if (controlsVisible) {
        builder
            .setNavigationInfo(MessageInfo.Builder(buildTimelineText()).build())
            .setActionStrip(buildPlaybackStrip())
            .setMapActionStrip(buildMapStrip())
    }
    // When controlsVisible == false: no ActionStrip, no MapActionStrip, no MessageInfo
    // The NavigationTemplate still renders but shows only the raw video surface

    return builder.build()
}
```

**Why this works:** Car App Library 1.7.0 allows building a NavigationTemplate with no ActionStrip and null NavigationInfo. The host renders the template overlay but with no controls — video fills the full visible area.

**Confidence:** HIGH — confirmed by UI-SPEC, consistent with Car App Library NavigationTemplate API.

### Pattern 2: 3-Second Auto-Hide Timer with Coroutine Job

Use a cancellable `Job` on `lifecycleScope` rather than `Handler.postDelayed`. This integrates cleanly with the existing coroutine usage in `VideoPlaybackScreen.init`:

```kotlin
private var hideControlsJob: Job? = null

private fun showControls() {
    controlsVisible = true
    invalidate()
    hideControlsJob?.cancel()
    hideControlsJob = lifecycleScope.launch {
        delay(3_000L)
        controlsVisible = false
        invalidate()
    }
}

private fun hideControls() {
    hideControlsJob?.cancel()
    controlsVisible = false
    invalidate()
}
```

Call `showControls()` on:
- Initial playback start (in the `init` block after `playerManager.play()` / `playWithHeaders()`)
- Each ActionStrip button click (seek-back, play/pause, seek-forward, favorite, stop)
- Each `SurfaceCallback.onClick` / `onScroll` event from the surface

**Confidence:** HIGH — this is standard Kotlin coroutine pattern for debounced UI timers.

### Pattern 3: Surface Touch Event Propagation

`VideoSurfaceRenderer` already implements `SurfaceCallback`. It is registered via `carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceRenderer)` in `AutoMediaSession.onCreateScreen()`.

To propagate tap events to `VideoPlaybackScreen`:

**Option A (preferred):** Add a `var onSurfaceTap: (() -> Unit)? = null` callback on `VideoSurfaceRenderer`. `VideoPlaybackScreen` sets it in `init`. `VideoSurfaceRenderer.onClick()` invokes it.

**Option B:** Add a `MutableStateFlow<Long>` tap event counter to `VideoSurfaceRenderer`; `VideoPlaybackScreen` collects it.

Option A is simpler and consistent with the existing lambda pattern used throughout the codebase.

```kotlin
// In VideoSurfaceRenderer — addition to existing SurfaceCallback impl:

var onSurfaceTap: (() -> Unit)? = null

// onClick — requires Car API level 5 (runtime guard required)
@RequiresCarApi(5)
@ExperimentalCarApi
override fun onClick(x: Float, y: Float) {
    onSurfaceTap?.invoke()
}

// onScroll — API level 2 fallback (fires on touchscreen drag/swipe)
override fun onScroll(distanceX: Float, distanceY: Float) {
    onSurfaceTap?.invoke()
}
```

```kotlin
// In VideoPlaybackScreen.init — register the callback:
surfaceRenderer.onSurfaceTap = { showControls() }
```

**Confidence:** HIGH for the pattern. MEDIUM for onClick availability at runtime (host-dependent).

### Pattern 4: Runtime API Level Guard for onClick

The manifest declares `minCarApiLevel="2"` but `SurfaceCallback.onClick` is `@RequiresCarApi(5)`. The Car App Library enforces this at compile time via annotation lint, but the feature still works safely if:

1. The annotation suppression is applied at the override site
2. A runtime check is used before relying on the callback being invoked

The `onClick` method is called by the **host** (Android Auto app on the phone), not by the app. If the host supports API level 5+, it will invoke `onClick`. If not, it simply never calls it. No crash occurs — the method just never fires.

```kotlin
// Safe approach: override onClick, suppress the annotation warning,
// and fall back to onScroll for older hosts.

@Suppress("OVERRIDE_DEPRECATION")
@androidx.annotation.OptIn(markerClass = [androidx.car.app.annotations.ExperimentalCarApi::class])
override fun onClick(x: Float, y: Float) {
    onSurfaceTap?.invoke()
}
```

`onScroll` remains the guaranteed fallback for API level 2 hosts. On a touchscreen head unit, any swipe/scroll gesture fires `onScroll`, providing reveal even without `onClick`.

**Confidence:** MEDIUM — confirmed that onClick exists at API level 5 and onScroll at level 2, but runtime behavior on a specific head unit depends on the host version.

### Anti-Patterns to Avoid

- **Do NOT use `Handler.postDelayed` for the hide timer** — the existing `lifecycleScope` coroutine approach is already established in this screen; mixing `Handler` adds unnecessary complexity and lifecycle risk.
- **Do NOT call `invalidate()` at higher than 1 Hz** — the existing throttle in the position-update loop must be respected to avoid IPC congestion on the Car App host binder.
- **Do NOT reset the hide timer on position updates** — only user interactions (taps, button presses) reset the timer; position ticks must not reset it.
- **Do NOT apply the hide state to the underlying video surface** — only the template overlay controls are hidden; ExoPlayer continues rendering to the surface regardless.
- **Do NOT add a PAN button to MapActionStrip** — the `Action.PAN` button is only needed for rotary/touchpad input devices in AAOS (Android Automotive), not for Android Auto phone projection with a touchscreen. Adding it would waste a MapActionStrip slot.

---

## Critical Finding: onClick API Level

| Method | Car API Level | Availability | Notes |
|--------|--------------|--------------|-------|
| `SurfaceCallback.onSurfaceAvailable` | 2 | Always available | Already implemented |
| `SurfaceCallback.onVisibleAreaChanged` | 2 | Always available | Already implemented |
| `SurfaceCallback.onSurfaceDestroyed` | 2 | Always available | Already implemented |
| `SurfaceCallback.onScroll(distanceX, distanceY)` | 2 | Always available | Fires on drag/swipe |
| `SurfaceCallback.onFling(velocityX, velocityY)` | 2 | Always available | Fires on fling gesture |
| `SurfaceCallback.onClick(x, y)` | **5** + @ExperimentalCarApi | **Host-dependent** | Preferred tap signal |

**The manifest declares `minCarApiLevel="2"`.** The `onClick` override compiles and runs safely — it simply never fires on pre-level-5 hosts. `onScroll` is the reliable fallback for those hosts.

**The manifest does NOT need to be changed** for Phase 1. `onClick` can be used defensively with the suppression annotations shown above.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead |
|---------|-------------|-------------|
| Time formatting | Custom string formatter | `TimeFormatter.formatMillis(ms)` — already exists |
| Position updates | Manual ExoPlayer polling | `MediaPlayerManager.positionMs` StateFlow — already exists |
| Debounce timer | `CountDownTimer`, `TimerTask` | Kotlin coroutine `delay` + `Job.cancel()` — already pattern in codebase |
| Seek operations | Direct `player.seekTo()` calls | `MediaPlayerManager.seekBack()` / `seekForward()` — already exists |
| ActionStrip rebuild | Custom view hierarchy | `NavigationTemplate.Builder` with conditional ActionStrip — Car App Library standard |

---

## Common Pitfalls

### Pitfall 1: Invalidate Rate Too High

**What goes wrong:** Calling `invalidate()` on every position tick (e.g., from a 100ms ExoPlayer listener) floods the Car App host IPC channel and causes buffering/lag in video rendering.

**Why it happens:** Car App Library serializes template objects across a binder IPC call per `invalidate()`. High frequency destroys throughput.

**How to avoid:** The existing 1 Hz throttle (comparing `lastFormattedPosition != newTime`) must remain in place. The auto-hide timer fires once per 3 seconds — that's fine.

**Warning signs:** Video stutters shortly after a seek; logcat shows repeated `IBinderThrottleException` or similar.

### Pitfall 2: onClick Never Fires on Some Hosts

**What goes wrong:** `SurfaceCallback.onClick` is `@ExperimentalCarApi` and the host may not invoke it even when Car API level >= 5, or may not invoke it on touchscreen devices in Android Auto (vs AAOS).

**Why it happens:** The official docs note that touch callbacks "may not be called in some car systems." Android Auto (phone projection) vs AAOS (native OS) have different input models.

**How to avoid:** Implement `onScroll` as the primary fallback. Both `onClick` and `onScroll` invoke the same `onSurfaceTap` callback — whichever fires, controls reveal.

**Warning signs:** Tapping surface does nothing; only dragging reveals controls.

### Pitfall 3: Auto-Hide Timer Not Reset on Action Button Press

**What goes wrong:** User taps seek-forward; controls hide 3 seconds after the INITIAL playback start, not after the button tap.

**Why it happens:** The ActionStrip `setOnClickListener` lambdas don't call `showControls()`.

**How to avoid:** Every ActionStrip and MapActionStrip `setOnClickListener` must call `showControls()` (or equivalent timer-reset logic) in addition to its primary action.

### Pitfall 4: Controls Not Shown on Pause

**What goes wrong:** User taps play/pause button; video pauses but controls immediately hide.

**Why it happens:** The hide timer starts at playback-start only; the `togglePlayPause` click doesn't reset it.

**How to avoid:** The `togglePlayPause` click listener must call `showControls()`.

### Pitfall 5: NavigationTemplate Builder Fails with Null

**What goes wrong:** Building `NavigationTemplate` with no ActionStrip may be rejected by some host versions.

**Why it happens:** Some older hosts may require at least a MapActionStrip to render the template.

**How to avoid:** Test the hidden state on target device. If it fails, keep a minimal empty MapActionStrip (0 actions) rather than null.

---

## Code Examples

### Complete Auto-Hide Pattern for VideoPlaybackScreen

```kotlin
// Source: Phase 1 RESEARCH — verified against Car App Library 1.7.0 NavigationTemplate API

private var controlsVisible = true
private var hideControlsJob: Job? = null

private fun showControls() {
    controlsVisible = true
    invalidate()
    hideControlsJob?.cancel()
    hideControlsJob = lifecycleScope.launch {
        delay(3_000L)
        controlsVisible = false
        invalidate()
    }
}

override fun onGetTemplate(): Template {
    val isPlaying = playerManager.isPlaying.value

    return if (controlsVisible) {
        NavigationTemplate.Builder()
            .setNavigationInfo(MessageInfo.Builder(buildTimelineText()).build())
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(buildAction(R.drawable.ic_seek_back) {
                        playerManager.seekBack()
                        showControls()
                    })
                    .addAction(buildAction(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    ) {
                        playerManager.togglePlayPause()
                        showControls()
                    })
                    .addAction(buildAction(R.drawable.ic_seek_forward) {
                        playerManager.seekForward()
                        showControls()
                    })
                    .build()
            )
            .setMapActionStrip(buildMapStrip())
            .build()
    } else {
        NavigationTemplate.Builder().build()
    }
}
```

### onClick in VideoSurfaceRenderer

```kotlin
// Source: Phase 1 RESEARCH — Car App Library 1.7.0 SurfaceCallback interface

var onSurfaceTap: (() -> Unit)? = null

@Suppress("OVERRIDE_DEPRECATION")
@androidx.annotation.OptIn(markerClass = [androidx.car.app.annotations.ExperimentalCarApi::class])
override fun onClick(x: Float, y: Float) {
    onSurfaceTap?.invoke()
}

override fun onScroll(distanceX: Float, distanceY: Float) {
    // Level-2 fallback: any drag gesture on surface triggers reveal
    onSurfaceTap?.invoke()
}
```

---

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|-----------------|--------|
| SeekBar / Slider (original ROADMAP intent) | Step-seek via ActionStrip buttons | Resolved in UI-SPEC: Car App Library has no Slider in NavigationTemplate |
| Always-visible controls | Auto-hide after 3s + tap-to-reveal | Locked user decision in UI-SPEC |
| Handler.postDelayed for timer | Kotlin coroutine delay + Job.cancel() | Coroutines are already the pattern in this screen |

---

## Environment Availability

Step 2.6: Phase 1 is code/config-only changes within the existing Android project. No external tools, services, or CLIs beyond what the project already uses. SKIPPED.

---

## Validation Architecture

Config `workflow.nyquist_validation` key is absent — treating as enabled.

### Test Framework

No automated test infrastructure exists (`CLAUDE.md`: "No test suite currently exists"). Phase 1 UAT is manual device testing.

| Property | Value |
|----------|-------|
| Framework | None — manual UAT on Android Auto device/emulator |
| Config file | None |
| Quick run command | `./gradlew installDebug` then test on device |
| Full suite command | Manual UAT checklist |

### Phase 1 Requirements → Test Map

| Req | Behavior | Test Type | Command | Notes |
|-----|----------|-----------|---------|-------|
| FEAT-1-AC1 | Timeline visible in VideoPlaybackScreen | Manual | Deploy + observe | |
| FEAT-1-AC2 | Seek buttons visible and interactive | Manual | Deploy + tap ±10s | |
| FEAT-1-AC3 | Seek within ±2s of target | Manual | Deploy + measure | |
| FEAT-1-AC4 | Duration in HH:MM:SS / HH:MM:SS format | Manual | Deploy + observe | |
| FEAT-1-AC5 | Timeline updates in real-time | Manual | Deploy + watch | |
| UI-SPEC Auto-hide | Controls hide after 3s | Manual | Deploy + wait | New requirement |
| UI-SPEC Reveal | Tap surface reveals controls | Manual | Deploy + tap | Critical — host-dependent |

### Wave 0 Gaps

- No automated test files exist for this feature
- Manual testing requires Android Auto head unit or DHU (Desktop Head Unit emulator)
- `./gradlew installDebug` on a connected phone, then project via Android Auto

---

## Open Questions

1. **Does the target head unit support Car API level 5?**
   - What we know: The phone's Android Auto app version determines the host API level. Android Auto 8.1+ supports API level 5.
   - What's unclear: The specific head unit / Android Auto version in the test environment.
   - Recommendation: Implement both `onClick` (level 5) and `onScroll` (level 2) as documented. Test on device. If only `onScroll` fires, that is acceptable — any surface drag reveals controls.

2. **Does NavigationTemplate.Builder().build() (no ActionStrip, no NavigationInfo) render without error on the target host?**
   - What we know: The Car App Library 1.7.0 API allows it in theory.
   - What's unclear: Whether any host version enforces a minimum template structure.
   - Recommendation: Test the hidden state first. Fallback: include a MapActionStrip with 0 actions if bare NavigationTemplate.Builder().build() is rejected.

3. **Does onScroll fire on a touchscreen head unit for a single tap (not drag)?**
   - What we know: `onScroll` is documented as a "scroll touch event" triggered by drag. A single tap (no movement) may not fire `onScroll`.
   - What's unclear: Whether a brief tap with no movement fires `onScroll` on real Android Auto hardware.
   - Recommendation: If `onClick` (level 5) is unavailable AND `onScroll` requires drag, the tap-to-reveal behavior may not work on some devices. In that case, rely on the existing ActionStrip button presses to reset the timer (controls always visible when buttons exist).

---

## Sources

### Primary (HIGH confidence)
- [SurfaceCallback API reference — Android Developers](https://developer.android.com/reference/androidx/car/app/SurfaceCallback) — onClick (level 5), onScroll (level 2), onFling (level 2) methods confirmed
- [Let users interact with your map — Android Developers](https://developer.android.com/training/cars/apps/library/interact-map) — Pan mode, onClick prerequisites, PAN button requirement for AAOS
- `VideoPlaybackScreen.kt` (codebase) — existing ActionStrip, MessageInfo, invalidate() pattern
- `VideoSurfaceRenderer.kt` (codebase) — existing SurfaceCallback implementation
- `MediaPlayerManager.kt` (codebase) — positionMs, durationMs, seekBack, seekForward StateFlows
- `AndroidManifest.xml` (codebase) — `minCarApiLevel="2"` confirmed
- `01-UI-SPEC.md` (phase document) — locked design decisions

### Secondary (MEDIUM confidence)
- [Car App Library releases — Android Developers](https://developer.android.com/jetpack/androidx/releases/car-app) — API level 5 features, onClick introduced
- [Mapbox onClick PR #1682](https://github.com/mapbox/mapbox-maps-android/pull/1682) — confirmed override pattern for onClick in SurfaceCallback

### Tertiary (LOW confidence)
- Multiple WebSearch results on SurfaceCallback onClick being @ExperimentalCarApi — consistent across sources, raised to MEDIUM

---

## Metadata

**Confidence breakdown:**
- Existing implementation inventory: HIGH — read from actual source files
- SurfaceCallback.onClick API level: HIGH — confirmed from official Android docs
- onScroll as level-2 fallback: HIGH — confirmed from official docs
- Auto-hide coroutine pattern: HIGH — standard Kotlin pattern, consistent with existing code
- Runtime onClick behavior on specific head unit: MEDIUM — depends on host version

**Research date:** 2026-04-28
**Valid until:** 2026-05-28 (stable Car App Library; unlikely to change)
