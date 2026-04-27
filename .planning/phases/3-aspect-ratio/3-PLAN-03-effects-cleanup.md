---
phase: 3-aspect-ratio
plan: 03
type: execute
wave: 0
depends_on: []
files_modified:
  - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
autonomous: true
requirements: []
must_haves:
  truths:
    - "MediaPlayerManager no longer contains the dead ENABLE_PRESENTATION_EFFECTS const or any branch gated by it"
    - "MediaPlayerManager no longer contains APPLY_EFFECTS_BEFORE_SURFACE, attachSurfaceAndEffects(), or applyPresentationEffect()"
    - "Surface attachment goes directly through player.setVideoSurface(surface) (no helper indirection)"
    - "Qualcomm 2s teardown timeout suppression code remains intact (lastSurfaceTeardownMs path)"
    - "Surface-deferred-prepare invariant (pendingPlay path) remains intact"
    - "ScalingMode enum remains (Wave 3 reuses it)"
  artifacts:
    - path: "app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt"
      provides: "Cleaned ExoPlayer wrapper without dead Media3 effects code"
  key_links:
    - from: "MediaPlayerManager.setVideoSurface"
      to: "ExoPlayer player.setVideoSurface"
      via: "Direct call (no attachSurfaceAndEffects intermediary)"
      pattern: "player\\.setVideoSurface\\(surface\\)"
---

<objective>
Wave 0c: Delete the dead Media3 `Presentation` effects scaffolding from `MediaPlayerManager.kt`. RESEARCH.md §"Runtime State Inventory → Code-level state to remove" lists exactly what must go. This is independent prep — no Wave 2/3 dependency, parallelizable with Wave 0a (JUnit) and 0b (spike).

Purpose: The `ENABLE_PRESENTATION_EFFECTS` const is permanently `false` (effects empirically broken on AA). Every code branch it gates is unreachable. Leaving them clutters the diff for Wave 3 wiring AND invites accidental re-enable. Delete now while the file is still in the well-understood pre-Option-B shape.

Output:
- `MediaPlayerManager.kt` with all dead-effects code removed.
- All surface-lifecycle invariants preserved (Qualcomm 2s suppression, deferred-prepare, ScalingMode enum).
- File compiles without warnings about unused symbols.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/3-aspect-ratio/3-RESEARCH.md
@CLAUDE.md
@app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt
</context>

<tasks>

<task type="auto">
  <name>Task 1: Remove ENABLE_PRESENTATION_EFFECTS / APPLY_EFFECTS_BEFORE_SURFACE consts and the verbose comment block</name>
  <files>app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt</files>
  <read_first>
    - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt (entire file — ~679 lines)
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (§"Runtime State Inventory → Code-level state to remove" — confirms exact deletion list)
  </read_first>
  <action>
    Open `app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt`.

    Inside the `companion object` (lines ~46-68), DELETE these exact items:
    - The entire comment block from `// Media3's GL effects pipeline (setVideoEffects)...` down through `//   adb shell setprop log.tag.Effect VERBOSE` (lines ~50-60 in current file).
    - The line `private const val ENABLE_PRESENTATION_EFFECTS = false`
    - The comment `// Experiment: swap the attach order so setVideoEffects() runs BEFORE setVideoSurface(). Only meaningful when ENABLE_PRESENTATION_EFFECTS is true.`
    - The line `private const val APPLY_EFFECTS_BEFORE_SURFACE = false`

    KEEP:
    - `private const val TAG = "MediaPlayerManager"`
    - `private const val PLAYBACK_SAVE_INTERVAL_MS = 10_000L`
    - The `TEARDOWN_TIMEOUT_WINDOW_MS` const (Qualcomm suppression — defense in depth per RESEARCH.md §"Pitfall 6")
    - The comment explaining the timeout window

    Add a single replacement comment in the companion object explaining what was removed and why:
    ```kotlin
            // Media3 Presentation effects were removed in Phase 3 Wave 0 — empirically broken
            // on Android Auto's remote Surface (setFrameRate -38 errno in DefaultVideoFrameProcessor).
            // Replaced by GLVideoPipeline (custom EGL14 + OES intermediary). See 3-RESEARCH.md.
    ```

    Also remove the now-unused import:
    ```kotlin
    import androidx.media3.effect.Presentation
    ```
    (line ~17 in current file).
  </action>
  <verify>
    <automated>! grep -q "ENABLE_PRESENTATION_EFFECTS" app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt && ! grep -q "APPLY_EFFECTS_BEFORE_SURFACE" app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt && ! grep -q "import androidx.media3.effect.Presentation" app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt</automated>
  </verify>
  <acceptance_criteria>
    - `MediaPlayerManager.kt` does NOT contain `ENABLE_PRESENTATION_EFFECTS`
    - `MediaPlayerManager.kt` does NOT contain `APPLY_EFFECTS_BEFORE_SURFACE`
    - `MediaPlayerManager.kt` does NOT contain `import androidx.media3.effect.Presentation`
    - `MediaPlayerManager.kt` STILL contains `TEARDOWN_TIMEOUT_WINDOW_MS` (Qualcomm suppression intact)
    - `MediaPlayerManager.kt` STILL contains `PLAYBACK_SAVE_INTERVAL_MS`
    - `MediaPlayerManager.kt` contains a comment referencing `GLVideoPipeline` or `Phase 3 Wave 0`
  </acceptance_criteria>
  <done>Constants and the Presentation import are gone; replacement comment in place.</done>
</task>

<task type="auto">
  <name>Task 2: Delete attachSurfaceAndEffects() and applyPresentationEffect(); inline direct setVideoSurface() at call sites</name>
  <files>app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt</files>
  <read_first>
    - app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt (post Task 1, focus on lines ~270-475 where the helpers and their call sites live)
    - .planning/phases/3-aspect-ratio/3-RESEARCH.md (§"Pattern 6: Teardown Order" — confirms direct setVideoSurface(null) is fine)
  </read_first>
  <action>
    In `MediaPlayerManager.kt`:

    **Delete the entire `attachSurfaceAndEffects(surface: Surface)` private function** (currently around lines 424-436). Replace each of its three call sites with a direct `player.setVideoSurface(surface)`:

    1. In `setVideoSurface(surface: Surface)` (around line 298): replace `attachSurfaceAndEffects(surface)` with `player.setVideoSurface(surface)`.
    2. In `playWithHeaders(...)` (around line 541): replace `attachSurfaceAndEffects(surface)` with `player.setVideoSurface(surface)`.
    3. In `playMediaItem(...)` (around line 593): replace `attachSurfaceAndEffects(surface)` with `player.setVideoSurface(surface)`.

    **Delete the entire `applyPresentationEffect()` private function** (currently around lines 438-474).

    **In `clearVideoSurface()`** (around lines 330-351): the `if (ENABLE_PRESENTATION_EFFECTS) { player.clearVideoSurface() }` block (around lines 338-342) should be DELETED entirely — the const is gone and the branch was unreachable. Per RESEARCH.md §"Pitfall 6", we now rely on `player.stop()` alone to detach (which is already on the next line) plus the existing `lastSurfaceTeardownMs` suppression.

    **In `setOutputSize()`** (around lines 365-387): delete the trailing `if (ENABLE_PRESENTATION_EFFECTS && activeSurface != null && player.playbackState != Player.STATE_IDLE) { applyPresentationEffect() }` block (around lines 380-386). It is unreachable. Keep the size-update logic above it intact.

    **In `setScalingMode()`** (around lines 390-422): currently this function references `ENABLE_PRESENTATION_EFFECTS` and `applyPresentationEffect()`. Replace its body with a placeholder that just stores the new mode value and logs — the actual GL pipeline will be wired in Wave 3 (Plan 06). New body:
    ```kotlin
        fun setScalingMode(mode: ScalingMode) {
            if (_scalingMode.value == mode) return
            _scalingMode.value = mode
            Log.i(TAG, "Scaling mode set to $mode (no-op until Wave 3 GLVideoPipeline wires it)")
        }
    ```

    **KEEP UNCHANGED:**
    - `enum class ScalingMode { FIT, FILL, STRETCH }` (Wave 3 reuses it)
    - `_scalingMode` MutableStateFlow + `scalingMode` StateFlow exposure
    - All Qualcomm `lastSurfaceTeardownMs` logic in `onPlayerError`
    - All `pendingPlay` / `pendingSurface` / surface-deferred-prepare logic in `setVideoSurface`, `play`, `playWithHeaders`, `playMediaItem`
    - The `surfaceWidth` / `surfaceHeight` fields and `setOutputSize` size-update logic (Wave 3 GLVideoPipeline reads these)

    Update the docstring on `clearVideoSurface()` to remove the `ENABLE_PRESENTATION_EFFECTS` reference and the obsolete `player.clearVideoSurface()` discussion. Replace with: "Calls `player.stop()` to release the codec; the Qualcomm 2s cleanup hang is handled by `lastSurfaceTeardownMs` suppression in `onPlayerError`. The next `setVideoSurface()` call will attach a new surface from IDLE."
  </action>
  <verify>
    <automated>! grep -q "attachSurfaceAndEffects" app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt && ! grep -q "applyPresentationEffect" app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt && grep -q "player.setVideoSurface(surface)" app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt && grep -q "TEARDOWN_TIMEOUT_WINDOW_MS" app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt && grep -q "enum class ScalingMode" app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt && ./gradlew :app:compileDebugKotlin --quiet</automated>
  </verify>
  <acceptance_criteria>
    - `MediaPlayerManager.kt` does NOT contain `attachSurfaceAndEffects` (def or call site)
    - `MediaPlayerManager.kt` does NOT contain `applyPresentationEffect` (def or call site)
    - `MediaPlayerManager.kt` does NOT contain `player.setVideoEffects(` (Presentation usage gone)
    - `MediaPlayerManager.kt` contains `player.setVideoSurface(surface)` at least 3 times (3 inlined call sites)
    - `MediaPlayerManager.kt` STILL contains `TEARDOWN_TIMEOUT_WINDOW_MS` and `lastSurfaceTeardownMs`
    - `MediaPlayerManager.kt` STILL contains `pendingPlay` and `pendingSurface` (deferred-prepare path intact)
    - `MediaPlayerManager.kt` STILL contains `enum class ScalingMode { FIT, FILL, STRETCH }`
    - `MediaPlayerManager.kt` STILL contains `setScalingMode(mode: ScalingMode)` function (now a stub)
    - `./gradlew :app:assembleDebug` exits 0
  </acceptance_criteria>
  <done>All dead Presentation-effects code removed; surface-lifecycle invariants preserved; app compiles.</done>
</task>

</tasks>

<verification>
- All dead `ENABLE_PRESENTATION_EFFECTS` / `APPLY_EFFECTS_BEFORE_SURFACE` / `attachSurfaceAndEffects` / `applyPresentationEffect` symbols are gone.
- Direct `player.setVideoSurface(surface)` calls are present at every former indirection site.
- `clearVideoSurface()` no longer calls `player.clearVideoSurface()` (RESEARCH.md Pitfall 6 mitigation).
- Qualcomm 2s suppression in `onPlayerError` is unchanged.
- `pendingPlay` / `pendingSurface` deferred-prepare path is unchanged.
- `enum class ScalingMode` and `setScalingMode()` are still present (stubbed for Wave 3).
- `./gradlew :app:assembleDebug` exits 0.
</verification>

<success_criteria>
- File is leaner; no unreachable branches.
- Wave 3 (Plan 06) will integrate `GLVideoPipeline` against this clean baseline.
- No regressions in surface attach / play / teardown behavior (manual smoke test reveals no new issues).
</success_criteria>

<output>
Create `.planning/phases/3-aspect-ratio/3-03-SUMMARY.md` summarizing:
- Lines deleted (rough count) and confirming each deleted symbol
- That `lastSurfaceTeardownMs` and `pendingPlay` paths are intact
- That `ScalingMode` enum and `setScalingMode` stub remain for Wave 3
</output>
