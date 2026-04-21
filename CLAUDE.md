# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Build the app (debug variant with .debug suffix)
./gradlew build

# Build and run on connected device
./gradlew installDebug

# Run the app on a device (after install)
adb shell am start -n com.pscholer.autoplayer.debug/.SettingsActivity

# Connect to Android Auto emulator or head unit
adb forward tcp:5037 tcp:5037

# View logcat output (filtered for app)
adb logcat | grep "pscholer\|AutoMedia\|VideoSurface"

# Build release APK (requires proguard)
./gradlew assembleRelease
```

## Project Architecture

**Single-module Android project** — `com.pscholer.autoplayer` package. Entry points are:
- **SettingsActivity**: Phone-side configuration UI (LAUNCHER intent)
- **AutoMediaService**: Android Auto entry point (extends CarAppService)

### Data Flow Architecture

**MediaRepository** (singleton facade) routes requests to three sources:
- **LOCAL**: MediaStore scan + SAF (Storage Access Framework) folders
- **PLEX**: Plex server via PlexApi (Retrofit)
- **JELLYFIN**: Jellyfin server via JellyfinApi (Retrofit)

Each source implements the same `getItems(source, parentId)` contract, returning normalized `MediaItem` objects.

**MediaSource enum** selects the active source. Parent ID semantics differ per source:
- LOCAL: null = full scan, non-null = ignored (flat list only)
- PLEX: null = library sections, non-null = section children
- JELLYFIN: null = user views, non-null = items under parent

### Android Auto Integration (CRITICAL)

**CarAppService declared as NAVIGATION category, not MEDIA.** This is a sideload workaround:
- NAVIGATION apps get full SurfaceContainer access for custom rendering (required for video surface)
- MEDIA apps are sandboxed into AA's media browser UI (blocks video output)
- For Play Store release, would need to switch to MEDIA category and accept AA media browser constraints

**Video rendering pipeline:**
1. `AutoMediaSession` creates `VideoPlaybackScreen` (extends NavigationTemplate)
2. Screen's `SurfaceCallback` fires → `VideoSurfaceRenderer` attaches Surface to ExoPlayer
3. `MediaPlayerManager` (Media3/ExoPlayer) handles playback state and seek

**Host validation:** `ALLOW_ALL_HOSTS_VALIDATOR` bypasses Play Store signature checks (sideload only). Never use in production Play Store builds.

### Key Dependencies & Versions

- **Gradle/Kotlin**: AGP 8.7.3, Kotlin 2.0.0, compileSdk 35, minSdk 29
- **Car App Library**: 1.7.0 (app + app-projected for phone→head unit projection)
- **Media3/ExoPlayer**: 1.3.1 (playback, HLS/DASH/SmoothStreaming, UI, OkHttp data source)
- **Networking**: Retrofit 2.11.0, OkHttp 4.12.0
- **DI**: Hilt 2.51.1 (annotations processed via kapt)
- **Persistence**: DataStore 1.1.1 (preferences for server URLs/API keys)
- **Image loading**: Coil 2.6.0 (thumbnails/posters)
- **Coroutines**: 1.8.1

### Entry Points & Key Classes

```
AutoPlayerApp
  └─ HiltAndroidApp (triggers Hilt code generation)

SettingsActivity
  └─ Phone-side UI for configuring Plex/Jellyfin servers + API keys

AutoMediaService (CarAppService)
  └─ onCreateSession() → AutoMediaSession

AutoMediaSession
  └─ Creates RootScreen, BrowseScreen, VideoPlaybackScreen

AutoMediaSession also owns MediaPlayerManager
  └─ Wraps ExoPlayer, handles state and seek operations

MediaRepository (injected into AutoMediaSession)
  └─ Routes getItems() calls to LocalMediaRepository, PlexRepository, or JellyfinRepository
  └─ Combines results (LOCAL source merges MediaStore + SAF folders)
```

## Module Structure

- `di/` — Hilt modules (AppModule, AppEntryPoint for Car App screens)
- `car/` — Android Auto UI (screens, surface renderer, media session)
- `service/` — AutoMediaService entry point
- `player/` — MediaPlayerManager (ExoPlayer wrapper)
- `data/` — MediaRepository, data sources (local, Plex, Jellyfin), models
- `util/` — PreferencesManager (DataStore wrapper for server config)

## Important Constraints

1. **NAVIGATION category is non-negotiable for video rendering** — switching to MEDIA breaks SurfaceCallback
2. **minSdk 29** — required for scoped storage (SAF) and adequate Car App Library support
3. **Kotlin 17 target** — required by modern androidX libraries; not negotiable
4. **Hilt EntryPoint pattern** — Car App Screens don't use @AndroidEntryPoint; use AppEntryPoint.entryPoint() to inject dependencies
5. **Foreground service** — media playback runs as ForegroundService (mediaPlayback type) to prevent OS termination

## Testing

No test suite currently exists. When adding tests:
- Unit tests should mock ExoPlayer and network calls (Retrofit)
- Instrumented tests need a real device/emulator with Android Auto projection
- Do not test NAVIGATION category declaration or SurfaceCallback directly in CI (requires AA emulator)

