---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: executing
last_updated: "2026-09-25T06:45:00.000Z"
progress:
  total_phases: 7
  completed_phases: 1
  total_plans: 12
  completed_plans: 10
---

# Project State — Android Auto Media Player

**Updated:** 2026-09-25
**Current phase:** 01 (Progress Slider & Timeline Display): plans 01-01 and 01-02 done; 01-03 (head-unit UAT) pending
**Open PR:** [zorge2411/Android-auto-media-player#1](https://github.com/zorge2411/Android-auto-media-player/pull/1) (branch `claude/eager-lovelace-7oubqk` → `master`)

> Plan counts in the frontmatter cover only phases planned in the current GSD format (01, 03, 04). Phases 02, 05, 06 and 07 have only the older single-file `N-PLAN.md` and are not counted until they are re-planned.

---

## Phase Status

| Phase | Feature | Plans | Code | Verification | Status |
|-------|---------|-------|------|--------------|--------|
| 01 | Progress slider & timeline | 2/3 | ✅ 01-01, 01-02 | 14 JVM unit tests green; **Android build not yet run**; head-unit UAT pending (01-03) | In progress (PR #1) |
| 02 | Stop other audio | legacy `2-PLAN.md` | Media3 `handleAudioFocus = true` | Not verified on device | Needs GSD re-plan / UAT |
| 03 | Aspect ratio (GL pipeline) | 6/7 | ✅ 03-01 … 03-06 | 03-07 head-unit verification pending | In progress |
| 04 | Smart back button | 2/2 | ✅ | `04-HUMAN-UAT.md`: 0/4 tested | Code complete, UAT pending |
| 05 | Playback resume | legacy `5-PLAN.md` | PlaybackRepository + 10 s periodic save + restore | Not verified on device | Needs GSD re-plan / UAT |
| 06 | Playlists | legacy `6-PLAN.md` | PlaylistRepository + model only | — | **Playlist UI (screens) not built** |
| 07 | Favorites | legacy `7-PLAN.md` | FavoriteRepository, FavoritesScreen, heart toggle | Not verified on device | Needs GSD re-plan / UAT |

---

## Phase 01: What Changed (2026-09-25)

**Planning corrections** (`01-RESEARCH-ADDENDUM.md`, checked against androidx-main source):
- `NavigationTemplate.Builder.build()` throws without an ActionStrip, and an empty ActionStrip also throws. Hidden state = omit the timeline `MessageInfo` only.
- `Action.PAN` in the map strip is required to receive `SurfaceCallback` touch events. Touchscreens don't display it.
- `SurfaceCallback.onClick` is `@RequiresCarApi(5)` (not experimental). `onScroll`/`onFling` are the level-2 fallback.
- `positionMs` never updated during steady playback, so the timeline was frozen.
- The host already conceals action strips when idle.

**01-01 Live timeline:**
- 1 Hz position ticker in `MediaPlayerManager`.
- `TimeFormatter` no longer wraps hours at 24 and is locale-safe; adds `formatTimeline()`.
- `TimeFormatterTest` (9 tests).

**01-02 Auto-hide controls:**
- `ControlsVisibilityController` (3 s timer, 5 tests).
- `SurfaceTouchEvents` singleton bridge; the renderer forwards `onClick`/`onScroll`/`onFling` into it.
- `VideoPlaybackScreen`:
  - timeline auto-hides, and any touch or button press reveals it;
  - PAN added to the map strip;
  - no invalidates for position ticks while hidden;
  - fixed a stale play/pause icon;
  - stop icon changed to `ic_close`.

**Not yet verified:**
- An Android compile (`./gradlew :app:assembleDebug :app:testDebugUnitTest`). The cloud session could not download the Android SDK.
- Behaviour on a head unit (plan 01-03).

---

## Next Steps

1. **Local build:** run `./gradlew :app:assembleDebug :app:testDebugUnitTest` on the PR branch and report any errors on PR #1.
2. **One car session covering all pending human verification:**
   - 01-03: 12-row matrix. Record the Car API level from the Settings "Last AA Connection" card; tap-to-reveal needs ≥ 5.
   - 03-07: aspect-ratio matrix (16:9 / 4:3 / 21:9 / 9:16, PLAY #2+).
   - 04-HUMAN-UAT: 4 back-navigation checks.
   - The 3 debug sessions awaiting verification (below).
3. Merge PR #1 once the build and UAT pass. Close Phase 01, or run `--gaps` for any failed rows.
4. Re-plan the legacy phases in GSD format, starting with 06 (playlists: the UI is missing).

---

## Open Debug Sessions (`.planning/debug/`)

| Session | Status |
|---------|--------|
| aapt2-android35-corrupt | fixing |
| audiotrack-init-einval-on-aa-playback | awaiting human verify |
| settings-activity-no-matching-component | awaiting human verify |
| video-playback-degradation-surface-width-minus-one | awaiting human verify |

## Pending Todos (`.planning/todos/pending/`)

- `2026-04-27-add-search-slider-on-video-playback-screen`: addressed by Phase 01 as ±10 s step-seek, because Car App Library has no slider widget. Possible follow-up: tap-to-seek on a GL-drawn progress bar (needs Car API ≥ 5). Close or convert once 01-03 passes.

Done: `switch-back-icon-from-pause-icon-to-relevant-icon` (Phase 01-02).

---

## Build Notes

- AGP 8.7.3, Gradle 8.10.2 wrapper, compileSdk 35, minSdk 29.
- Kotlin 1.9.24 (root `build.gradle.kts`). `kotlin-test-junit` takes its version from the plugin.
- The repo ships only `gradlew.bat` (no Unix `gradlew`).
- The JDK is not pinned in the repo: Android Studio uses its Gradle JDK setting, and the CLI uses `JAVA_HOME` (or `org.gradle.java.home` in `~/.gradle/gradle.properties`).
- Unit tests: `./gradlew :app:testDebugUnitTest` (AspectRatioCalculatorTest, TimeFormatterTest, ControlsVisibilityControllerTest).

## Known Limitations

| Issue | Impact |
|-------|--------|
| No slider/SeekBar in Car App Library | Seeking is ±10 s step buttons |
| `onClick` only on Car API ≥ 5 hosts, and "may not be called in some car systems" | Tap-to-reveal may be unavailable; drag (`onScroll`) and button presses still reveal |
| Host decides when action strips hide | App only controls the timeline's 3 s hide |
| GridTemplate (1.7.0) has no scroll-to-index | Browse scroll restore is best-effort (Phase 04) |
| Resume positions are local only | No Plex/Jellyfin server sync |
