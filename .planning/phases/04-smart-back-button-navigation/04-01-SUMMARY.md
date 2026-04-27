---
phase: 04-smart-back-button-navigation
plan: 01
subsystem: ui
tags: [android-auto, car-app-library, browse-screen, scroll-state, grid-template]

# Dependency graph
requires: []
provides:
  - BrowseScreen constructor with initialScrollIndex parameter
  - lastClickedIndex field tracking clicked item index per BrowseScreen instance
  - Index-aware buildGridItem passing index to click handler
  - Scroll-state metadata foundation for FEAT-2-AC4

affects:
  - 04-02 (subsequent plans in this phase may build on scroll state)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Constructor-parameter scroll state: BrowseScreen receives initialScrollIndex and tracks lastClickedIndex without external state"
    - "forEachIndexed pattern: items rendered with positional index for click tracking"

key-files:
  created: []
  modified:
    - app/src/main/java/com/pscholer/autoplayer/car/screens/BrowseScreen.kt

key-decisions:
  - "D-05/D-10: Track scroll position by last-clicked item index, passed as constructor parameter initialScrollIndex: Int = 0"
  - "D-06: GridTemplate 1.7.0 has no scroll-to-index API; index stored as state metadata for best-effort UX, visual scroll restoration deferred to SectionedItemTemplate (1.8.0+)"
  - "Child BrowseScreen receives initialScrollIndex=0 (starts at top); parent retains its own lastClickedIndex independently"
  - "No setSelectedIndex or OnBackPressedCallback — Pitfall 1 and D-02 respected"

patterns-established:
  - "Pattern: BrowseScreen scroll state via constructor parameter, not DI or session-level state (D-11)"

requirements-completed:
  - FEAT-2-AC1
  - FEAT-2-AC2
  - FEAT-2-AC4

# Metrics
duration: 2min
completed: 2026-04-27
---

# Phase 04 Plan 01: Smart Back Button Navigation — Scroll Index Tracking Summary

**BrowseScreen enhanced with initialScrollIndex constructor parameter and lastClickedIndex field using forEachIndexed, enabling scroll-state metadata per D-05/D-10 (visual restoration deferred — GridTemplate 1.7.0 has no scroll-to-index API)**

## Performance

- **Duration:** 2 min
- **Started:** 2026-04-27T16:12:58Z
- **Completed:** 2026-04-27T16:14:36Z
- **Tasks:** 1 of 1
- **Files modified:** 1

## Accomplishments

- Added `initialScrollIndex: Int = 0` as fourth constructor parameter to BrowseScreen (D-10)
- Added `private var lastClickedIndex: Int = initialScrollIndex` field with decision-ID comment (D-05)
- Updated `buildGridItem` signature to `buildGridItem(item: MediaItem, index: Int)` and wired forEachIndexed call
- Capture `lastClickedIndex = index` before `screenManager.push()` in click listener
- Child BrowseScreen for sub-folder receives `initialScrollIndex=0` (fresh top-of-list start)
- No forbidden APIs introduced: no `setSelectedIndex`, `setInitialFocusedIndex`, `OnBackPressedCallback`
- `assembleDebug` BUILD SUCCESSFUL — project compiles cleanly

## Task Commits

1. **Task 1: Add initialScrollIndex parameter and lastClickedIndex tracking to BrowseScreen** - `4dff8c5` (feat)

## Files Created/Modified

- `app/src/main/java/com/pscholer/autoplayer/car/screens/BrowseScreen.kt` - Added initialScrollIndex param, lastClickedIndex field, index-aware buildGridItem, forEachIndexed iteration

## Decisions Made

- Used constructor parameter pattern (not DI, not session state) per D-11 — simpler, self-contained per screen instance
- Passed `0` as child BrowseScreen's `initialScrollIndex` when navigating into sub-folders (child starts fresh at top)
- No visual scroll restoration attempted — GridTemplate 1.7.0 has no `setInitialFocusedIndex` or `setScrollPosition` API (RESEARCH.md Pitfall 1 confirmed). This is intentionally a state-bookkeeping foundation; `lastClickedIndex` is available for future use when Car App Library 1.8.0's `SectionedItemTemplate` becomes viable.

## Deviations from Plan

None - plan executed exactly as written.

## Visual Scroll Restoration Limitation (Known)

Per RESEARCH.md Pitfall 1 and D-06: `GridTemplate` in Car App Library 1.7.0 has **no scroll-to-index API**. `setSelectedIndex()` only works on selectable lists with `OnSelectedListener`, not on click-to-navigate grids. There is no `setInitialFocusedIndex` on `GridTemplate.Builder`.

The grid will always render from the top when `onGetTemplate()` is called on resume. The `lastClickedIndex` field is stored as state metadata (satisfying FEAT-2-AC4's intent to track position), but the host does not visually scroll to it. This is documented as a known limitation — left for future work when `SectionedItemTemplate` (Car App Library 1.8.0+) is stable and the project can adopt it.

## Issues Encountered

None - build succeeded on first attempt.

## Next Phase Readiness

- Scroll-state infrastructure in place; BrowseScreen instances each independently track their last-clicked index
- AC1 (back from VideoPlaybackScreen stops playback, returns to Browse) and AC2 (back from BrowseScreen returns to RootScreen) were already satisfied by existing lifecycle observer and Action.BACK — confirmed unchanged
- AC4 scroll-state metadata foundation complete; visual restoration is best-effort per library constraint
- Ready for plan 02 if further smart-back-button work is planned (stop button labeling, VideoPlaybackScreen discretionary changes)

## Self-Check: PASSED

- FOUND: `.planning/phases/04-smart-back-button-navigation/04-01-SUMMARY.md`
- FOUND: commit `4dff8c5` feat(04-01): add scroll index tracking to BrowseScreen
- FOUND: `initialScrollIndex: Int = 0` at line 61
- FOUND: `private var lastClickedIndex` at line 66
- FOUND: `lastClickedIndex = index` at line 194
- FOUND: `buildGridItem(item: MediaItem, index: Int)` at line 178
- FOUND: `forEachIndexed` at line 168

---
*Phase: 04-smart-back-button-navigation*
*Completed: 2026-04-27*
