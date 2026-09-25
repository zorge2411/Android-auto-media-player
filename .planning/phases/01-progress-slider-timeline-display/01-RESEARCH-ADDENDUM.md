# Phase 1: Research Addendum (planning-time corrections)

**Date:** 2026-09-25
**Why:** Before planning, the 01-RESEARCH.md claims were checked against the androidx-main source (`car/app/app/src/main/java/androidx/car/app/...`) and the current Android for Cars "Let users interact with your map" guide. Four findings change the implementation. Plans 01-01 and 01-02 follow this addendum wherever it conflicts with RESEARCH.md.

| # | RESEARCH.md said | Actual | Impact |
|---|------------------|--------|--------|
| 1 | Hidden state = `NavigationTemplate.Builder().build()` (no strips) — "HIGH confidence" | `NavigationTemplate.Builder.build()` throws `IllegalStateException("Action strip for this template must be set")`. `ActionStrip.Builder.build()` with zero actions also throws (“must contain at least one action”). | Hidden state = omit `setNavigationInfo` only. Both strips are always set. |
| 2 | "Do NOT add a PAN button to MapActionStrip" | Docs: "To receive map interactivity callbacks, you **must** add an `Action.PAN` button in the map action strip … If your app omits the `Action.PAN` button … it doesn't receive user input from the `SurfaceCallback` methods." On touchscreens the PAN button isn't displayed. | `Action.PAN` is always in the map strip; without it tap-to-reveal can never work. |
| 3 | `onClick` needs `@ExperimentalCarApi` opt-in | `SurfaceCallback.onClick` is `@RequiresCarApi(5)` only; it is a default method that is "may not be called in some car systems". | Plain override; no opt-in annotation. |
| 4 | `positionMs` StateFlow is "Complete" | `_positionMs` is only written in `onPlaybackStateChanged` / `onPositionDiscontinuity`. It never updates during steady playback. | New 1 Hz ticker (Plan 01-01). Without it, FEAT-1-AC5 fails. |

**Additional host behaviour (docs):** "The action strip is concealed when in the idle state and reappears in the active state." The host already auto-hides both strips. The app's own 3 s timer therefore only needs to govern the timeline `MessageInfo`. Plan 03 row 5 records how the host's timing compares to ours.

**Minor bugs found:**
- `TimeFormatter` wraps hours at 24 and uses the default-locale `String.format`.
- The screen's invalidate de-dupe ignores `isPlaying`, so pausing within the same second leaves a stale play/pause icon.

**Todo disposition:**
- `add-search-slider-on-video-playback-screen`: resolved by this phase as step-seek (no slider widget exists in Car App Library). A possible later enhancement is tap-to-seek using `onClick(x, y)` on a progress bar drawn into the GL pipeline. That is deferred; it needs a GL overlay and Car API ≥ 5.
- `switch-back-icon-from-pause-icon-to-relevant-icon`: folded into Plan 01-02 (uses the existing `ic_close`).

**Sources:**
- https://github.com/androidx/androidx/blob/androidx-main/car/app/app/src/main/java/androidx/car/app/navigation/model/NavigationTemplate.java
- https://github.com/androidx/androidx/blob/androidx-main/car/app/app/src/main/java/androidx/car/app/model/ActionStrip.java
- https://github.com/androidx/androidx/blob/androidx-main/car/app/app/src/main/java/androidx/car/app/SurfaceCallback.java
- https://developer.android.com/training/cars/apps/library/interact-map
