# Codebase Structure

**Analysis Date:** 2026-04-17

## Directory Layout

```
com.pscholer.autoplayer/
├── di/                          # Dependency injection (Hilt modules)
│   ├── AppModule.kt             # Single-component module (currently empty)
│   └── AppEntryPoint.kt         # Hilt entry point for non-standard DI
├── car/                         # Android Auto UI components
│   ├── AutoMediaSession.kt      # CarAppService session lifecycle
│   ├── screens/                 # Car app screens
│   │   ├── RootScreen.kt        # Media source selector (Local/Plex/Jellyfin)
│   │   ├── BrowseScreen.kt      # Hierarchical media browser
│   │   └── VideoPlaybackScreen.kt # Video playback overlay
│   └── surface/
│       └── VideoSurfaceRenderer.kt # Car display Surface callback + lifecycle
├── service/
│   └── AutoMediaService.kt      # CarAppService entry point
├── player/
│   └── MediaPlayerManager.kt    # ExoPlayer singleton wrapper
├── data/                        # Data layer (repositories)
│   ├── MediaRepository.kt       # Unified facade for all media sources
│   ├── MediaSource.kt           # Enum: LOCAL, PLEX, JELLYFIN
│   ├── models/
│   │   └── MediaItem.kt         # Parcelable data model (all sources)
│   ├── local/
│   │   └── LocalMediaRepository.kt # MediaStore + SAF scanning
│   └── remote/
│       ├── plex/
│       │   ├── PlexApi.kt       # Retrofit interface
│       │   ├── PlexModels.kt    # API response DTOs
│       │   └── PlexRepository.kt # Retrofit client + mapping
│       └── jellyfin/
│           ├── JellyfinApi.kt   # Retrofit interface
│           ├── JellyfinModels.kt # API response DTOs + auth models
│           └── JellyfinRepository.kt # Retrofit client + auth + mapping
├── util/
│   └── PreferencesManager.kt    # DataStore-backed config persistence
├── SettingsActivity.kt          # Phone-side configuration UI
└── AutoPlayerApp.kt             # Application class (@HiltAndroidApp)

resources/
├── xml/
│   ├── automotive_app_desc.xml  # Android Auto permissions/features
│   └── network_security_config.xml # Cleartext HTTP config
├── layout/
│   └── activity_settings.xml    # Settings screen layout (phone UI)
├── drawable/
│   ├── ic_folder.xml, ic_video_file.xml, ic_plex.xml, ic_jellyfin.xml
│   ├── ic_play.xml, ic_pause.xml, ic_seek_back.xml, ic_seek_forward.xml
│   └── ic_launcher_*.xml
├── values/
│   ├── strings.xml              # String resources
│   ├── colors.xml               # Color definitions
│   └── themes.xml               # App theme
└── mipmap/
    └── ic_launcher_*.xml        # App icon variants
```

## Directory Purposes

**di/**
- Purpose: Hilt dependency injection configuration
- Bindings: All services use constructor injection (auto-bound as singletons)
- Key file: `AppEntryPoint.kt` — non-standard component access (Screens, Sessions)

**car/**
- Purpose: Android Auto UI components (screens, lifecycle, surface rendering)
- Dependency: Accesses MediaRepository and MediaPlayerManager via AppEntryPoint
- Key classes:
  - `AutoMediaSession` — creates root screen, registers surface callback
  - `RootScreen` — triple-tile picker (Local/Plex/Jellyfin)
  - `BrowseScreen` — hierarchical navigator with async thumbnail prefetch
  - `VideoPlaybackScreen` — playback controls (play/pause, seek)
  - `VideoSurfaceRenderer` — bridges car display Surface to ExoPlayer

**service/**
- Purpose: Android Auto service entry point
- Key file: `AutoMediaService.kt` — sideload workarounds (ALLOW_ALL_HOSTS_VALIDATOR, NAVIGATION category)

**player/**
- Purpose: Media playback engine
- Key file: `MediaPlayerManager.kt` — ExoPlayer singleton with surface attachment and state flows

**data/models/**
- Purpose: Unified data model across all sources
- Key file: `MediaItem.kt` — Parcelable (passed between screens); fields: id, title, isFolder, thumbnailUrl, streamUrl, headers, mimeType, durationMs, subtitle

**data/local/**
- Purpose: On-device media discovery
- Key file: `LocalMediaRepository.kt` — MediaStore scan + SAF directory walk

**data/remote/plex/**
- Purpose: Plex media server integration
- Key files:
  - `PlexApi.kt` — Retrofit interface (/library/sections, /library/sections/{id}/all, /library/metadata/{key}/children)
  - `PlexModels.kt` — API DTOs (PlexLibraryResponse, PlexMediaResponse, PlexMetadata)
  - `PlexRepository.kt` — HTTP client factory with auth header interceptor

**data/remote/jellyfin/**
- Purpose: Jellyfin media server integration
- Key files:
  - `JellyfinApi.kt` — Retrofit interface (/Users/AuthenticateByName, /Users/{userId}/Views, /Users/{userId}/Items)
  - `JellyfinModels.kt` — API DTOs + auth models (JellyfinAuthRequest, JellyfinAuthResponse, JellyfinItem)
  - `JellyfinRepository.kt` — Retrofit client + cross-domain redirect detection + auth state caching

**util/**
- Purpose: Shared utilities and configuration
- Key file: `PreferencesManager.kt` — DataStore persistence for Plex/Jellyfin config, SAF URIs, session tokens

**SettingsActivity.kt**
- Purpose: Phone-side configuration UI (launcher entry point)
- Responsibilities:
  - Configure Plex server URL + token
  - Configure Jellyfin server URL + username/password
  - Add local SAF folders
  - Request permissions
  - Validate Jellyfin auth with `JellyfinRepository.testAndSave()`

**AutoPlayerApp.kt**
- Purpose: Application class with @HiltAndroidApp annotation
- Enables Hilt dependency injection across the app

## Key File Locations

### Entry Points

| File | Type | Purpose |
|------|------|---------|
| `SettingsActivity.kt` | Activity | Phone UI for config (launcher intent) |
| `AutoMediaService.kt` | CarAppService | Android Auto entry point |
| `AutoMediaSession.kt` | Session | Car display lifecycle manager |
| `RootScreen.kt` | Screen | First car screen (source picker) |

### Configuration

| File | Purpose |
|------|---------|
| `AndroidManifest.xml` | Permissions, service/activity declarations, metadata |
| `automotive_app_desc.xml` | Android Auto feature flags |
| `network_security_config.xml` | Cleartext HTTP permission for local servers |
| `PreferencesManager.kt` | DataStore key definitions + persistence methods |

### Core Logic

| File | Purpose |
|------|---------|
| `MediaRepository.kt` | Unified facade routing to LOCAL/PLEX/JELLYFIN |
| `LocalMediaRepository.kt` | MediaStore + SAF scanning |
| `PlexRepository.kt` | Plex API client + media mapping |
| `JellyfinRepository.kt` | Jellyfin API client + auth + media mapping |
| `MediaPlayerManager.kt` | ExoPlayer singleton + surface attachment |
| `VideoSurfaceRenderer.kt` | Car display Surface callback implementation |

### Data Models

| File | Purpose |
|------|---------|
| `MediaItem.kt` | Unified data class (all sources) |
| `MediaSource.kt` | Source enum (LOCAL, PLEX, JELLYFIN) |
| `PlexModels.kt` | Plex API response types |
| `JellyfinModels.kt` | Jellyfin API response + auth types |

### UI Screens

| File | Purpose |
|------|---------|
| `RootScreen.kt` | Source selection (GridTemplate, 3 tiles) |
| `BrowseScreen.kt` | Hierarchical media browser (GridTemplate, async thumbnails) |
| `VideoPlaybackScreen.kt` | Playback controls (NavigationTemplate, ActionStrip) |
| `SettingsActivity.kt` | Phone config UI (EditText fields, buttons) |

## Naming Conventions

### Files

- **Repositories**: `{Source}Repository.kt` (e.g., PlexRepository, JellyfinRepository, LocalMediaRepository)
- **APIs**: `{Service}Api.kt` (e.g., PlexApi, JellyfinApi)
- **Models/DTOs**: `{Source}Models.kt` or `{Service}Models.kt` (e.g., PlexModels, JellyfinModels)
- **Screens**: `{Screen}Screen.kt` (e.g., RootScreen, BrowseScreen, VideoPlaybackScreen)
- **Utilities**: `{Purpose}Manager.kt` (e.g., PreferencesManager, MediaPlayerManager)
- **Activities**: `{Purpose}Activity.kt` (e.g., SettingsActivity)
- **Services**: `{Purpose}Service.kt` (e.g., AutoMediaService)
- **Entry Points**: `{Purpose}EntryPoint.kt` or `{Purpose}App.kt` (e.g., AppEntryPoint, AutoPlayerApp)

### Kotlin Classes and Interfaces

- **Repositories**: Nouns plural or singular (PlexRepository, LocalMediaRepository)
- **APIs**: Ends with Api (PlexApi, JellyfinApi)
- **Screens**: `{Purpose}Screen` (RootScreen, BrowseScreen)
- **Sealed classes** (state models): UpperCamelCase (PlaybackState, SurfaceState, JellyfinSaveState)
- **Data classes**: UpperCamelCase (MediaItem, PlexConfig, JellyfinSession)
- **Enums**: UpperCamelCase (MediaSource, PlaybackState)

### Functions and Variables

- **Function parameters**: camelCase
- **Local variables**: camelCase
- **Tag constants**: `private const val TAG = "ClassName"`
- **State flow fields**: `_privateName` for mutable, `publicName` for immutable exposed flow
- **Lambdas/Higher-order**: Short names (e.g., `onClick`, `onError`)

### Packages

- Hierarchical by domain: `com.pscholer.autoplayer.{domain}.{subdomain}`
- Examples:
  - `com.pscholer.autoplayer.car.screens` — car UI components
  - `com.pscholer.autoplayer.data.remote.plex` — Plex data layer
  - `com.pscholer.autoplayer.data.local` — Local data layer

## Where to Add New Code

### New Feature (Media Source Integration)

**Files to create:**
1. `data/remote/{source}/{Source}Api.kt` — Retrofit interface with API endpoints
2. `data/remote/{source}/{Source}Models.kt` — API response DTOs and auth models
3. `data/remote/{source}/{Source}Repository.kt` — Retrofit client, auth, media mapping

**Files to modify:**
1. `data/MediaSource.kt` — Add enum value
2. `data/MediaRepository.kt` — Add case in `getItems()` switch
3. `util/PreferencesManager.kt` — Add config keys and accessor properties
4. `car/screens/RootScreen.kt` — Add tile for new source
5. `SettingsActivity.kt` — Add config UI (EditText fields)
6. `SettingsViewModel.kt` — Add save/load logic

**Test consideration:** New repository should follow Plex/Jellyfin error handling patterns (empty list on error, specific exception types for auth failures)

### New Screen/UI Flow

**Files to create:**
1. `car/screens/{New}Screen.kt` — Extend Screen, implement onGetTemplate()

**Files to modify:**
1. `car/screens/` — Existing screens that navigate to the new screen call `screenManager.push({New}Screen(...))`
2. `car/AutoMediaSession.kt` — Only if changing the root screen

**Pattern:** Use `EntryPointAccessors` to access singletons (MediaRepository, MediaPlayerManager)

### New Utility/Helper

**Files to create:**
1. `util/{Purpose}.kt` or extend `util/PreferencesManager.kt`

**Package:** `com.pscholer.autoplayer.util.*`

**Scope:** @Singleton if shared state; no annotation if stateless utility

### New Player Feature (Custom Playback Logic)

**File to modify:**
1. `player/MediaPlayerManager.kt` — Add method or extend state flows

**Pattern:**
- Update ExoPlayer via `player.{method}()`
- Propagate state via StateFlow (do NOT call listeners directly)
- Keep async operations off the main thread (use Dispatchers.IO where needed)

### New Permission or Resource

**Files to modify:**
1. `AndroidManifest.xml` — Add <uses-permission>
2. `network_security_config.xml` — If cleartext/cert changes needed
3. `automotive_app_desc.xml` — If declaring new car features
4. `SettingsActivity.kt` — If requesting runtime permission

## Module Dependencies

### Dependency Graph

```
AutoMediaService (CarAppService)
  └─ AutoMediaSession (Session)
      ├─ RootScreen (Screen)
      │   └─ BrowseScreen (uses screenManager.push)
      │       └─ VideoPlaybackScreen (uses screenManager.push)
      │           └─ MediaPlayerManager
      │               └─ ExoPlayer (Media3)
      │
      └─ VideoSurfaceRenderer (SurfaceCallback)
          └─ MediaPlayerManager

SettingsActivity (Activity)
  ├─ SettingsViewModel (ViewModel)
  │   ├─ PreferencesManager
  │   └─ JellyfinRepository (testAndSave)
  └─ PreferencesManager

MediaRepository (Facade)
  ├─ LocalMediaRepository (MediaStore + SAF)
  ├─ PlexRepository (Retrofit + Plex API)
  └─ JellyfinRepository (Retrofit + Jellyfin API)
      └─ PreferencesManager

BrowseScreen / VideoPlaybackScreen
  └─ MediaRepository (via AppEntryPoint)
      └─ {Source}Repository

PlexRepository / JellyfinRepository
  └─ PreferencesManager (for config + auth tokens)

DataStore (Preferences)
  └─ PreferencesManager (consumer)
```

### Cross-Package Dependencies

| Package | Depends On | Purpose |
|---------|-----------|---------|
| `car.*` | `data.*`, `player.*`, `di.*` | Repository, player, DI |
| `data/*` | `data.models`, `util.*` | Data models, config |
| `service.*` | `car.*`, `di.*` | Session, dependency access |
| `util.*` | None | Utility-only, no circular deps |
| `player.*` | None (Media3 only) | Stateless ExoPlayer wrapper |

### Prohibited Dependencies

- **Circular imports**: No package should import from a package that imports from it
- **UI in data layer**: Data repositories must NOT reference Android Context (except where unavoidable, e.g., LocalMediaRepository for MediaStore)
- **Direct ExoPlayer access**: Screens must use MediaPlayerManager, never access player directly (except in MediaPlayerManager's listener)

## Special Directories

### res/
**Generated:** No (hand-written)
**Committed:** Yes

**Contents:**
- `xml/` — Android Auto metadata and network security config
- `layout/` — Phone settings UI layout (DataBinding enabled)
- `drawable/` — Vector drawables for icons
- `values/` — Strings, colors, themes
- `mipmap/` — App launcher icons (adaptive)

### .planning/codebase/
**Generated:** Yes (by GSD mappers)
**Committed:** Yes (documentation)

**Contents:**
- Architecture and structure analysis documents
- Updated when codebase patterns change significantly

### build/
**Generated:** Yes (by Gradle)
**Committed:** No (in .gitignore)

**Contains:**
- Compiled bytecode
- Resource processing outputs
- Generated code (Hilt, DataBinding)

### .gradle/, .idea/
**Generated:** Yes (by Gradle and Android Studio)
**Committed:** No (in .gitignore, or .idea/*.xml selectively)

---

*Structure analysis: 2026-04-17*
