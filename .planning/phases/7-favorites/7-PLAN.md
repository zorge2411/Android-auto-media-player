# Phase 7 Plan — Favorites

**Phase:** 7-favorites  
**Goal:** Mark and filter favorite videos  
**Estimated Effort:** 2-3 hours

## Quick Summary

Add heart/star icon to toggle favorites. Show favorites in separate section.

## Tasks

### Task 7.1: Create Favorite Model
**File:** `data/Favorite.kt`
```kotlin
data class Favorite(
    val id: String = UUID.randomUUID().toString(),
    val mediaId: String,
    val source: String, // LOCAL, PLEX, JELLYFIN
    val mediaTitle: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

### Task 7.2: Create FavoriteRepository
**File:** `data/FavoriteRepository.kt`
```kotlin
class FavoriteRepository(private val preferencesManager: PreferencesManager) {
    suspend fun add(favorite: Favorite)
    suspend fun remove(mediaId: String)
    suspend fun isFavorite(mediaId: String): Boolean
    suspend fun list(): List<Favorite>
    suspend fun listBySource(source: String): List<Favorite>
}
```

### Task 7.3: Add Heart Icon to VideoPlaybackScreen
**File:** `car/screens/VideoPlaybackScreen.kt`
- Add favorite toggle button to ActionStrip
- Observe FavoriteRepository to show filled/hollow heart
- On click, toggle favorite status

### Task 7.4: Add Favorite Indicator to BrowseScreen
**File:** `car/screens/BrowseScreen.kt`
- Show small heart icon next to favorited videos
- Observe FavoriteRepository for each video

### Task 7.5: Create FavoritesScreen
**File:** `car/screens/FavoritesScreen.kt` (NEW)
- Add "Favorites" menu item to RootScreen
- List all favorited videos across all sources
- Play video on select
- Toggle favorite from list (remove from favorites)

### Task 7.6: Add "Add to Favorites" Context Menu
**File:** `car/screens/BrowseScreen.kt`
- Right-click or long-press on video
- Option: "Add to Favorites" / "Remove from Favorites"

## UAT Criteria
- [ ] Toggle favorite by tapping heart icon
- [ ] Favorite status persists
- [ ] Heart icon shows favorite status in list
- [ ] Favorites screen lists all favorited videos
- [ ] Works across LOCAL/PLEX/JELLYFIN sources
- [ ] Remove favorite from list works

## Data Storage
- DataStore preferences: `favorites` (JSON array)
- Cross-source: mediaId + source is unique key
- No server sync (local-only MVP)

## Notes
- Favorites are app-local (not synced with Plex/Jellyfin watched status)
- Icon should be consistent (material design heart)
- Optional: Sort favorites by date added or title
