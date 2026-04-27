# Phase 4: Smart Back Button Navigation - Context

**Gathered:** 2026-04-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement context-aware back navigation across the 3-screen hierarchy (Root → Browse → VideoPlayback). This phase delivers: back-from-video behavior, scroll position restoration in BrowseScreen, stop-and-exit UX, and resolution of the dead NavigationStack utility.

</domain>

<decisions>
## Implementation Decisions

### Back-from-Video Behavior
- **D-01:** Back button from VideoPlaybackScreen stops playback and returns to Browse.
- **D-02:** Let Car App Library handle back naturally — the screen's existing `onStop` lifecycle observer already calls `playerManager.stop()`. No explicit `onBackPressed()` override needed.
- **D-03:** The behavior is already correct by virtue of the lifecycle observer; no extra wiring required.

### Scroll Position Restoration
- **D-04:** BrowseScreen MUST restore scroll position when the user navigates back to it.
- **D-05:** Track scroll position by last-clicked item index (not item ID). GridTemplate has no `getScrollOffset()` API — approximating via last-tapped index is the practical workaround.
- **D-06:** On back-navigation to BrowseScreen, scroll to the last-clicked index via GridTemplate's initial focus mechanism.

### Stop & Exit UX
- **D-07:** The existing stop button in `VideoPlaybackScreen`'s `mapStrip` is sufficient for stop-and-exit. No new mechanism (long-press hardware back, separate exit button) is needed.
- **D-08:** Stop button behavior: `playerManager.stop()` + `screenManager.pop()` — pops back ONE level (to Browse), NOT to Root. User returns to the browse context they came from.

### NavigationStack Fate
- **D-09:** Keep `NavigationStack.kt` but repurpose it for scroll state metadata only — NOT as the actual navigation driver. `screenManager` remains the real back stack.
- **D-10:** BrowseScreen receives its initial scroll index as a constructor parameter (`initialScrollIndex: Int = 0`). When pushing a child BrowseScreen or VideoPlaybackScreen, BrowseScreen passes back the last-clicked index so the parent can restore when popped back to.
- **D-11:** No Hilt injection of NavigationStack; no access via `AutoMediaSession`. Simple constructor parameter pattern.

### Claude's Discretion
- Icon/label for the stop button (can rename to make "stop & return" intent clearer)
- Exact GridTemplate API call to restore scroll focus (needs research — may require `GridTemplate.Builder().setInitialFocusedIndex()` or similar)
- Whether to simplify/delete unused parts of NavigationStack after repurposing

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Requirements
- `REQUIREMENTS.md` Feature 2 — Smart Back Button Navigation (acceptance criteria, constraints, out-of-scope)
- `CLAUDE.md` — Project architecture, Car App Library constraints, key class list

### Existing Navigation Code
- `app/src/main/java/com/pscholer/autoplayer/util/NavigationStack.kt` — Dead utility to repurpose
- `app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt` — Existing stop button + lifecycle observer
- `app/src/main/java/com/pscholer/autoplayer/car/screens/BrowseScreen.kt` — No scroll state tracking yet
- `app/src/main/java/com/pscholer/autoplayer/car/AutoMediaSession.kt` — Creates NavigationStack as dead field

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `NavigationStack.kt`: Has `Browse(parentId, scrollPosition)` target — `scrollPosition` field is already there but unused. Repurpose the field.
- `VideoPlaybackScreen.kt`: Stop button already implemented in `mapStrip` (`playerManager.stop()` + `screenManager.pop()`). Lifecycle observer already calls `playerManager.stop()` on `onStop`.
- `screenManager.push()` / `screenManager.pop()`: Car App Library's built-in stack — this IS the navigation mechanism.

### Established Patterns
- Screens use constructor parameters for data (e.g., `BrowseScreen(carContext, source, parentId)`). Adding `initialScrollIndex: Int = 0` follows this pattern.
- `lifecycleScope.launch` for async work within screens — used consistently.

### Integration Points
- `BrowseScreen.buildGridItem()` — track the clicked item's index when calling `screenManager.push()`.
- `BrowseScreen.onGetTemplate()` — apply initial scroll focus when `initialScrollIndex > 0`.
- `AutoMediaSession.navigationStack` field — can be removed or left as dead reference after repurposing strategy is implemented at screen level.

</code_context>

<specifics>
## Specific Ideas

- The `NavigationStack.Browse.scrollPosition` field already exists — reuse it conceptually even if the actual tracking moves to constructor parameters.
- Car App Library's `GridTemplate` may or may not support initial focus index — researcher should verify the exact API.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 04-smart-back-button-navigation*
*Context gathered: 2026-04-27*
