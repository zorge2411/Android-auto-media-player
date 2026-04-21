# Phase 4 Plan — Smart Back Button Navigation

**Phase:** 4-back-navigation  
**Goal:** Context-aware back navigation (resume vs. browse)  
**Estimated Effort:** 2-3 hours

## Quick Summary

Implement navigation stack tracking so back button returns to previous screen with preserved state.

## Tasks

### Task 4.1: Create Navigation State Manager
**File:** `util/NavigationStack.kt`
```kotlin
class NavigationStack {
    private val stack = mutableListOf<NavigationTarget>()
    
    sealed class NavigationTarget {
        object Root : NavigationTarget()
        data class Browse(val parentId: String?) : NavigationTarget()
        data class Video(val mediaItem: MediaItem) : NavigationTarget()
    }
    
    fun push(target: NavigationTarget) { stack.add(target) }
    fun pop(): NavigationTarget? = if (stack.isNotEmpty()) stack.removeAt(stack.size - 1) else null
    fun peek(): NavigationTarget? = stack.lastOrNull()
    fun clear() { stack.clear() }
}
```

### Task 4.2: Integrate into AutoMediaSession
**File:** `car/AutoMediaSession.kt`
- Add NavigationStack as member
- Track screen transitions (Root → Browse → Video)
- Implement back handler that pops stack and navigates

### Task 4.3: Preserve Scroll Position
**File:** `car/screens/BrowseScreen.kt`
- Store scroll position when navigating away
- Restore position when returning to screen

### Task 4.4: Handle Stop/Exit
**File:** `car/screens/VideoPlaybackScreen.kt`
- Long-press back = clear playback and navigate to Root
- Confirmation dialog before stopping (optional MVP)

## UAT Criteria
- [ ] Back from Video → Browse with scroll restored
- [ ] Back from Browse → Root
- [ ] Long-press back stops playback
- [ ] No navigation loops

## Rollback
If back breaks, remove NavigationStack integration and use simple pop() on each screen.
