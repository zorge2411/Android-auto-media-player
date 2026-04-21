# Requirements — Android Auto Media Player Features

**Milestone:** This Week  
**Phase Count:** 7  
**Epic Themes:** Player UX, Collections, Persistence

---

## Feature 1: Progress Slider & Playback Timeline

**User Story:** As a driver, I want to see how far through a video I am and be able to scrub/seek to a specific timestamp.

**Acceptance Criteria:**
- Progress slider displayed below/beside video in VideoPlaybackScreen
- Slider shows current position and total duration
- User can drag to seek (tap + drag or swipe gesture)
- Duration displayed as "HH:MM:SS / HH:MM:SS" format
- Slider updates in real-time as playback progresses
- Works with ExoPlayer's seek API

**Constraints:**
- Car App Library has limited interactive controls; verify slider works in NAVIGATION template
- No touch input while driving; slider only functional when parked

**Out of Scope:**
- Rewind/fastforward buttons (use seek + slider instead)
- Playback speed control

---

## Feature 2: Smart Back Button Navigation

**User Story:** As a user, I want the back button to intelligently route me based on context (resume playback vs. return to browse).

**Acceptance Criteria:**
- Back button on VideoPlaybackScreen routes to BrowseScreen (resume from same position)
- Back button on BrowseScreen returns to RootScreen
- Long-press back (or dedicated button) stops playback and exits
- Previous browse state (scroll position, selected item) is preserved

**Constraints:**
- Car App Library screens must use back navigation stacks
- No OS back button; must implement custom back via template API

**Out of Scope:**
- Multi-level back stack (just 3 screens: Root → Browse → Video)

---

## Feature 3: Stop Other Audio Sources

**User Story:** As a user, I want the app to pause other audio when playback starts (podcasts, music apps, etc.).

**Acceptance Criteria:**
- AudioManager.requestAudioFocus() called when ExoPlayer starts
- System pauses other audio apps (if they respect audio focus)
- Release audio focus when playback stops or app backgrounded
- Foreground service maintains focus while app minimized

**Constraints:**
- Requires MODIFY_AUDIO_SETTINGS permission
- Some apps (YouTube, Spotify premium) may not respect audio focus
- Car head unit may have its own audio priority rules

**Out of Scope:**
- Detecting or controlling other specific apps

---

## Feature 4: Media Aspect Ratio Preservation

**User Story:** As a user, I want videos to display in their native aspect ratio without distortion or letterboxing.

**Acceptance Criteria:**
- Detect video aspect ratio from ExoPlayer metadata
- Calculate VideoSurfaceRenderer surface size based on aspect ratio
- Apply scaling to SurfaceTexture (or adjust surface bounds)
- Test with common formats: 16:9, 4:3, 21:9, 9:16
- No black bars or pillarboxing visible

**Constraints:**
- SurfaceContainer provides fixed surface size; scaling must happen at renderer level
- Testing requires real Android Auto head unit or emulator

**Out of Scope:**
- Fit-to-screen modes (just native aspect ratio)
- Manual aspect ratio adjustment

---

## Feature 5: Playlists (Create, Edit, Play)

**User Story:** As a user, I want to create custom playlists and save videos to them for later playback.

**Acceptance Criteria:**
- Create playlist via BrowseScreen context menu
- Add/remove videos to playlist
- Browse playlists in RootScreen (new menu item)
- Play playlist (auto-play next video on finish)
- Edit playlist name and order
- Delete playlist

**Data Model:**
- Playlist: id, name, owner (LOCAL), createdAt, videos (list of MediaItem references)
- Store in DataStore as JSON

**Constraints:**
- LOCAL playlists only (Plex/Jellyfin can use their native playlists via API later)
- UI limited to car-friendly selection (no multi-select drag-and-drop)

**Out of Scope:**
- Server-side playlists (Plex collections, Jellyfin playlists)
- Sharing playlists across devices

---

## Feature 6: Favorites (Mark & Filter)

**User Story:** As a user, I want to mark videos as favorites for quick access to my preferred content.

**Acceptance Criteria:**
- Heart/star icon in VideoPlaybackScreen and BrowseScreen
- Tap icon to toggle favorite status
- Show favorite status in browse list (visual indicator)
- New "Favorites" section in RootScreen to browse all favorited videos
- Filter works across LOCAL/PLEX/JELLYFIN (favorites are app-level, not server-level)

**Data Model:**
- Favorite: id, mediaItemId, source (LOCAL/PLEX/JELLYFIN), timestamp
- Store in DataStore as JSON

**Constraints:**
- Favorites are local app state (not synced with servers)
- Must track which server a video came from to reconstruct MediaItem

**Out of Scope:**
- Server-side favorites (Plex watchlist, Jellyfin ratings)

---

## Feature 7: Playback Resume (Position Tracking & Restore)

**User Story:** As a user, I want the app to remember my watch position in a video so I can resume from where I left off.

**Acceptance Criteria:**
- On playback start, check DataStore for saved position
- If found, seek to saved position automatically
- While playing, periodically save current position (every 10 seconds)
- Save position on pause, stop, or app exit
- Restore position within 10 seconds accuracy
- Clear position when video finished (>95% watched)

**Data Model:**
- PlaybackState: mediaItemId, source, position (ms), duration (ms), lastUpdated
- Store in DataStore

**Constraints:**
- Position tracking for LOCAL/PLEX/JELLYFIN
- Plex/Jellyfin have native resume endpoints; use those for server-side tracking too

**Out of Scope:**
- Multi-device sync (just local device)
- Watched/unwatched status (that's for Plex/Jellyfin ratings)

---

## Glossary

- **MediaItem**: Unified video object with id, title, source, duration, metadata
- **MediaRepository**: Facade routing browse/list queries to LOCAL/PLEX/JELLYFIN
- **ExoPlayer**: Google's Media3 video player; handles playback, seeking, audio focus
- **SurfaceContainer**: Car App Library API for rendering custom content (video surface)
- **DataStore**: Android persistence library for app preferences and state
- **Audio Focus**: Android system mechanism for managing which app plays audio

---

## Dependencies Between Features

```
Progress Slider ──→ Playback Resume (seek needs accurate duration tracking)
                ┌──→ Smart Back Button (must preserve navigation state)
                │
Stop Other Audio ┘
                 
Media Aspect Ratio ──→ (standalone, no dependencies)

Playlists ──→ Playback Resume (resume within playlist)
Favorites ──→ (standalone, no dependencies)
```

## Priority Order (This Week)

1. **Progress Slider** (foundational for playback UX)
2. **Stop Other Audio** (UX improvement, small effort)
3. **Media Aspect Ratio** (visual correctness, needed before showing feature)
4. **Smart Back Button** (navigation polish)
5. **Playback Resume** (infrastructure for persistent state)
6. **Playlists** (most complex, depends on #5)
7. **Favorites** (medium complexity, independent)
