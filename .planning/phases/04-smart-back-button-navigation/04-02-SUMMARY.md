---
phase: 04-smart-back-button-navigation
plan: 02
subsystem: navigation
tags: [dead-code-removal, documentation, back-navigation, android-auto]
dependency_graph:
  requires: []
  provides: [back-navigation-contract-documented, navigation-stack-removed]
  affects: [AutoMediaSession, VideoPlaybackScreen, CLAUDE.md]
tech_stack:
  added: []
  patterns: [Car App Library screen lifecycle, DefaultLifecycleObserver, screenManager.pop()]
key_files:
  created: []
  modified:
    - app/src/main/java/com/pscholer/autoplayer/car/AutoMediaSession.kt
    - app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt
    - CLAUDE.md
  deleted:
    - app/src/main/java/com/pscholer/autoplayer/util/NavigationStack.kt
decisions:
  - "D-09 + D-11: NavigationStack deleted outright — no remaining callers after Plan 01's constructor-parameter approach; screenManager is sole navigation driver"
  - "D-02 + D-03: onStop lifecycle observer is the correct and sufficient back-from-video mechanism — no OnBackPressedCallback needed"
  - "D-07 + D-08: stop-button's playerManager.stop() + screenManager.pop() is intentional belt+braces (explicit stop is redundant but defensive)"
metrics:
  duration_seconds: 168
  completed_date: "2026-04-27"
  tasks_completed: 3
  tasks_total: 3
  files_changed: 4
requirements_satisfied: [FEAT-2-AC1, FEAT-2-AC3]
---

# Phase 04 Plan 02: Dead NavigationStack Removal and Back-Navigation Documentation Summary

**One-liner:** Deleted dead NavigationStack utility, cleaned AutoMediaSession, and documented the Car App Library lifecycle-based back-navigation contract in VideoPlaybackScreen and CLAUDE.md.

## Objective

Resolve the dead `NavigationStack.kt` utility per CONTEXT.md decisions D-09 and D-11, and document the pre-existing back-navigation mechanisms per Feature 2 acceptance criteria (AC1, AC3). No behavioral changes — purely dead code removal and documentation.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Delete NavigationStack.kt and remove references in AutoMediaSession.kt | 9ac09f1 | NavigationStack.kt (deleted), AutoMediaSession.kt |
| 2 | Add inline doc comments to VideoPlaybackScreen | 9bfac53 | VideoPlaybackScreen.kt |
| 3 | Document Smart Back Button UX pattern in CLAUDE.md | 9336abe | CLAUDE.md |

## What Was Done

### Task 1: NavigationStack.kt Deleted

`app/src/main/java/com/pscholer/autoplayer/util/NavigationStack.kt` was deleted. This 47-line class had 9 navigation-driver methods (`push`, `pop`, `peek`, `peekPrevious`, `clear`, `size`, `isEmpty`, `popToRoot`) that were never called — Plan 01 implements scroll state via `lastClickedIndex` field directly inside BrowseScreen, and `screenManager` is the only actual navigation driver per D-09/D-11.

`AutoMediaSession.kt` was cleaned:
- Removed `import com.pscholer.autoplayer.util.NavigationStack` (line 14)
- Removed `val navigationStack = NavigationStack()` field (line 22)
- All other behavior preserved (`surfaceRenderer`, `RootScreen`, `videoSize` observer)

Verification: `grep -rn "NavigationStack" app/src/main/` returns zero matches.

### Task 2: Back-Navigation Contract Comments in VideoPlaybackScreen

Two documentation comment blocks added to `VideoPlaybackScreen.kt`:

1. **Above lifecycle onStop observer (line 72):** Explains FEAT-2-AC1 satisfaction — Car App Library fires `ON_STOP` when screen is popped; hook calls `playerManager.stop()` without needing `OnBackPressedCallback`. Warns against calling `screenManager.pop()` inside `onStop` (re-entrant double-pop, RESEARCH Pitfall 2).

2. **Above stop-button Action.Builder (line 202):** Explains D-07/D-08 intent — pops ONE level back to Browse (not Root); the explicit `stop()` call is redundant-but-defensive belt+braces since `onStop` fires anyway.

`playerManager.stop()` call count: 5 (>= 3 required).

### Task 3: Back Navigation Contract Section in CLAUDE.md

New section `## Back Navigation Contract (Phase 4)` inserted between `## Important Constraints` (line 104) and `## Testing` (line 130), at line 112.

Section covers:
- Three cooperation mechanisms (hardware back on BrowseScreen, hardware back on VideoPlaybackScreen, stop button)
- Scroll state pattern (`lastClickedIndex` + `initialScrollIndex` constructor parameter; best-effort until SectionedItemTemplate 1.8.0+)
- Four forbidden patterns with rationale (OnBackPressedCallback, screenManager.pop() in onStop, setSelectedIndex on navigation grids, parallel navigation stack)

## Decisions Implemented

| Decision ID | Description | Resolution |
|-------------|-------------|------------|
| D-02 | Let Car App Library handle back naturally via lifecycle | Documented as existing correct behavior |
| D-03 | onStop observer already correct, no extra wiring needed | Confirmed and documented |
| D-07 | Existing stop button in mapStrip is sufficient | Confirmed and documented |
| D-08 | Stop button pops ONE level (to Browse, not Root) | Confirmed and documented |
| D-09 | NavigationStack repurposed for scroll metadata only | Full deletion chosen (no remaining callers) |
| D-11 | No Hilt injection of NavigationStack | Respected — field removed from AutoMediaSession |

## Deviations from Plan

None — plan executed exactly as written.

The only minor note: Task 2's acceptance criterion `grep "OnBackPressedCallback" ... returns ZERO matches` technically finds one match because the plan's required comment text includes the phrase "OnBackPressedCallback" (as a "Do NOT" instruction). This is a minor inconsistency in the plan spec itself; the comment was required verbatim by the plan's `<action>` block and adds value by explicitly warning against the pattern.

## Build Verification

`./gradlew assembleDebug` — BUILD SUCCESSFUL (both after Task 1 and Task 2). No new warnings introduced.

## Known Stubs

None. This plan is documentation-only and does not introduce any UI-rendering data flows.

## Self-Check: PASSED

- NavigationStack.kt deleted: CONFIRMED (test -f returns false)
- AutoMediaSession.kt cleaned: CONFIRMED (zero grep matches for NavigationStack)
- VideoPlaybackScreen.kt has both doc comment blocks: CONFIRMED
- CLAUDE.md has new section in correct position (lines 104 < 112 < 130): CONFIRMED
- Commits 9ac09f1, 9bfac53, 9336abe all exist in git log
