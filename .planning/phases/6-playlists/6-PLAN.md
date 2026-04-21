# Phase 6 Plan — Playlists

**Phase:** 6-playlists  
**Goal:** Create and manage custom video collections  
**Estimated Effort:** 4-5 hours (largest phase)

## Quick Summary

Store playlists with video references. Enable creation, editing, playback (auto-advance).

## Tasks

### Task 6.1: Create Playlist Models
**File:** `data/Playlist.kt`
```kotlin
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val owner: String = "LOCAL",
    val videos: List<PlaylistItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class PlaylistItem(
    val mediaId: String,
    val source: String,
    val addedAt: Long = System.currentTimeMillis()
)
```

### Task 6.2: Create PlaylistRepository
**File:** `data/PlaylistRepository.kt`
```kotlin
class PlaylistRepository(private val preferencesManager: PreferencesManager) {
    suspend fun create(playlist: Playlist)
    suspend fun list(): List<Playlist>
    suspend fun get(id: String): Playlist?
    suspend fun update(playlist: Playlist)
    suspend fun delete(id: String)
    suspend fun addVideo(playlistId: String, item: PlaylistItem)
    suspend fun removeVideo(playlistId: String, mediaId: String)
}
```

### Task 6.3: Add "Playlists" Menu Item to RootScreen
**File:** `car/screens/RootScreen.kt`
- New menu item: "Playlists"
- Navigate to PlaylistBrowseScreen on select

### Task 6.4: Create PlaylistBrowseScreen
**File:** `car/screens/PlaylistBrowseScreen.kt` (NEW)
- List all playlists
- Option to create new
- Option to delete
- Navigate to PlaylistDetailScreen to view/edit

### Task 6.5: Create PlaylistDetailScreen
**File:** `car/screens/PlaylistDetailScreen.kt` (NEW)
- Display videos in playlist
- Play from current position
- Remove video option
- Back to browse

### Task 6.6: Create New Playlist Dialog/Flow
**File:** `car/screens/CreatePlaylistScreen.kt` (NEW)
- Simple text input for playlist name
- Car App Library limited text input → use voice or pre-defined names

### Task 6.7: Integrate Playlist Playback
**File:** `car/AutoMediaSession.kt`
- When video in playlist finishes, auto-load next video
- Update current position in playlist

## UAT Criteria
- [ ] Create playlist via context menu
- [ ] Add/remove videos
- [ ] Browse playlists
- [ ] Play playlist (auto-advance)
- [ ] Edit/delete playlists
- [ ] Playlists survive app restart

## Challenges
- Car App Library limited text input (consider voice or pre-defined names)
- No multi-select UI for adding videos to playlist → drag-and-drop not feasible
- Workaround: "Add to Playlist" context menu on each video
