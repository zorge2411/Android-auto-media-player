# External Integrations

**Analysis Date:** 2026-04-17

## APIs & External Services

### Plex Media Server

**Purpose:** Browse and stream movies/TV shows from a Plex library

**Implementation:**
- API Client: `com.pscholer.autoplayer.data.remote.plex.PlexApi` (Retrofit HTTP interface)
- Repository: `com.pscholer.autoplayer.data.remote.plex.PlexRepository`
- Models: `com.pscholer.autoplayer.data.remote.plex.PlexModels`

**Key Endpoints:**
- `GET /library/sections` - List all library sections (Movies, TV Shows, Music, etc.) identified by `type` field
- `GET /library/sections/{sectionId}/all` - List all items in a library section with pagination (`X-Plex-Container-Start`, `X-Plex-Container-Size`)
- `GET /library/metadata/{ratingKey}/children` - Get children (seasons under show, episodes under season, etc.)

**Authentication:**
- Header-based token: `X-Plex-Token: <token>` on every request
- Token is user-provided during setup and persisted via `PreferencesManager`
- Configuration stored as `PlexConfig` containing `serverUrl` and `token`

**Direct Playback:**
- URL construction (client-side): `{serverUrl}{part.key}?X-Plex-Token={token}`
- Example: `http://192.168.1.10:32400/library/parts/12345/file.mkv?X-Plex-Token=abc123`
- No transcoding; streams original file format
- Token appended as query parameter for authentication

**Data Model:**
- `PlexSection` - Library section with key, title, type (movie/show/music), thumb, art
- `PlexMetadata` - Media item (movie/show/season/episode) with:
  - `ratingKey` - Unique identifier
  - `type` - "movie", "show", "season", or "episode"
  - `parentTitle` / `grandparentTitle` - Hierarchy (episode → season → show)
  - `index` / `parentIndex` - Episode/season numbering
  - `duration` - Milliseconds
  - `Media` array with codec/container info and `Part` objects containing playback URL
- `PlexPart` - Individual media file with key (playback path) and file size

**Client Identification Headers (every request):**
- `X-Plex-Client-Identifier: auto-player-android-auto`
- `X-Plex-Product: Auto Player`
- `X-Plex-Version: 1.0`
- `X-Plex-Platform: Android`
- `Accept: application/json`

**Configuration:**
- Server URL stored as preference, sanitized to include scheme:
  - Local IPs (192.168.x, 10.x, 172.16-31.x) default to `http://`
  - Domain names default to `https://`
- URL cached to avoid rebuilding Retrofit client on every request

### Jellyfin Media Server

**Purpose:** Browse and stream movies/TV shows from a Jellyfin library

**Implementation:**
- API Client: `com.pscholer.autoplayer.data.remote.jellyfin.JellyfinApi` (Retrofit HTTP interface)
- Repository: `com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository`
- Models: `com.pscholer.autoplayer.data.remote.jellyfin.JellyfinModels`

**Key Endpoints:**
- `POST /Users/AuthenticateByName` - Authenticate with username/password, returns access token and user ID
- `GET /Users/{userId}/Views` - Get top-level views (Movies, TV Shows, Collections, etc.)
- `GET /Users/{userId}/Items` - Get items with filtering by parent, type (Movie/Series/Episode/Season), pagination, sort options

**Authentication Flow:**
1. Client calls `POST /Users/AuthenticateByName` with username/password
2. Server returns `AccessToken` and `User.Id`
3. Subsequent requests use: `X-Emby-Token: <accessToken>` header
4. Credentials stored in DataStore as `JellyfinConfig` (serverUrl, username, password)
5. Session token cached separately in `JellyfinSession` (userId, token) for fast lookup
6. On token expiry, auto-re-authenticate using stored credentials

**X-Emby-Authorization Header Format (for initial auth):**
```
X-Emby-Authorization: MediaBrowser Client="AutoPlayer", Device="Android", DeviceId="auto-player-001", Version="1.0"
```

**Direct Stream URL (client-side construction):**
- Static playback (original format): `{serverUrl}/Videos/{itemId}/stream?static=true&api_key={token}`
- Transcoded fallback: `{serverUrl}/Videos/{itemId}/stream.mp4?api_key={token}&...transcode_params`
- Token appended as `api_key` query parameter

**Data Model:**
- `JellyfinItem` - Media item with:
  - `id` - Unique identifier
  - `type` - "Movie", "Series", "Season", "Episode", "CollectionFolder", "UserView", "BoxSet"
  - `name` - Display title
  - `indexNumber` / `parentIndexNumber` - Episode # / Season #
  - `seriesName` / `seasonName` - Hierarchy labels
  - `runtimeTicks` - Duration in 10,000,000 ticks/second (divide by 10,000 for milliseconds)
  - `imageTags` - Map of image type → tag ID for image URL construction
  - `mediaSources` - Array of media source objects with codec and streaming support flags
- `JellyfinMediaSource` - Media format info with `supportsDirectPlay` and `supportsDirectStream` booleans
- `JellyfinAuthResponse` - Login response containing `accessToken` and `User` object

**Thumbnail URL Construction:**
```
{serverUrl}/Items/{itemId}/Images/Primary?tag={imageTags["Primary"]}&maxWidth=256
```

**Error Handling for Auth Proxies:**
- Detects cross-domain redirects (e.g., Pangolin, Authentik, Authelia proxies)
- `CrossDomainRedirectException` thrown if request to expected host redirects to different host
- Provides user-friendly guidance to use direct Jellyfin URL or enable "Extended compatibility" in proxy dashboard
- Checks Content-Type for HTML responses when expecting JSON (indicates proxy error page)

**Configuration:**
- Server URL sanitized same as Plex (local IPs → http, domains → https)
- Lenient Gson deserialization to tolerate minor JSON format deviations

## Data Storage

### Local Configuration Storage

**Storage Method:**
- `androidx.datastore:datastore-preferences` - Encrypted key-value store (Protocol Buffers)
- File: `autoplayer_prefs` (in app-specific directory, encrypted on API 24+)
- Manager: `com.pscholer.autoplayer.util.PreferencesManager`

**Persisted Configuration:**

**Plex:**
- `plex_server_url` - Server URL (e.g., `http://192.168.1.10:32400`)
- `plex_token` - API token for authentication

**Jellyfin:**
- `jf_server_url` - Server URL
- `jf_username` - Username for re-authentication
- `jf_password` - Password for re-authentication (stored plaintext in local encrypted storage)
- `jf_user_id` - Cached user ID from last successful auth
- `jf_token` - Cached access token from last successful auth

**Local Media:**
- `saf_uris` - Set of Storage Access Framework (SAF) tree URIs for external/OTG drives

### Local Media (Device Storage)

**MediaStore Scanning:**
- Scans device's built-in video library (Movies, DCIM, Downloads, etc.)
- Uses `MediaStore.Video.Media` content provider
- API 29+: Volume-aware via `MediaStore.VOLUME_EXTERNAL` collection
- Supported MIME types: mp4, mkv, webm, avi, mov, 3gpp, flv, wmv
- Returns flat list of `MediaItem` with content:// URIs for direct playback
- Location: `com.pscholer.autoplayer.data.local.LocalMediaRepository.scanMediaStore()`

**Storage Access Framework (SAF) Trees:**
- Provides access to USB-OTG drives and user-granted folders outside MediaStore scope
- User grants access via `Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)`
- App persists URI with `takePersistableUriPermission()` for long-term access
- Recursive directory walk to enumerate video files
- Location: `com.pscholer.autoplayer.data.local.LocalMediaRepository.scanSafDirectory()`

**Document File Access:**
- Uses `androidx.documentfile:documentfile` for traversing SAF tree URIs
- Filters by MIME type to find playable video files

## Android Media & Playback

### Media3 / ExoPlayer Integration

**Purpose:** Video and audio playback engine with streaming support

**Implementation:**
- Singleton: `com.pscholer.autoplayer.player.MediaPlayerManager`
- Instance: `ExoPlayer` initialized with audio attributes and lifecycle listeners
- Builds `ProgressiveMediaSource` for files, `DefaultHttpDataSource` for HTTP streams

**Media Sources Supported:**
- Local files (MediaStore URIs, SAF URIs) - Progressive download
- Plex Direct Play (HTTP .mkv/.mp4/etc) - Progressive download via OkHttp
- Jellyfin Direct Stream (HTTP video/mp4) - Progressive download via OkHttp
- HLS streams (via `media3-exoplayer-hls`)
- DASH streams (via `media3-exoplayer-dash`)
- Smooth Streaming (via `media3-exoplayer-smoothstreaming`)

**Audio Configuration:**
- Content Type: `AUDIO_CONTENT_TYPE_MOVIE` (video/movie content)
- Usage: `USAGE_MEDIA` (standard foreground media)
- Focus Handling: Media3 auto-manages Android AudioManager focus:
  - LOSS → pause (phone call, another app)
  - LOSS_TRANSIENT → pause (brief interruption)
  - LOSS_TRANSIENT_CAN_DUCK → lower volume 30% (navigation voice)
  - GAIN → resume/restore volume automatically
- Headphone disconnect handling enabled (auto-pause on unplug/BT disconnect)

**HTTP Data Source Configuration:**
- Uses OkHttp client for HTTP downloads via `media3-datasource-okhttp`
- Allows custom headers (e.g., `X-Plex-Token`, `X-Emby-Token`) on requests

**Playback State Tracking:**
- State flow: `playbackState` (Idle, Buffering, Ready, Ended, Error)
- Playing indicator: `isPlaying` boolean state flow
- Current item: `currentItem` (NowPlaying object) state flow
- Error reporting: `PlaybackState.Error` with code and message

**Surface Management:**
- Android Auto video surface callback support (for car display rendering)
- Tracks active `Surface` and visible area (`Rect`) for video rendering

## Android Auto / Car App Library

### Navigation vs. Media Category

**Architecture Choice:** NAVIGATION category (not MEDIA)

**Rationale (per AndroidManifest.xml):**
- Navigation apps receive full-screen `SurfaceContainer` access for custom UI
- Media apps are sandboxed into AA's standard media browser UI with limited control
- Sideload workaround used (similar to Fermata Auto) to bypass media-browser restrictions
- Enables full-screen video playback on car display

**Implementation:**
- Service: `com.pscholer.autoplayer.service.AutoMediaService` (extends `CarAppService`)
- Intent filter: `androidx.car.app.CarAppService` action with `androidx.car.app.category.NAVIGATION` category
- Exported service with `foregroundServiceType="mediaPlayback"`

**Car App API Level:**
- Minimum: API level 2 (introduces SurfaceCallback / SurfaceContainer for video surface access)
- Required permission: `androidx.car.app.ACCESS_SURFACE` (grants access to car display Surface object)
- Without this permission, `SurfaceCallback.onSurfaceAvailable()` fires but `getSurface()` returns null

**Car Display Integration:**
- Phone connects to car head unit via Android Auto (wireless or USB)
- App runs on phone, UI projected to car display via `app-projected` library
- Car App Service handles all car-side events and surface rendering

## HTTP & Network Configuration

**HTTP Client (OkHttp 4.12.0):**
- Shared instance per media source (Plex / Jellyfin)
- Logging interceptor with BASIC level (headers, status, request/response lines)
- Custom header injection for authentication (X-Plex-Token, X-Emby-Authorization, X-Emby-Token)
- Timeout and connection pooling managed by OkHttp defaults

**Network Connectivity Check:**
- Permission: `android.permission.ACCESS_NETWORK_STATE` - Check device network status
- Permission: `android.permission.INTERNET` - Network access required

**Network Security Configuration:**
- File: `app/src/main/res/xml/network_security_config` (referenced in AndroidManifest.xml)
- Likely contains cleartext traffic policy (permits http:// for local IPs)
- May include certificate pinning for remote domains

**JSON Serialization (Gson):**
- Retrofit converter: `com.squareup.retrofit2:converter-gson`
- Plex: Standard Gson with `@SerializedName` annotations (case-sensitive field mapping)
- Jellyfin: Lenient Gson mode for minor JSON deviations
- Response wrapping: Retrofit `Response<T>` for Jellyfin auth to handle HTML error pages gracefully

## Permissions

### Storage Access

- `READ_MEDIA_VIDEO` (API 33+) - Read video files from MediaStore
- `READ_EXTERNAL_STORAGE` (API 29-32 fallback) - Pre-API 33 video access
- Scoped storage support enforced (API 29+)

### Network

- `INTERNET` - Network requests to media servers
- `ACCESS_NETWORK_STATE` - Check network connectivity

### Android Auto

- `androidx.car.app.ACCESS_SURFACE` - Critical: access to car display Surface object
- `androidx.car.app.NAVIGATION_TEMPLATES` - Navigation template support
- `FOREGROUND_SERVICE` - Background playback service
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - Foreground service type for playback
- `WAKE_LOCK` - Keep device active during playback

---

*Integration audit: 2026-04-17*
