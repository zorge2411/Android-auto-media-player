# Architecture

**Analysis Date:** 2026-04-17

## Architectural Style

**Single-module Android Auto application** using layered architecture with dependency injection via Hilt.

**Package structure:** `com.pscholer.autoplayer.*`

**Key design decisions:**
- NAVIGATION app category (not MEDIA) to unlock full-screen video rendering
- Sideload workaround: `HostValidator.ALLOW_ALL_HOSTS_VALIDATOR` for unsigned app
- EntryPoint pattern for non-standard Android components (Screens/Sessions)
- Multi-source media abstraction with unified repository interface
- Direct Surface integration between Android Auto and ExoPlayer

## Entry Points

**SettingsActivity** (`com.pscholer.autoplayer.SettingsActivity`)
- Type: Activity (phone-side configuration UI)
- Launcher entry point (Intent.ACTION_MAIN / Intent.CATEGORY_LAUNCHER)
- Responsibilities:
  - Manage Plex/Jellyfin server configuration
  - Request storage permissions (READ_MEDIA_VIDEO)
  - Handle SAF folder picker for local media access
  - Validate Jellyfin credentials with server connectivity check
- ViewModel: `SettingsViewModel` manages state flows for config save operations

**AutoMediaService** (`com.pscholer.autoplayer.service.AutoMediaService`)
- Type: CarAppService (Android Auto entry point)
- Declared with category `NAVIGATION` (sideload workaround for full-screen access)
- Responsibilities:
  - Create and manage the car app session
  - Override `createHostValidator()` to return `ALLOW_ALL_HOSTS_VALIDATOR`
- Note: For Play Store releases, switch to MEDIA category and standard host validator

**AutoMediaSession** (`com.pscholer.autoplayer.car.AutoMediaSession`)
- Type: Session (car display lifecycle manager)
- Responsibilities:
  - Register `VideoSurfaceRenderer` as the car display SurfaceCallback
  - Create root navigation screen (RootScreen)
  - Handle car configuration changes (orientation, DPI)
  - Inject dependencies via `AppEntryPoint` entry point accessor

## Dependency Injection

**Framework:** Hilt with SingletonComponent scope

**Singletons (auto-bound via @Inject constructors):**
- `MediaRepository` — routes requests to appropriate source implementation
- `MediaPlayerManager` — ExoPlayer wrapper and surface attachment
- `PlexRepository` — Plex API client and media mapping
- `JellyfinRepository` — Jellyfin API client with auth handling
- `LocalMediaRepository` — MediaStore and SAF scanning
- `PreferencesManager` — DataStore-backed configuration persistence

**Entry point pattern** (`com.pscholer.autoplayer.di.AppEntryPoint`):
```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun mediaRepository(): MediaRepository
    fun playerManager(): MediaPlayerManager
}
```
Used in Screens/Sessions via:
```kotlin
EntryPointAccessors.fromApplication(carContext.applicationContext, AppEntryPoint::class.java)
```

**AppModule** (`com.pscholer.autoplayer.di.AppModule`):
- Currently empty placeholder
- Use for third-party types (Retrofit, OkHttpClient factories) if needed
- Note: All local services use constructor injection (Hilt auto-binds)

## Data Flow

### Browse → Play Pipeline

```
RootScreen (MediaSource picker)
  ↓
BrowseScreen (parentId=null)  [loads library roots via MediaRepository]
  ↓
BrowseScreen (parentId=X)      [loads child items]
  ↓
VideoPlaybackScreen            [starts playback via MediaPlayerManager]
  ↓
MediaPlayerManager.playWithHeaders() or .play()
  ↓
ExoPlayer renders → VideoSurfaceRenderer.setVideoSurface()
  ↓
Car display Surface (from SurfaceContainer)
```

### Media Source Abstraction

**MediaRepository** (`com.pscholer.autoplayer.data.MediaRepository`):
- Unified facade abstracting three source types
- `suspend fun getItems(source: MediaSource, parentId: String?): List<MediaItem>`
- Routes calls to appropriate implementation based on source enum

**MediaSource** (`com.pscholer.autoplayer.data.MediaSource`):
```kotlin
enum class MediaSource(val displayName: String) {
    LOCAL("Local Files"),           // MediaStore + SAF
    PLEX("Plex"),                   // Plex API
    JELLYFIN("Jellyfin")            // Jellyfin API
}
```

**Per-source semantics:**
- **LOCAL**: `parentId=null` → MediaStore scan + SAF folders; non-null → specific SAF directory
- **PLEX**: `parentId=null` → library sections; non-null → section items or children
- **JELLYFIN**: `parentId=null` → user views; non-null → items under parent

### Local Media Discovery

**LocalMediaRepository** scans in two phases:

1. **MediaStore scan** (all indexed videos):
   - Uses `MediaStore.Video.Media` with VOLUME_EXTERNAL collection (API 29+)
   - Projects: ID, DISPLAY_NAME, DURATION, MIME_TYPE
   - Filtered to video/* MIME types

2. **SAF folder trees** (user-granted access):
   - User picks directories via Intent.ACTION_OPEN_DOCUMENT_TREE
   - URI persisted with takePersistableUriPermission()
   - Recursive walk via DocumentFile.listFiles()
   - Supports: USB OTG, custom folders, scoped storage bypass

### Plex Integration

**PlexRepository** (`com.pscholer.autoplayer.data.remote.plex.PlexRepository`):
- Retrofit HTTP client with request interceptor:
  - Injects X-Plex-Client-Identifier, X-Plex-Product, X-Plex-Version, X-Plex-Platform headers
  - HttpLoggingInterceptor at BASIC level
- URL sanitization: infers `http://` for private IPs, `https://` for domain names
- API endpoints:
  - `/library/sections` — list movie/show libraries
  - `/library/sections/{id}/all` — browse section items
  - `/library/metadata/{key}/children` — traverse hierarchy (seasons, episodes)
- Stream URLs: `{serverUrl}{part.key}?X-Plex-Token={token}` (direct play, no transcode)

### Jellyfin Integration

**JellyfinRepository** (`com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository`):
- Lenient Gson (tolerates minor JSON deviations)
- Cross-domain redirect detector interceptor:
  - Catches auth proxy redirects (Pangolin, Authentik, Authelia)
  - Throws `CrossDomainRedirectException` with actionable error message
  - Prevents HTML page parsing errors
- URL sanitization: same private IP heuristics as Plex
- Auth flow:
  1. `testAndSave()` — verify credentials before persisting
  2. HTTP response inspection before Gson parsing (catches HTML responses)
  3. Session token cached; reused on subsequent calls
  4. Auto re-authenticate if token expired
- API endpoints:
  - `/Users/AuthenticateByName` — POST login
  - `/Users/{userId}/Views` — user views (Movies, TV Shows, etc.)
  - `/Users/{userId}/Items` — browse items with query filters
- Stream URLs: `{serverUrl}/Videos/{id}/stream?static=true&api_key={token}` (direct stream, no transcode)

### Configuration Persistence

**PreferencesManager** (`com.pscholer.autoplayer.util.PreferencesManager`):
- Backed by DataStore<Preferences> (modern successor to SharedPreferences)
- Stores:
  - **Plex**: serverUrl, token
  - **Jellyfin**: serverUrl, username, password (config); userId, token (session)
  - **SAF URIs**: set of tree URIs for persistent folder access
- Blocking reads (`.first()`) — called on main thread; acceptable for app startup

## Media Playback Pipeline

### Surface Management

**VideoSurfaceRenderer** (`com.pscholer.autoplayer.car.surface.VideoSurfaceRenderer`):
- Implements `SurfaceCallback` interface
- Lifecycle:
  1. `onSurfaceAvailable()` — receives car display Surface from SurfaceContainer
  2. Immediately wires Surface to ExoPlayer via `playerManager.setVideoSurface()`
  3. `onVisibleAreaChanged()` / `onStableAreaChanged()` — notifies player of safe zones (optional letterboxing)
  4. `onSurfaceDestroyed()` — detaches Surface before release
  5. `onConfigurationChanged()` — re-attaches Surface after orientation change
- Exposes state flow: `StateFlow<SurfaceState>` for screens to observe availability
- Critical: Without `androidx.car.app.ACCESS_SURFACE` permission, `SurfaceContainer.surface` is null

### ExoPlayer Wrapper

**MediaPlayerManager** (`com.pscholer.autoplayer.player.MediaPlayerManager`):
- Singleton ExoPlayer instance with:
  - Audio focus handling (LOSS → pause, LOSS_TRANSIENT → pause, DUCK on nav instructions)
  - Audio becoming noisy detection (auto-pause on headphone unplug)
  - Content type: AUDIO_CONTENT_TYPE_MOVIE for video
  - Usage: USAGE_MEDIA for standard media classification
- Playback methods:
  - `play(uri, mimeType)` — simple URL playback with optional MIME type
  - `playWithHeaders(uri, headers)` — Plex/Jellyfin auth headers via DefaultHttpDataSource
- Playback control: togglePlayPause(), seekForward(ms), seekBack(ms), seekTo(pos)
- State flows:
  - `playbackState: StateFlow<PlaybackState>` — Idle, Buffering, Ready, Ended, Error
  - `isPlaying: StateFlow<Boolean>` — current play state
  - `currentItem: StateFlow<NowPlaying?>` — title and stream URL
- Player listener observes ExoPlayer state changes and propagates via flows

### Playback Screen Composition

**VideoPlaybackScreen** (`com.pscholer.autoplayer.car.screens.VideoPlaybackScreen`):
- Template: NavigationTemplate (maximizes visible video area)
- Layers:
  1. **Background**: ExoPlayer video rendering to car display Surface
  2. **Overlay**: NavigationTemplate with ActionStrip + MapActionStrip
- Controls:
  - **ActionStrip** (right edge): Seek Back | Play/Pause | Seek Forward
  - **MapActionStrip** (top-right): Stop button
- Re-renders template on `isPlaying` state change (swaps play/pause icon)
- Starts playback on screen init (calls `playerManager.play()`)

## Sideload Workarounds

### NAVIGATION Category (Instead of MEDIA)

**Why:** 
- MEDIA apps are sandboxed into Android Auto's media browser chrome
- NAVIGATION apps receive full-screen SurfaceContainer access
- Used by Fermata Auto, SyncPlayer, and other sideloaded video players

**Implementation** (`AndroidManifest.xml`):
```xml
<service android:name=".service.AutoMediaService" android:exported="true">
    <intent-filter>
        <action android:name="androidx.car.app.CarAppService" />
        <category android:name="androidx.car.app.category.NAVIGATION" />  <!-- Not MEDIA -->
    </intent-filter>
</service>
```

**Caveat:** Play Store releases must use MEDIA category and accept media-browser restrictions.

### ALLOW_ALL_HOSTS_VALIDATOR

**Why:** 
- Play Store apps must be signed and registered with Android Auto host
- Sideloaded unsigned apps are rejected by standard host validator
- Bypass needed for development/sideload

**Implementation** (`AutoMediaService.kt`):
```kotlin
override fun createHostValidator(): HostValidator =
    HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
```

**Caveat:** Never use for production Play Store builds.

### Cleartext HTTP Permission

**Why:**
- Local Plex/Jellyfin servers often run on private IPs (192.168.x.x, 10.x.x.x)
- HTTP not HTTPS on LAN is common
- getSanitizedUrl() defaults to https:// for domain names; http:// for private IPs

**Implementation** (`network_security_config.xml`):
```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors><certificates src="system" /></trust-anchors>
    </base-config>
</network-security-config>
```

## Cross-Cutting Concerns

### Error Handling

**Per-layer strategy:**
- **Repository methods**: Return empty list on error (graceful degradation)
  - Log via `Log.e(TAG, "Operation failed", exception)`
  - Screens show "No media found" or error message
- **Jellyfin auth**: Type-safe exception matching in SettingsViewModel
  - `CrossDomainRedirectException` detected and mapped to proxy guidance
  - HTML response detection in authenticate() (proxy interception)
  - Cleartext/timeout/401 errors with specific guidance in error toast
- **Playback**: ExoPlayer.Listener propagates errors via `playbackState` StateFlow
  - `PlaybackState.Error(message, errorCode)` captured in screen state

### Logging

**Framework:** Android Log (Log.d/i/w/e)

**Conventions:**
- Each class has `companion object { private const val TAG = "ClassName" }`
- Info level: User-facing events (auth success, surface attachment, stream start)
- Debug level: Component lifecycle (screen push/pop, configuration changes)
- Warning level: Recoverable issues (permission denied, SAF tree open failure)
- Error level: Failures with stack traces (API errors, playback exceptions)

### Permissions

**Runtime permissions:**
- READ_MEDIA_VIDEO (API 33+) / READ_EXTERNAL_STORAGE (pre-33) — local media scan
- INTERNET — Plex/Jellyfin API requests
- FOREGROUND_SERVICE_MEDIA_PLAYBACK — playback service (declared, not requested)
- ACCESS_SURFACE — critical for video rendering (declared, not requested)
- NAVIGATION_TEMPLATES — full-screen navigation screens (declared, not requested)

**Storage access:**
- MediaStore automatic via READ_MEDIA_VIDEO
- SAF folders: user grants persistable URI permission via Intent.ACTION_OPEN_DOCUMENT_TREE

---

*Architecture analysis: 2026-04-17*
