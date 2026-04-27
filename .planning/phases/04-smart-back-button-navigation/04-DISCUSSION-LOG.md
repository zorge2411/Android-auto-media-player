# Phase 4: Smart Back Button Navigation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-27
**Phase:** 04-smart-back-button-navigation
**Areas discussed:** Back-from-video behavior, Scroll position restoration, Long-press / stop-and-exit UX, NavigationStack fate

---

## Back-from-Video Behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Stop playback, return to Browse | Consistent with existing stop button. Clean state — no background audio surprises. | ✓ |
| Pause playback, return to Browse | Video pauses; user can return to resume. Requires keeping screen alive. | |
| Continue playing, return to Browse | Audio continues in background while browsing. Odd for a video player. | |

**User's choice:** Stop playback, return to Browse

---

| Option | Description | Selected |
|--------|-------------|----------|
| Let Car App Library handle it | Screen's onStop lifecycle already calls playerManager.stop(). No extra wiring needed. | ✓ |
| Explicitly override back handling | Add onBackPressed() override for explicitness, even if identical behavior. | |

**User's choice:** Let Car App Library handle it (rely on existing lifecycle observer)

---

## Scroll Position Restoration

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — restore position | Best UX for large libraries. Requires tracking scroll offset. | ✓ |
| No — reset to top | Simpler. Acceptable for small libraries. | |

**User's choice:** Yes — restore scroll position

---

| Option | Description | Selected |
|--------|-------------|----------|
| Track last-clicked item index | Approximation using last-tapped index. GridTemplate.Builder initial focus workaround. | ✓ |
| Track by item ID | More robust if list order changes, but requires list re-scan. | |

**User's choice:** Track last-clicked item index

---

## Long-press / Stop-and-Exit UX

| Option | Description | Selected |
|--------|-------------|----------|
| Existing stop button is sufficient | Rename to "Stop & Exit". No new mechanism needed. | ✓ |
| Add screenManager.popToRoot() on stop | Pop all the way to RootScreen. More "exit" feeling but takes user far back. | |
| Add a separate dedicated exit button | Keep stop for back-to-browse, add exit-to-root separately. Uses up mapStrip slots. | |

**User's choice:** Existing stop button is sufficient

---

| Option | Description | Selected |
|--------|-------------|----------|
| Pop to Browse (one level back) | Returns to last browse context. Natural "back from video". | ✓ |
| Pop to Root (screenManager.popToRoot) | Full exit. Good for switching source. | |

**User's choice:** Pop to Browse (one level back) — existing behavior

---

## NavigationStack Fate

| Option | Description | Selected |
|--------|-------------|----------|
| Repurpose for scroll state tracking only | Keep NavigationStack for metadata; screenManager stays as real back stack. | ✓ |
| Delete it entirely | Store scroll position as BrowseScreen constructor param. Clean, no utility class. | |
| Wire it as the real navigation driver | Replace screenManager-driven navigation. More work, fights Car App Library lifecycle. | |

**User's choice:** Repurpose for scroll state tracking only

---

| Option | Description | Selected |
|--------|-------------|----------|
| Pass scroll index as constructor parameter | Simple: push BrowseScreen with initial index. No shared state needed. | ✓ |
| Access AutoMediaSession.navigationStack via carContext | Shared reference, more coupling, centralizes state. | |
| Inject NavigationStack via Hilt | Clean DI but adds Hilt scope complexity. | |

**User's choice:** Pass scroll index as constructor parameter

---

## Claude's Discretion

- Icon/label for the stop button
- Exact GridTemplate API for initial scroll focus
- Whether to simplify NavigationStack after repurposing

## Deferred Ideas

None.
