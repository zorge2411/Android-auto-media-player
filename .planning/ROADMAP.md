# Roadmap — Android Auto Media Player

**Project:** Android Auto Media Player  
**Milestone:** Feature Week 1  
**Created:** 2026-04-17  
**Target Completion:** 2026-04-24  

---

## Overview

7-phase roadmap implementing playback UI enhancements, audio management, and user collections (playlists, favorites, resume).

**Theme:** From basic video player → feature-rich media app

---

## Phase 1: Progress Slider & Timeline Display

**Goal:** Display playback progress and enable seeking via slider control

**Scope:**
- Add SeekBar/Slider to VideoPlaybackScreen template
- Connect to ExoPlayer's current position and duration
- Implement seek on drag/tap
- Display formatted duration (HH:MM:SS format)
- Real-time position updates during playback

**Deliverables:**
- VideoPlaybackScreen updated with slider UI
- ExoPlayer seek binding
- Duration/position formatter
- CarAppService TV safety compliance check

**UAT Criteria:**
- Slider visible and interactive
- Seek works within ±2 seconds of target
- Duration updates in real-time
- No playback stutter during seek

**Risk:** Car App Library may restrict SeekBar interactivity; fallback to ExoPlayer built-in player UI controls if needed

---

## Phase 2: Stop Other Audio Sources

**Goal:** Pause other apps' audio (music, podcasts) when playback starts

**Scope:**
- Request audio focus in MediaPlayerManager.play()
- Release audio focus on pause/stop
- Handle audio focus loss (pause if another app takes focus)
- Maintain focus while app backgrounded (foreground service)

**Deliverables:**
- AudioManager integration in MediaPlayerManager
- Audio focus request/release lifecycle
- Focus loss callback handler
- Permission verification

**UAT Criteria:**
- Other audio paused when playback starts
- Focus released cleanly on exit
- Foreground service holds focus while minimized
- No permission crashes

**Risk:** Car head unit audio routing may override focus requests

---

## Phase 3: Media Aspect Ratio Preservation

**Goal:** Display videos in native aspect ratio without distortion

**Scope:**
- Extract video aspect ratio from ExoPlayer VideoSize listener
- Calculate surface dimensions based on aspect ratio
- Apply scaling in VideoSurfaceRenderer
- Test with common formats (16:9, 4:3, 21:9, 9:16)

**Deliverables:**
- VideoSurfaceRenderer aspect ratio scaling
- Aspect ratio calculation logic
- SurfaceTexture dimension updates
- Test matrix for format coverage

**UAT Criteria:**
- Videos display in native aspect ratio
- No visible distortion or stretching
- Works on real Android Auto head unit
- No black bars (or minimal, architecture-constrained)

**Risk:** SurfaceContainer may impose fixed dimensions; may need to adjust viewport instead

---

## Phase 4: Smart Back Button Navigation

**Goal:** Context-aware back navigation (resume vs. browse)

**Scope:**
- Implement back stack tracking (RootScreen → BrowseScreen → VideoPlaybackScreen)
- Preserve browse state (scroll position, selected item)
- Route back to appropriate screen based on entry point
- Implement long-press to stop and exit

**Deliverables:**
- Navigation state manager
- Back handler in AutoMediaSession
- Browse state preservation (scroll position)
- UX pattern documentation for users

**UAT Criteria:**
- Back returns to previous screen with state preserved
- Long-press stops playback
- No navigation loops or crashes
- Scroll position maintained in browse

**Risk:** Car App Library navigation model may differ from standard Android; verify with real device

**Plans:** 2/2 plans complete
- [x] 04-01-PLAN.md — BrowseScreen scroll-index tracking (initialScrollIndex param, lastClickedIndex field)
- [x] 04-02-PLAN.md — Remove dead NavigationStack, document back-navigation contract

---

## Phase 5: Playback Resume Infrastructure

**Goal:** Track and restore video watch positions

**Scope:**
- Add PlaybackState model (mediaId, position, timestamp)
- DataStore persistence for playback state
- Periodic position saves (every 10 seconds)
- Auto-seek on playback start if position exists
- Clear position on video completion (>95%)

**Deliverables:**
- PlaybackState data class
- DataStore schema and access layer
- MediaPlayerManager position tracking
- Restoration logic in VideoPlaybackScreen

**UAT Criteria:**
- Position saved during playback
- App exit and restart restores position
- Resume within 10 seconds accuracy
- Position cleared on completion
- Works across app backgrounding

**Risk:** Position accuracy depends on ExoPlayer event timing; may drift slightly

---

## Phase 6: Playlists (Create, Edit, Play)

**Goal:** Allow users to create custom video collections

**Scope:**
- Add Playlist data model (id, name, videos, createdAt)
- DataStore persistence for playlists
- UI for creating/editing playlists in BrowseScreen
- New "Playlists" menu in RootScreen
- Playlist playback (auto-advance to next video)
- Delete/rename playlists

**Deliverables:**
- Playlist data model and DAO
- PlaylistRepository (read/write playlists)
- Playlist creation/edit UI flow
- Playlist playback integration with MediaPlayerManager
- Playlist browser screen

**UAT Criteria:**
- Create playlist via context menu
- Add/remove videos from playlist
- Playlist visible in RootScreen
- Playlist plays in sequence
- Edit/delete operations work
- Playlists survive app restart

**Risk:** Car App Library text input limited; playlist naming may be tedious

**Depends on:** Phase 5 (playback resume for persistence)

---

## Phase 7: Favorites (Mark & Filter)

**Goal:** Allow users to mark and filter favorite videos

**Scope:**
- Add Favorite data model (mediaId, source, timestamp)
- DataStore persistence for favorites
- Heart/star icon in playback and browse screens
- Visual indicator in browse list
- New "Favorites" section in RootScreen
- Filter across LOCAL/PLEX/JELLYFIN sources

**Deliverables:**
- Favorite data model and DAO
- FavoriteRepository (read/write)
- UI for toggling favorite status
- Favorites browsing screen
- Visual indicator in MediaItem lists

**UAT Criteria:**
- Toggle favorite by tapping heart icon
- Favorite status persists
- Favorites screen shows all favorited videos
- Visual indicator visible in browse
- Works across all sources

**Risk:** Cross-source favorite matching requires stable mediaId scheme

**Depends on:** Phase 5 (DataStore patterns established)

---

## Dependency Graph

```
Phase 1 (Progress Slider)
  └─ Standalone

Phase 2 (Stop Other Audio)
  └─ Standalone

Phase 3 (Aspect Ratio)
  └─ Standalone

Phase 4 (Smart Back)
  └─ Depends on: Phase 1 (playback screen exists)

Phase 5 (Resume Infrastructure)
  └─ Depends on: Phase 1 (playback screen exists)

Phase 6 (Playlists)
  └─ Depends on: Phase 5 (persistence infrastructure)

Phase 7 (Favorites)
  └─ Depends on: Phase 5 (persistence infrastructure)
```

## Execution Order (Recommended)

**Week Timeline:**
- **Day 1-2**: Phases 1, 2, 3 (parallel; no dependencies)
- **Day 2-3**: Phase 4 (after phase 1 complete)
- **Day 3-4**: Phase 5 (core persistence)
- **Day 4-5**: Phases 6, 7 (parallel; both depend on phase 5)
- **Day 5**: UAT & integration testing on real device
- **Day 6-7**: Bug fixes & polish

---

## Success Metrics

- **All 7 phases complete** with UAT passing
- **Code coverage**: At least 1 test per feature (even if minimal)
- **Real device test**: All features verified on Android Auto head unit
- **Zero crashes** related to new code
- **Performance**: Playback remains smooth at 30fps minimum

---

## Known Constraints

- **Car App Library API limits**: Some features may be constrained by car display restrictions
- **Testing challenges**: Android Auto features hard to test without real device
- **Aspect ratio**: May be architecture-limited by SurfaceContainer
- **Text input**: Playlist naming may require voice input or simplified flow
- **Audio focus**: Car head units may override system audio focus

---

## Out of Scope (Future Milestones)

- Multi-device sync
- Server-side playlists (Plex collections, Jellyfin playlists)
- Rewind/fastforward buttons
- Playback speed control
- Watched/unwatched status
- Sharing playlists
