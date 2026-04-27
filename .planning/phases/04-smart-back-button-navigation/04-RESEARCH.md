# Phase 4: Smart Back Button Navigation - Research

**Researched:** 2026-04-27
**Domain:** Android Car App Library — Screen lifecycle, back navigation, scroll state
**Confidence:** HIGH (for lifecycle behavior); MEDIUM (for scroll restoration workaround)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Back button from VideoPlaybackScreen stops playback and returns to Browse.
- **D-02:** Let Car App Library handle back naturally — the screen's existing `onStop` lifecycle observer already calls `playerManager.stop()`. No explicit `onBackPressed()` override needed.
- **D-03:** The behavior is already correct by virtue of the lifecycle observer; no extra wiring required.
- **D-04:** BrowseScreen MUST restore scroll position when the user navigates back to it.
- **D-05:** Track scroll position by last-clicked item index (not item ID). GridTemplate has no `getScrollOffset()` API — approximating via last-tapped index is the practical workaround.
- **D-06:** On back-navigation to BrowseScreen, scroll to the last-clicked index via GridTemplate's initial focus mechanism.
- **D-07:** The existing stop button in `VideoPlaybackScreen`'s `mapStrip` is sufficient for stop-and-exit. No new mechanism needed.
- **D-08:** Stop button behavior: `playerManager.stop()` + `screenManager.pop()` — pops back ONE level (to Browse), NOT to Root.
- **D-09:** Keep `NavigationStack.kt` but repurpose it for scroll state metadata only — NOT as the actual navigation driver.
- **D-10:** BrowseScreen receives its initial scroll index as a constructor parameter (`initialScrollIndex: Int = 0`). When pushing a child BrowseScreen or VideoPlaybackScreen, BrowseScreen passes back the last-clicked index so the parent can restore when popped back to.
- **D-11:** No Hilt injection of NavigationStack; no access via `AutoMediaSession`. Simple constructor parameter pattern.

### Claude's Discretion

- Icon/label for the stop button (can rename to make "stop & return" intent clearer)
- Exact GridTemplate API call to restore scroll focus (needs research)
- Whether to simplify/delete unused parts of NavigationStack after repurposing

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| FEAT-2-AC1 | Back button on VideoPlaybackScreen routes to BrowseScreen | D-02/D-03: onStop fires on pop, existing lifecycle observer handles stop |
| FEAT-2-AC2 | Back button on BrowseScreen returns to RootScreen | Action.BACK already calls screenManager.pop() |
| FEAT-2-AC3 | Long-press/dedicated button stops playback and exits | D-07/D-08: existing stop button with screenManager.pop() |
| FEAT-2-AC4 | Previous browse state (scroll position) is preserved | D-05/D-06/D-10: index parameter + onGetTemplate() re-called on resume |
</phase_requirements>

---

## Summary

The Car App Library's `Screen` lifecycle is the definitive mechanism for this phase. When `screenManager.pop()` is called (either by the hardware back button, `Action.BACK`, or explicit code), the popped screen receives `ON_PAUSE → ON_STOP → ON_DESTROY` in sequence. This means the `DefaultLifecycleObserver.onStop()` already wired in `VideoPlaybackScreen` will fire on back-press — D-02 and D-03 are confirmed correct as-is.

When a screen is re-exposed after a screen above it is popped, the Car App host calls `onGetTemplate()` on the revealed screen again (equivalent to a template refresh). This is the hook for scroll restoration: if `BrowseScreen` stores the last-clicked index and uses it in `onGetTemplate()`, the grid will be built with that item in focus. The mechanism is constructor-parameter based (D-10), not a GridTemplate API for scroll offset, because no such API exists for GridTemplate in Car App Library 1.7.x.

`ItemList.Builder.setSelectedIndex()` is confirmed to exist but is restricted to selectable lists (those with `OnSelectedListener`). It CANNOT be used on a normal click-to-navigate grid. There is no `setInitialFocusedIndex` or scroll-offset API on `GridTemplate.Builder` in the current Car App Library (1.7.0). The correct workaround is to build the `ItemList` with the target item first by sorting or by maintaining item order and triggering `invalidate()` — or accept that "scroll restoration" means the host host will show the grid from the top and the user must scroll to the prior item. This is the practical limitation the planner must account for.

**Primary recommendation:** Implement `initialScrollIndex: Int = 0` constructor parameter on `BrowseScreen`, store the last-clicked index in a field, and call `invalidate()` after pop — the template is rebuilt but true scroll-to-index is not possible in GridTemplate without a selectable list pattern. The planner should document this as a best-effort UX, not a pixel-perfect restoration.

---

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| androidx.car.app | 1.7.0 | Car App Library screens, templates, ScreenManager | Already in project; locked version |
| androidx.car.app.model.GridTemplate | 1.7.0 | Grid UI for BrowseScreen | Already used |
| androidx.car.app.model.ItemList | 1.7.0 | Item container for GridTemplate | Already used |
| androidx.lifecycle (DefaultLifecycleObserver) | bundled with Car App | Lifecycle hooks on Screen | Already used in VideoPlaybackScreen |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| kotlinx.coroutines (lifecycleScope) | 1.8.1 | Async work in screen lifecycle | Already used for thumbnail loading and playback |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Constructor `initialScrollIndex` param | Hilt-injected NavigationStack | D-11 locks out DI; constructor param is simpler and doesn't require session-level state |
| Best-effort index tracking | SectionedItemTemplate (1.8.0+) scroll save | 1.8.0 is alpha/beta; not stable; Car App 1.7.0 is the locked version |

**Installation:** No new dependencies required — all libraries already in `build.gradle`.

---

## Architecture Patterns

### Recommended Project Structure

No new files needed. Changes are confined to existing files:

```
car/screens/
├── BrowseScreen.kt        # Add initialScrollIndex param, lastClickedIndex field, onResume invalidate
├── VideoPlaybackScreen.kt # Verify stop button label/icon (discretionary)
car/
└── AutoMediaSession.kt    # Remove or leave dead navigationStack field (cleanup)
util/
└── NavigationStack.kt     # Repurpose: trim unused methods, keep Browse(scrollPosition) concept
```

### Pattern 1: Constructor-Parameter Scroll State

**What:** BrowseScreen stores the index of the last item clicked in a `var lastClickedIndex: Int = initialScrollIndex` field. When pushing VideoPlaybackScreen or a child BrowseScreen, it passes the current `lastClickedIndex` as a lambda or captures it inline.

**When to use:** Anytime BrowseScreen is pushed (both from RootScreen and recursively from itself for sub-folders).

**Example:**

```kotlin
// BrowseScreen.kt — constructor
class BrowseScreen(
    carContext: CarContext,
    private val source: MediaSource,
    private val parentId: String?,
    private val initialScrollIndex: Int = 0   // NEW
) : Screen(carContext) {

    private var lastClickedIndex: Int = initialScrollIndex

    // in buildGridItem(), track index:
    private fun buildGridItem(item: MediaItem, index: Int): GridItem {
        ...
        builder.setOnClickListener {
            lastClickedIndex = index   // capture before push
            if (item.isFolder) {
                screenManager.push(BrowseScreen(carContext, source, item.id, 0))
            } else {
                screenManager.push(VideoPlaybackScreen(carContext, item))
            }
        }
    }
}
```

### Pattern 2: invalidate() on Screen Resume

**What:** When `VideoPlaybackScreen` is popped, the Car App host calls `onGetTemplate()` on the now-visible `BrowseScreen` again automatically. No explicit `invalidate()` call is needed in BrowseScreen — the host triggers a template refresh.

**When to use:** Trust the framework. `onGetTemplate()` is called when the screen becomes the top of the stack.

**Critical constraint — same template type rule:** When popping back to a Screen, the app MUST return the same template type as the last template sent from that screen. BrowseScreen always returns `GridTemplate` (or `MessageTemplate` for error/loading states) — as long as the same type is returned the host accepts it.

**Example:**

```kotlin
// No invalidate() needed on resume. onGetTemplate() will be called by the host.
// lastClickedIndex is already set; use it to build the template.
override fun onGetTemplate(): Template {
    // build GridTemplate using lastClickedIndex for reference
    // (actual scroll-to-index is NOT available in GridTemplate API — see Pitfall 1)
}
```

### Pattern 3: Back Press via Action.BACK (confirmed sufficient)

**What:** BrowseScreen already has `setStartHeaderAction(Action.BACK)` in its Header. This automatically calls `screenManager.pop()`. No override needed.

**When to use:** Already in place. No code change required.

### Pattern 4: onStop Lifecycle Observer (confirmed sufficient for VideoPlaybackScreen)

**What:** `VideoPlaybackScreen.init` adds a `DefaultLifecycleObserver` that calls `playerManager.stop()` in `onStop`. Since `ON_STOP` fires when the screen is popped (either via back button or `screenManager.pop()`), playback will stop correctly in both cases.

**Verified:** Car App Library `Screen` lifecycle progresses `ON_PAUSE → ON_STOP → ON_DESTROY` on pop. `onStop` fires. This is confirmed from source-level documentation.

### Anti-Patterns to Avoid

- **Using `ItemList.Builder.setSelectedIndex()` for scroll restoration:** This only works on selectable lists (with `OnSelectedListener`). On a click-to-navigate grid, calling `setSelectedIndex()` is silently ignored. Do not use this.
- **Overriding `OnBackPressedDispatcher`:** D-02 locks this out. The default behavior (pop) is correct. Do not add a custom `OnBackPressedCallback`.
- **Calling `screenManager.pop()` inside `onStop` of VideoPlaybackScreen:** `onStop` fires BECAUSE of the pop — calling pop again inside it would cause double-pop and corrupt the stack.
- **Using `NavigationStack` as the actual navigation driver:** D-09 locks this out. `screenManager` is the only navigation driver.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Back stack management | Custom stack data structure | `screenManager.push()` / `screenManager.pop()` | Car App Library enforces a 5-screen limit and manages host-side template quotas |
| Hardware back interception | Custom `OnBackPressedDispatcher` callback | Default `Action.BACK` behavior | Default already calls `screenManager.pop()`; overriding adds complexity with no benefit |
| Playback stop on back | Explicit `screenManager.pop()` override | `DefaultLifecycleObserver.onStop()` already present | Already wired; `onStop` fires reliably on pop |

**Key insight:** The Car App Library's screen stack is a host-managed contract. Fighting it (custom back dispatch, parallel stacks) causes template quota errors and undefined behavior.

---

## Common Pitfalls

### Pitfall 1: GridTemplate Has No Scroll-to-Index API

**What goes wrong:** Planner assumes `GridTemplate.Builder` has a `setInitialFocusedIndex(Int)` or similar method and writes a task to call it. No such method exists in Car App Library 1.7.0.

**Why it happens:** The feature exists in `SectionedItemTemplate` (added in 1.8.0-alpha02, stable in 1.8.0-beta01 as of April 2026) but NOT in `GridTemplate`. Confusing the two is easy.

**How to avoid:** Use `initialScrollIndex` as a constructor parameter that BrowseScreen records for its own state. The grid will be rebuilt from scratch when `onGetTemplate()` is called after resume — the host does not guarantee focus position. This is a best-effort UX.

**Warning signs:** Any task that mentions `setInitialFocusedIndex`, `setScrollPosition`, or `setSelectedIndex` on a GridTemplate without an `OnSelectedListener` is wrong.

### Pitfall 2: Double-Pop if screenManager.pop() Called Inside onStop

**What goes wrong:** If a developer adds `screenManager.pop()` inside the `onStop` lifecycle observer in `VideoPlaybackScreen`, the screen pops itself WHILE the framework is already executing a pop, causing stack corruption or a crash.

**Why it happens:** `onStop` fires as a consequence of pop. Adding another pop creates a re-entrant call.

**How to avoid:** Only call `screenManager.pop()` from explicit user-action listeners (the stop button's `setOnClickListener`). The lifecycle observer should only call `playerManager.stop()`.

**Warning signs:** `screenManager.pop()` inside any `onStop()`, `onPause()`, or `onDestroy()` override.

### Pitfall 3: Template Type Mismatch on Resume

**What goes wrong:** BrowseScreen currently returns `GridTemplate` normally but `MessageTemplate` for loading/permission states. If the last sent template before pushing VideoPlaybackScreen was a `MessageTemplate`, the host will reject a `GridTemplate` on resume (and vice versa).

**Why it happens:** The Car App Library enforces that on back-navigation the returned template type matches the last sent type. If state changes during VideoPlayback mean BrowseScreen's condition logic would return a different type, an error is thrown.

**How to avoid:** On resume after pop, BrowseScreen should ensure it returns the same template type it sent before the push. Since the user clicked an item (meaning loading had completed and `GridTemplate` was shown), this is naturally satisfied. But watch for edge cases where permissions are revoked mid-session.

**Warning signs:** `TemplateException` in logcat mentioning "template type mismatch" after back navigation.

### Pitfall 4: Items list re-loaded on Resume Loses Index Context

**What goes wrong:** BrowseScreen calls `loadMedia()` in `init`, which resets `items` and calls `invalidate()`. If `loadMedia()` is triggered again on resume (e.g., via a lifecycle observer on `onStart`), the `items` list is rebuilt and `lastClickedIndex` might point to a different item.

**Why it happens:** If someone adds `onStart { loadMedia() }` to keep the list fresh, the index-to-item mapping is invalidated.

**How to avoid:** Only call `loadMedia()` in `init` (current behavior). Do not re-trigger loading on resume for this phase. The items list is stable within a screen instance's lifetime.

---

## Code Examples

Verified patterns from official sources and existing project code:

### Back Navigation — Confirmed Working (no change needed)

```kotlin
// Source: existing VideoPlaybackScreen.kt — lifecycle observer
lifecycle.addObserver(object : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) {
        playerManager.stop()   // fires on pop (back press) AND on stop button
    }
})

// Stop button — explicit pop (already implemented)
.setOnClickListener {
    playerManager.stop()
    screenManager.pop()   // pops to BrowseScreen
}
```

### Scroll Index Tracking — New Pattern

```kotlin
// Source: pattern derived from Car App Library documentation + project conventions
class BrowseScreen(
    carContext: CarContext,
    private val source: MediaSource,
    private val parentId: String?,
    private val initialScrollIndex: Int = 0   // passed by caller on push
) : Screen(carContext) {

    private var lastClickedIndex: Int = initialScrollIndex

    // In items.forEachIndexed:
    items.forEachIndexed { index, item ->
        listBuilder.addItem(buildGridItem(item, index))
    }

    private fun buildGridItem(item: MediaItem, index: Int): GridItem {
        ...
        builder.setOnClickListener {
            lastClickedIndex = index
            if (item.isFolder) {
                screenManager.push(BrowseScreen(carContext, source, item.id, 0))
            } else {
                screenManager.push(VideoPlaybackScreen(carContext, item))
            }
        }
        return builder.build()
    }

    // onGetTemplate — host calls this on resume after pop automatically
    override fun onGetTemplate(): Template {
        // lastClickedIndex is available here
        // Note: GridTemplate has no scroll-to API; this index is available for
        // future use (e.g., visual highlight) but cannot force scroll position
        val listBuilder = ItemList.Builder()
        items.forEachIndexed { index, item ->
            listBuilder.addItem(buildGridItem(item, index))
        }
        return GridTemplate.Builder()
            .setHeader(header)
            .setItemSize(GridTemplate.ITEM_SIZE_LARGE)
            .setSingleList(listBuilder.build())
            .build()
    }
}
```

### RootScreen Push — Pass initialScrollIndex = 0

```kotlin
// RootScreen.kt — no change needed; default is 0
screenManager.push(BrowseScreen(carContext, source, parentId = null))
// becomes:
screenManager.push(BrowseScreen(carContext, source, parentId = null, initialScrollIndex = 0))
// or just rely on default parameter
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Custom back stack data structures | `screenManager.push/pop` (Car App Library native) | Car App Library 1.0 | Don't maintain parallel stacks |
| Manual lifecycle cleanup | `DefaultLifecycleObserver` pattern | Car App Library 1.0 | Hook into `onStop` cleanly |
| No scroll restoration in GridTemplate | `SectionedItemTemplate` auto-save (1.8.0+) | Car App 1.8.0-alpha02 (June 2025) | NOT available in 1.7.0; defer this feature if pixel-perfect scroll needed |

**Deprecated/outdated:**
- `onBackPressed()` Activity override: Not applicable to Car App Library Screens; use `OnBackPressedDispatcher` from `carContext` if overriding back is ever needed.

---

## Open Questions

1. **Does the Car App host guarantee `onGetTemplate()` is called when a screen is re-exposed after pop?**
   - What we know: Official docs say "`onGetTemplate()` is invoked whenever a new template is needed." The screen navigation docs describe quota reset on pop, implying a fresh template is fetched.
   - What's unclear: Whether there is a delay or whether the host may cache the previous template.
   - Recommendation: Treat it as guaranteed based on documentation. If testing reveals otherwise, add an explicit `invalidate()` in a lifecycle `onResume` observer.

2. **Will the Car App host visually scroll the grid to `lastClickedIndex` if the item is not visible?**
   - What we know: GridTemplate has no scroll-to-index API in 1.7.0. The grid is always rendered from the top on template rebuild.
   - What's unclear: Whether the host has any focus-restore heuristic based on prior template state.
   - Recommendation: Accept that scroll restoration is best-effort (records last clicked index for state bookkeeping, but grid resets to top visually on resume). Document this as a known limitation in PLAN.md verification steps.

---

## Environment Availability

Step 2.6: SKIPPED — this phase is code/config changes only within existing Android project. No new external tools, services, or CLIs required beyond the existing build toolchain.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | None — no test suite exists (per CLAUDE.md: "No test suite currently exists") |
| Config file | None |
| Quick run command | Manual: `adb logcat | grep "pscholer"` |
| Full suite command | Manual: Android Auto emulator or head unit |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FEAT-2-AC1 | Back on VideoPlaybackScreen stops playback and returns to Browse | manual | adb logcat observation | ❌ Wave 0 (no test infra) |
| FEAT-2-AC2 | Back on BrowseScreen returns to RootScreen | manual | adb logcat observation | ❌ Wave 0 |
| FEAT-2-AC3 | Stop button stops playback and pops to Browse | manual | adb logcat observation | ❌ Wave 0 |
| FEAT-2-AC4 | Scroll index tracked, BrowseScreen rebuilds with lastClickedIndex set | manual-only | Cannot automate (requires AA emulator) | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** Build with `./gradlew assembleDebug` — confirm no compile errors
- **Per wave merge:** Install and manually verify back navigation on Android Auto emulator
- **Phase gate:** All 4 acceptance criteria verified manually before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] No automated test framework exists — manual verification only (per CLAUDE.md constraint)
- [ ] `./gradlew assembleDebug` acts as the "compile gate" for each task

*(No automated test infrastructure to create — CLAUDE.md explicitly states no test suite exists and instrumented tests require real device/AA emulator)*

---

## Sources

### Primary (HIGH confidence)
- Car App Library Screen.java source (GitHub androidx/androidx) — lifecycle ON_STOP fires on pop confirmed
- Car App Library release notes 1.8.0-alpha02, 1.8.0-beta01 — SectionedItemTemplate scroll save confirmed; GridTemplate NOT included
- Android Developers: screen navigation docs — `Action.BACK` calls `screenManager.pop()`, ScreenManager stack behavior confirmed

### Secondary (MEDIUM confidence)
- Android Developers: `onGetTemplate()` called after `invalidate()` and on initial screen creation — inferred also called on resume after pop from quota documentation
- ItemList.java source (GitHub androidx/androidx) — `setSelectedIndex()` requires `OnSelectedListener` (selectable-only constraint confirmed)

### Tertiary (LOW confidence)
- Host behavior guaranteeing `onGetTemplate()` re-call on screen resume after pop — documented indirectly via quota reset behavior but not stated explicitly for the non-quota case

---

## Metadata

**Confidence breakdown:**
- Screen lifecycle (onStop fires on pop): HIGH — confirmed from Screen.java source code
- GridTemplate scroll API absence: HIGH — confirmed from release notes (feature is in SectionedItemTemplate 1.8.0, not GridTemplate 1.7.0)
- setSelectedIndex selectable-only constraint: HIGH — confirmed from ItemList.java source
- onGetTemplate() re-called on resume: MEDIUM — strongly implied by documentation, not stated verbatim for resume-after-pop case
- Constructor parameter pattern for scroll index: HIGH — consistent with existing project conventions

**Research date:** 2026-04-27
**Valid until:** 2026-05-27 (stable library; Car App 1.7.0 is locked in project)
