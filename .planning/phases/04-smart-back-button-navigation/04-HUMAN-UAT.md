---
status: partial
phase: 04-smart-back-button-navigation
source: [04-VERIFICATION.md]
started: 2026-04-27T16:23:06Z
updated: 2026-04-27T16:23:06Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. FEAT-2-AC1 — Hardware back from video stops playback and returns to BrowseScreen
expected: Pressing hardware back while VideoPlaybackScreen is active stops media playback and returns the user to BrowseScreen (not RootScreen)
result: [pending]

### 2. FEAT-2-AC2 — Hardware back from BrowseScreen returns to RootScreen
expected: Pressing hardware back while BrowseScreen is active returns the user to RootScreen
result: [pending]

### 3. FEAT-2-AC3 — Stop button stops playback and returns to BrowseScreen
expected: Tapping the stop/pause icon in the mapStrip on VideoPlaybackScreen stops media playback and pops to BrowseScreen (not RootScreen)
result: [pending]

### 4. FEAT-2-AC4 — Back-navigation does not crash; scroll index preserved internally
expected: Tapping item #5 then pressing back does not crash the app; lastClickedIndex is retained internally (visual scroll-to-index is a known 1.7.0 limitation — no assertion on visual position)
result: [pending]

## Summary

total: 4
passed: 0
issues: 0
pending: 4
skipped: 0
blocked: 0

## Gaps
