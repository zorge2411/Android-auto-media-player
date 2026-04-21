# Android Auto Media Player — Project Goals

**Status:** Active Development  
**Target:** This week  
**Owner:** peter.scholer74@gmail.com  

## Vision

Transform the Android Auto media player from a basic playback app into a feature-rich media browser with playback controls, user-created collections, and seamless playback experience.

## Success Criteria

- **Playback UX**: Users can scrub/seek with progress slider and understand current position
- **Audio Management**: Other audio sources (music, podcasts) stop when playback starts
- **Visual Fidelity**: Videos display in correct aspect ratio without letterboxing
- **Collections**: Users can create playlists and favorite videos for quick access
- **Session Persistence**: App remembers watch position across restarts
- **Navigation**: Back button intelligently routes based on context (resume playback vs. browse)

## Features (This Milestone)

### Player UI
- [x] Plan progress slider implementation
- [x] Plan media aspect ratio handling
- [x] Plan smart back button routing

### Audio Management
- [x] Plan audio focus / stop other sources

### Collections & Persistence
- [x] Plan playlists (creation, management, playback)
- [x] Plan favorites (marking, retrieval)
- [x] Plan playback resume (position tracking, restore on reopen)

## Technical Constraints

- **Android 10+ (minSdk 29)** — required for scoped storage
- **NAVIGATION category** — non-negotiable for video rendering
- **ExoPlayer-based playback** — architecture locked to Media3
- **Single-module** — all code in com.pscholer.autoplayer
- **Hilt DI** — dependency injection framework
- **DataStore persistence** — for user preferences and state

## Architecture Decisions

- **MediaRepository pattern** — routes queries to LOCAL/PLEX/JELLYFIN, can be extended for playlists
- **MediaPlayerManager** — owns ExoPlayer instance, extends with position tracking
- **Car App Library** — UI constraints (no complex dialogs, limited text input)

## Known Risks

1. **Playlist complexity**: Storing playlist metadata (order, ownership, contents) across LOCAL/PLEX/JELLYFIN sources
2. **Position tracking**: Syncing playback position with remote servers (Plex/Jellyfin support this)
3. **Audio focus**: Requires proper audio focus management; may conflict with system audio
4. **Aspect ratio**: SurfaceContainer may enforce constraints; test required
5. **Testing gap**: Zero test coverage; features need verification on real device
