---
phase: 01-progress-slider-timeline-display
plan: 02
status: complete
completed: 2026-09-25
commits: [a846dce, 110656e, 7b93d07, f6dcdaa]
---

# 01-02 Summary — Auto-hide controls

**Delivered**
- `car/ControlsVisibilityController.kt`: a pure-Kotlin 3 s auto-hide state machine (`StateFlow<Boolean>`), with 5 virtual-time tests. Added `kotlinx-coroutines-test:1.8.1` as a test dependency.
- `car/surface/SurfaceTouchEvents.kt`: a `@Singleton` SharedFlow (buffer 1, DROP_OLDEST), exposed through `AppEntryPoint`.
- `VideoSurfaceRenderer`: overrides `onClick` (`@RequiresCarApi(5)`), `onScroll` and `onFling`, each of which emits a touch event. It takes a new constructor param, which `AutoMediaSession` passes in.
- `VideoPlaybackScreen`:
  - The timeline `MessageInfo` is shown only while the controls are visible.
  - `setActionStrip` and `setMapActionStrip` are called in every state.
  - `Action.PAN` is the first map-strip action.
  - Every button resets the timer, and so do surface touches.
  - Position ticks don't invalidate while hidden; `isPlaying` changes always invalidate.
  - The stop icon is now `ic_close`.
  - `controls.cancel()` runs in the existing `onStop` observer. The Phase 4 back contract is otherwise untouched (no `OnBackPressedCallback`, no pop in `onStop`).
- Todo `switch-back-icon-from-pause-icon-to-relevant-icon` moved to `todos/done/`.

**Verification**
- Controller tests pass (5/5) in the JVM harness.
- The API claims were checked against androidx-main source:
  - `NavigationTemplate.build()` requires an ActionStrip.
  - `ActionStrip.build()` rejects an empty strip.
  - `ACTIONS_CONSTRAINTS_MAP` allows 4 actions and exempts standard actions (PAN) from the icon requirement.
  - The annotation is `androidx.car.app.annotations.RequiresCarApi`.
- Android build: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` passed on the dev PC on 2026-09-25 (28/28 unit tests).

**Deviations**
- None functional.
- The research's "Option A lambda" was replaced by the SharedFlow singleton, as the plan specified: screens have no reference to the session-owned renderer.
