# Phase 5 Plan — Playback Resume

**Phase:** 5-playback-resume  
**Goal:** Track and restore video watch positions  
**Estimated Effort:** 3-4 hours

## Quick Summary

Save playback position to DataStore periodically. Auto-seek when playback resumes.

## Tasks

### Task 5.1: Create PlaybackState Model
**File:** `data/PlaybackState.kt`
```kotlin
data class PlaybackState(
    val mediaId: String,
    val source: String, // LOCAL, PLEX, JELLYFIN
    val positionMs: Long,
    val durationMs: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)
```

### Task 5.2: Create PlaybackRepository
**File:** `data/PlaybackRepository.kt`
```kotlin
class PlaybackRepository(private val preferencesManager: PreferencesManager) {
    suspend fun save(state: PlaybackState)
    suspend fun load(mediaId: String): PlaybackState?
    suspend fun delete(mediaId: String)
}
```

### Task 5.3: Integrate Position Tracking in MediaPlayerManager
- Periodic ticker (every 10 seconds) to save position via PlaybackRepository
- Clear position on completion (>95% watched)
- Clear on stop()

### Task 5.4: Auto-Restore on Playback Start
**File:** `car/screens/VideoPlaybackScreen.kt`
- On playback start, query PlaybackRepository
- If position found, seek to saved position
- Verify position is < 95% (not already watched)

### Task 5.5: Plex/Jellyfin Sync (Optional)
- For Plex/Jellyfin sources, also update server resume point via their APIs
- Local storage is MVP; server sync is enhancement

## UAT Criteria
- [ ] Position saved every 10 seconds
- [ ] App exit and restart resumes position
- [ ] Position clears on completion
- [ ] Position clears on explicit stop
- [ ] Works across LOCAL/PLEX/JELLYFIN

## Data Storage
- DataStore preferences: `playback_state_{mediaId}`
- Serialization: JSON via PreferencesManager
