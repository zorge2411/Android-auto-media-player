---
phase: 01-progress-slider-timeline-display
plan: 02
type: execute
wave: 1
depends_on: []
files_modified:
  - app/build.gradle.kts
  - app/src/main/java/com/pscholer/autoplayer/car/ControlsVisibilityController.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/SurfaceTouchEvents.kt
  - app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt
  - app/src/main/java/com/pscholer/autoplayer/car/AutoMediaSession.kt
  - app/src/main/java/com/pscholer/autoplayer/di/AppEntryPoint.kt
  - app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt
  - app/src/test/java/com/pscholer/autoplayer/car/ControlsVisibilityControllerTest.kt
autonomous: true
requirements: [FEAT-1-AC1, FEAT-1-AC2, FEAT-1-AC4, UI-SPEC-AUTOHIDE, UI-SPEC-REVEAL]
must_haves:
  truths:
    - "The timeline (MessageInfo) disappears 3 s after the last user interaction and comes back on a surface tap, a surface drag, or any strip button press"
    - "NavigationTemplate is ALWAYS built with an ActionStrip; the builder throws IllegalStateException without one, and an empty ActionStrip also throws"
    - "The map ActionStrip always contains Action.PAN; without it the host delivers NO SurfaceCallback touch events (onClick, onScroll, onFling)"
    - "Action strips are hidden by the host itself when idle (Android for Cars 'interact with your map' guide); the app does not try to remove them"
    - "Position ticks never reset the hide timer, and invalidate() is not called for position ticks while the timeline is hidden"
    - "The play/pause icon refreshes on every isPlaying change, even within the same formatted second"
    - "Timer logic is a pure-Kotlin class covered by JVM tests using virtual time"
  artifacts:
    - path: "app/src/main/java/com/pscholer/autoplayer/car/ControlsVisibilityController.kt"
      provides: "3 s auto-hide state machine exposing StateFlow<Boolean>"
      contains: "class ControlsVisibilityController"
    - path: "app/src/main/java/com/pscholer/autoplayer/car/surface/SurfaceTouchEvents.kt"
      provides: "@Singleton SharedFlow bridge from renderer (session-owned) to screen (Hilt EntryPoint)"
      contains: "@Singleton"
    - path: "app/src/test/java/com/pscholer/autoplayer/car/ControlsVisibilityControllerTest.kt"
      provides: "runTest/advanceTimeBy tests"
      contains: "advanceTimeBy"
  key_links:
    - from: "VideoSurfaceRenderer.onClick / onScroll / onFling"
      to: "SurfaceTouchEvents.emit()"
      via: "tryEmit on a MutableSharedFlow(extraBufferCapacity = 1, DROP_OLDEST)"
      pattern: "surfaceTouchEvents"
    - from: "VideoPlaybackScreen"
      to: "ControlsVisibilityController.onUserInteraction()"
      via: "collect SurfaceTouchEvents.taps + every Action click listener"
      pattern: "onUserInteraction"
    - from: "VideoPlaybackScreen map strip"
      to: "Action.PAN"
      via: "first action in setMapActionStrip"
      pattern: "Action.PAN"
---

<objective>
Wave 1 (runs in parallel with Plan 01; the two plans change different files): implement the LOCKED auto-hide / tap-to-reveal contract from 01-UI-SPEC.md, corrected by 01-RESEARCH-ADDENDUM.md.

Three research findings are corrected by this plan (checked against androidx-main source and the current Android for Cars docs, 2026-09-25):
1. The research's hidden state `NavigationTemplate.Builder().build()` **crashes**: `build()` throws `IllegalStateException("Action strip for this template must be set")`. `ActionStrip.Builder().build()` with no actions also throws.
2. The research said "Do NOT add a PAN button". That is wrong for this feature. The docs say an app that omits `Action.PAN` from the map action strip "doesn't receive user input from the SurfaceCallback methods". On touchscreens the host does not display the PAN button, so it takes no visible space.
3. `SurfaceCallback.onClick` is `@RequiresCarApi(5)` and is **not** `@ExperimentalCarApi`. No opt-in annotation is needed. It is a default interface method, so it is simply never called on older hosts.

Resulting design:
- **Hidden state** = template WITHOUT `setNavigationInfo(...)`. It still has the ActionStrip and the map strip, because the ActionStrip is required and PAN is needed for touch.
- The host conceals both strips when idle. The app only controls the timeline.

Also closes pending todo `2026-04-27-switch-back-icon-from-pause-icon-to-relevant-icon` (stop button: `ic_pause` → existing `ic_close`), since this plan already rewrites that map strip.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/01-progress-slider-timeline-display/01-UI-SPEC.md
@.planning/phases/01-progress-slider-timeline-display/01-RESEARCH.md
@.planning/phases/01-progress-slider-timeline-display/01-RESEARCH-ADDENDUM.md
@CLAUDE.md  (Back Navigation Contract — must not be disturbed)
@app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt
@app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt
@app/src/main/java/com/pscholer/autoplayer/car/AutoMediaSession.kt
@app/src/main/java/com/pscholer/autoplayer/di/AppEntryPoint.kt
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: ControlsVisibilityController + tests</name>
  <files>
    app/build.gradle.kts
    app/src/main/java/com/pscholer/autoplayer/car/ControlsVisibilityController.kt
    app/src/test/java/com/pscholer/autoplayer/car/ControlsVisibilityControllerTest.kt
  </files>
  <behavior>
    API:
    ```kotlin
    class ControlsVisibilityController(
        private val scope: CoroutineScope,
        private val hideAfterMs: Long = 3_000L
    ) {
        val visible: StateFlow<Boolean>          // starts true
        fun onUserInteraction()                  // visible = true, restart the hide timer
        fun cancel()                             // cancel the timer; state stays as it is
    }
    ```
    Tests (runTest + a StandardTestDispatcher-backed scope, advanceTimeBy / runCurrent):
    1. Starts visible. After `onUserInteraction()` + 2_999 ms it is still visible. At 3_000 ms it is hidden.
    2. A second interaction at 2_000 ms restarts the timer, so it is still visible at 4_999 ms and hidden at 5_000 ms.
    3. An interaction while hidden → visible immediately (after runCurrent), and hidden again 3 s later.
    4. `cancel()` stops a pending hide (still visible long after 3 s).
    5. `visible` emits distinct values only (repeated interactions while visible cause no extra emission). Collect into a list to assert this.
  </behavior>
  <action>
    - Add `testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")` under the Testing block. It must match the coroutines-core version (1.8.1).
    - Write the tests first (RED), then the implementation (GREEN): a `MutableStateFlow(true)` plus a `Job?` that is cancelled and relaunched with `delay(hideAfterMs)` on each interaction.
    - No Android imports in the controller.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "*ControlsVisibilityControllerTest*"</automated>
  </verify>
  <done>Commits `test(01-02): ...` then `feat(01-02): ControlsVisibilityController`.</done>
</task>

<task type="auto">
  <name>Task 2: Surface touch bridge (SurfaceTouchEvents + renderer overrides)</name>
  <files>
    app/src/main/java/com/pscholer/autoplayer/car/surface/SurfaceTouchEvents.kt
    app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt
    app/src/main/java/com/pscholer/autoplayer/car/AutoMediaSession.kt
    app/src/main/java/com/pscholer/autoplayer/di/AppEntryPoint.kt
  </files>
  <read_first>
    - AutoMediaSession.kt: the renderer is a private lateinit created per session, and screens are built by BrowseScreen/FavoritesScreen with only (carContext, item). So a direct lambda (research Option A) has no clean path to reach the screen. Use a Hilt singleton instead.
  </read_first>
  <action>
    - `@Singleton class SurfaceTouchEvents @Inject constructor()` with
      `private val _taps = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)`,
      `val taps: SharedFlow<Unit>`, and `fun emit() { _taps.tryEmit(Unit) }`.
    - Add `fun surfaceTouchEvents(): SurfaceTouchEvents` to `AppEntryPoint`.
    - `VideoSurfaceRenderer` gets a third constructor param `private val touchEvents: SurfaceTouchEvents`. `AutoMediaSession` passes `entryPoint.surfaceTouchEvents()`.
    - Override in the renderer:
      - `onClick(x, y)`, annotated `@RequiresCarApi(5)`; no ExperimentalCarApi opt-in needed.
      - `onScroll(dx, dy)` and `onFling(vx, vy)` (level 2).
      Each one logs at `Log.v` and calls `touchEvents.emit()`. Do not add `onScale`; pinch has no meaning here.
    - Do not change any surface/GL attach logic.
  </action>
  <verify>
    <automated>./gradlew :app:assembleDebug && grep -q "override fun onClick" app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt</automated>
  </verify>
  <done>Commit `feat(01-02): forward surface touch events via SurfaceTouchEvents`.</done>
</task>

<task type="auto">
  <name>Task 3: Wire auto-hide into VideoPlaybackScreen</name>
  <files>app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt</files>
  <action>
    - Create `private val controls = ControlsVisibilityController(lifecycleScope)`.
    - Add `private var controlsVisible = true`.
    - In `init`:
      - collect `controls.visible`: set `controlsVisible`, then `invalidate()`;
      - collect `entryPoint.surfaceTouchEvents().taps` → `controls.onUserInteraction()`;
      - call `controls.onUserInteraction()` once, right after playback is started (this starts the initial 3 s timer).
    - Rewrite the position `combine` loop:
      - track `lastIsPlaying`;
      - always update `currentPositionMs` and `currentDurationMs`;
      - call `invalidate()` only when (a) `isPlaying` changed (this fixes the stale play/pause icon), OR (b) `controlsVisible` is true AND the formatted second or the duration changed.
    - `buildTimelineText()` → `TimeFormatter.formatTimeline(currentPositionMs, currentDurationMs)`. This comes from Plan 01. If Plan 01 has not landed yet, inline the same string and leave a `// TODO(01-01)` note; the orchestrator merges both before Plan 03.
    - `onGetTemplate()`:
      - `NavigationTemplate.Builder().setActionStrip(playbackStrip).setMapActionStrip(mapStrip)`
      - add `.setNavigationInfo(MessageInfo.Builder(timeline).build())` ONLY when `controlsVisible`.
      - NEVER build without an ActionStrip.
    - Map strip, in order:
      1. `Action.PAN`
      2. favorite toggle
      3. stop, now using `R.drawable.ic_close`
      That is 3 of the 4 allowed actions.
    - Every click listener (seek back, play/pause, seek forward, favorite, stop) also calls `controls.onUserInteraction()` before its action. Stop keeps its existing `playerManager.stop()` + `screenManager.pop()` and nothing else.
    - Keep the Phase 4 contract intact:
      - no `OnBackPressedCallback`;
      - no `pop()` in `onStop`;
      - the existing lifecycle observer stays.
      Add `controls.cancel()` to that observer's `onStop` (safe, idempotent).
    - Update the ASCII diagram in the class KDoc: the stop icon, PAN, and the auto-hide note.
    - Move pending todo `2026-04-27-switch-back-icon-from-pause-icon-to-relevant-icon.md` to `.planning/todos/done/`.
  </action>
  <verify>
    <automated>./gradlew :app:assembleDebug :app:testDebugUnitTest && grep -q "Action.PAN" app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt && ! grep -q "NavigationTemplate.Builder().build()" app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt</automated>
  </verify>
  <acceptance_criteria>
    - Every branch of `onGetTemplate()` calls `setActionStrip(...)`.
    - The map strip contains `Action.PAN` in every branch.
    - `grep -n "OnBackPressedCallback" VideoPlaybackScreen.kt` returns nothing.
    - No `invalidate()` for position ticks while hidden (by code inspection).
  </acceptance_criteria>
  <done>Commit `feat(01-02): auto-hide timeline with tap-to-reveal; stop icon → ic_close`.</done>
</task>

</tasks>

<verification>
- `./gradlew :app:assembleDebug :app:testDebugUnitTest` is green.
- Adversarial re-read: search the diff for any `NavigationTemplate.Builder()` chain that can reach `.build()` without `setActionStrip`.
</verification>
