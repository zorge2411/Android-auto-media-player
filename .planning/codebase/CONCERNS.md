# Codebase Concerns

**Analysis Date:** 2026-04-17

## Known Limitations

**NAVIGATION Category Sideload Workaround:**
- Issue: Android Auto enforces MEDIA category for media apps on Play Store, which sandboxes the app into media browser UI with limited Surface access
- Files: `app/src/main/AndroidManifest.xml` (lines 51-67), `app/src/main/java/com/pscholer/autoplayer/service/AutoMediaService.kt` (lines 20)
- Workaround: App declares as NAVIGATION category to gain full-screen SurfaceContainer access for video rendering
- Impact: Play Store publication is blocked; app is sideload-only. Cannot migrate to official app store without accepting MEDIA category restrictions
- Fix approach: This is intentional by design. If Play Store release is required in future, would need to accept media browser UI limitations or find alternative distribution

**Minimum SDK 29 Constraint:**
- Issue: App requires minSdk 29 for scoped storage support and Android Auto API level 2 (SurfaceCallback)
- Files: `app/build.gradle.kts` (line 15)
- Impact: Cannot target devices pre-Android 10 (API 29); excludes ~10-15% of active Android user base
- Reasoning: Non-negotiable for video-on-AA functionality; older APIs lack surface access

**ALLOW_ALL_HOSTS_VALIDATOR:**
- Issue: Security bypass for sideloaded use; bypasses Play Store host signature verification
- Files: `app/src/main/java/com/pscholer/autoplayer/service/AutoMediaService.kt` (line 20)
- Impact: App will not pass Play Store review; only safe for sideload distribution
- Fix approach: Never submit to Play Store with this validator; swapping requires accepting MEDIA category limitations

---

## Technical Debt

**runBlocking() in PreferencesManager (Synchronous DataStore Access):**
- Issue: `PreferencesManager.kt` uses `runBlocking {}` on main thread to synchronously read DataStore preferences
- Files: `app/src/main/java/com/pscholer/autoplayer/util/PreferencesManager.kt` (lines 31, 54, 65, 93)
- Pattern: Properties like `plexConfig`, `jellyfinConfig`, `jellyfinSession`, `safUris` all call `runBlocking`
- Risk: Can cause ANR (Application Not Responding) if called on main thread during slow storage operations
- Impact: Settings UI may freeze briefly when reading preferences; car display may appear unresponsive
- Fix approach: 
  - Convert preference properties to suspend functions
  - Use StateFlow-based cache for frequently accessed values (config, tokens)
  - Lazy-initialize on demand rather than on property access
  - Priority: Medium (low likelihood in practice on modern devices, but best practice violation)

**Silent Exception Suppression in Image Loading:**
- Issue: `BrowseScreen.kt` swallows all thumbnail loading errors without logging
- Files: `app/src/main/java/com/pscholer/autoplayer/car/screens/BrowseScreen.kt` (line 143)
- Code: `catch (_: Exception) { /* silently skip failed thumbnails */ }`
- Risk: Network issues, permission failures, or disk errors go unnoticed
- Impact: Users see fallback icons instead of thumbnails without knowing why; hard to debug
- Fix approach: Log exception details at debug level; could track in analytics if telemetry added
- Priority: Low (graceful fallback exists)

**Cascading Error Silencing in Repository Methods:**
- Issue: All `getItems()`, `getLibraries()`, `getSectionItems()` calls use `.getOrElse { e -> ... log; emptyList() }`
- Files: 
  - `app/src/main/java/com/pscholer/autoplayer/data/remote/jellyfin/JellyfinRepository.kt` (lines 217-237)
  - `app/src/main/java/com/pscholer/autoplayer/data/remote/plex/PlexRepository.kt` (lines 70-103)
  - `app/src/main/java/com/pscholer/autoplayer/data/local/LocalMediaRepository.kt`
- Risk: Authentication failures, network errors, permission denials all return empty lists; UI cannot distinguish between "loading" and "no results"
- Impact: Users see blank screens without error messages; cannot know if connection failed or if source has no media
- Fix approach: Bubble up Result/sealed class to caller so BrowseScreen can display error state; already has `errorMessage` field (line 53) but not used for auth errors
- Priority: Medium (impacts user experience on first connection attempt)

**Missing Consumer for currentItem StateFlow:**
- Issue: `MediaPlayerManager.kt` exposes `currentItem` StateFlow but nothing observes/displays it
- Files: `app/src/main/java/com/pscholer/autoplayer/player/MediaPlayerManager.kt` (line 54)
- Risk: Now-playing metadata is tracked internally but lost to observers
- Impact: Cannot implement now-playing overlay, lockscreen widgets, or media notifications
- Fix approach: Emit to observing screens or create media notification; low priority for MVP
- Priority: Low (MVP feature, nice-to-have for polish)

---

## Security Considerations

**Credentials Stored in Plain Text:**
- Risk: Jellyfin username, password, and auth tokens stored in DataStore (unencrypted shared preferences)
- Files: `app/src/main/java/com/pscholer/autoplayer/util/PreferencesManager.kt` (lines 46-87)
- Sensitive data: `PASSWORD` key (line 48), `TOKEN` keys (Jellyfin line 50, Plex line 27)
- Current mitigation: DataStore is encrypted at rest on modern Android (10+) via EncryptedSharedPreferences
- Recommendation: 
  - Explicitly use `EncryptedSharedPreferences` (not plain DataStore) for auth tokens
  - Store Jellyfin password only in session token; delete on logout
  - Implement token refresh; short-lived tokens reduce compromise window
  - Add account logout/clear credentials button in SettingsActivity
- Priority: High (user credentials at risk on rooted devices or physical access)

**Cleartext HTTP Enabled:**
- Risk: `network_security_config.xml` permits cleartext traffic for all hosts
- Files: `app/src/main/res/xml/network_security_config.xml` (line 9)
- Current mitigation: `getSanitizedUrl()` defaults domain names to https; local IPs use http
- Risk: Domain URLs manually entered as http:// will transmit credentials over cleartext
- Recommendation:
  - Block cleartext entirely; require https:// for remote servers
  - Keep http only for explicitly whitelisted local IP ranges (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
  - Add validation warning in SettingsActivity if user enters http:// for non-local URL
- Priority: High (credentials transmitted unencrypted over public networks)

**Plex Token Handling:**
- Risk: Plex API token embedded directly in stream URLs (e.g., `?X-Plex-Token=token`)
- Files: `app/src/main/java/com/pscholer/autoplayer/data/remote/plex/PlexRepository.kt` (lines 107-108, 128, 136)
- Issue: Token appears in URL logs, device analytics, MediaStore URIs, and screenshot histories
- Recommendation: Use HTTP header-based auth instead of query param where possible; rotate tokens regularly
- Priority: Medium (Plex service-specific; less critical if token is read-only)

**Jellyfin Cross-Domain Redirect Detection:**
- Implementation: `JellyfinRepository.kt` includes proxy interception (lines 80-96)
- Current handling: Throws `CrossDomainRedirectException` with helpful error message
- Coverage: Good for Pangolin/Authentik detection, but does not catch:
  - Redirects within same domain (forwarding proxy)
  - Malicious redirects that claim same host
- Recommendation: Document the limitation; add HSTS pinning for known servers if expanded
- Priority: Low (niche use case; proper server config avoids proxy issues)

---

## Performance Bottlenecks

**Synchronous Thumbnail Prefetch on BrowseScreen:**
- Issue: `BrowseScreen.prefetchThumbnails()` loads all 256x256 bitmaps into memory before rendering
- Files: `app/src/main/java/com/pscholer/autoplayer/car/screens/BrowseScreen.kt` (lines 126-147)
- Problem: With 100 videos, loads ~25-30 MB of Bitmap objects into heap; blocks UI while loading
- Limit: `JellyfinApi.getItems()` defaults to Limit=100 (line 45); grid can request up to 6 visible items while driving
- Impact: 
  - Memory spike during scroll
  - UI lag if thumbnails.size > ~50
  - No pagination (requests all items at once)
- Fix approach:
  - Implement virtualized grid (load thumbnails only for visible items)
  - Add pagination to API calls (first request 10, then lazy-load more)
  - Use thumbnail caching layer (Coil cache is basic)
  - Stream decode (decode on demand, discard offscreen bitmaps)
- Priority: Medium-High (will hit ceiling at 200+ item libraries)

**MediaStore Scan on Every Browse to LOCAL:**
- Issue: `LocalMediaRepository.scanMediaStore()` queries entire device index on every root browse
- Files: `app/src/main/java/com/pscholer/autoplayer/data/local/LocalMediaRepository.kt` (lines 40-87)
- Problem: O(n) scan through all indexed videos; can take 2-5 seconds on device with 1000+ videos
- Impact: Navigating back to Local source root causes visible delay; no caching
- Fix approach:
  - Cache results with versioning (invalidate on MediaStore change)
  - Use MediaStore Change Notification API (ContentObserver) to trigger refresh
  - Implement pagination (first 100, then lazy-load)
  - Add background scan with progress callback
- Priority: Medium (only affects local playback, rare in AA use case)

**No Pagination for Plex/Jellyfin Browsing:**
- Issue: `getItems()` fetches default Limit=100 for Jellyfin (line 45 JellyfinApi); Plex has no limit specified
- Files: `app/src/main/java/com/pscholer/autoplayer/data/remote/jellyfin/JellyfinApi.kt` (line 45)
- Risk: Large libraries (500+ episodes in a season) load entire page at once
- Impact: Network transfer spike, memory pressure, grid rendering lag
- Fix approach:
  - Implement cursor-based pagination UI (load more button)
  - Default to first 50, load next 50 on scroll
  - Add pagination params to MediaRepository interface
- Priority: Low (edge case; most users have <100 items per browse level)

**HTTP Logging Interceptor Always Enabled:**
- Issue: `HttpLoggingInterceptor.Level.BODY` enabled on all Retrofit instances
- Files:
  - `app/src/main/java/com/pscholer/autoplayer/data/remote/jellyfin/JellyfinRepository.kt` (line 104)
  - `app/src/main/java/com/pscholer/autoplayer/data/remote/plex/PlexRepository.kt` (line 59)
- Problem: Logs full request/response bodies (including auth tokens, stream URLs) to Logcat in debug and release builds
- Impact: 
  - Credentials visible in device logs (readable by adb logcat without permissions)
  - Performance overhead from string serialization
  - Sensitive data in crash reports if using remote logging
- Fix approach:
  - Move to debug-only via BuildConfig.DEBUG check
  - Set to BASIC level (headers only) if enabled
  - Never log auth tokens or stream URLs
- Priority: High (credentials leak to logs)

---

## Compatibility Issues

**Android Version-Specific Permissions Logic:**
- Implementation: `SettingsActivity.checkPermissions()` (lines 159-169) and `LocalMediaRepository.scanMediaStore()` (lines 43-48)
- Coverage: Handles API 33+ (READ_MEDIA_VIDEO), pre-33 (READ_EXTERNAL_STORAGE), and pre-29 (deprecated EXTERNAL_CONTENT_URI)
- Known gap: API 30-32 have intermediate permission model (partial access); app uses pre-33 fallback which works but may not show all accessible files
- Mitigation: Acceptable for MVP; add SAF folder picker (already done) as workaround
- Priority: Low (rare API levels; SAF workaround available)

**Car App Library Version Pinned:**
- Files: `app/build.gradle.kts` (line 67-68)
- Version: androidx.car.app:app:1.7.0 (latest as of 2025)
- Risk: If Car App Library bumps minimum Car API level in future release, app may break on older car units
- Mitigation: Monitor release notes; test on real head units before major dependency updates
- Priority: Low (stable library; minimal breaking changes)

**Media3 Version Compatibility:**
- Version: 1.3.1 (lines 71-79 in build.gradle.kts)
- Coverage: Includes HLS, DASH, SmoothStreaming decoders for adaptive bitrate playback
- Known issue: AV1 codec support requires separate module (not included); h.265/HEVC works on API 21+
- Impact: Cannot play AV1-encoded files; modern HDR content may fall back to h.264
- Mitigation: Document supported formats; add AV1 decoder module if needed
- Priority: Low (rare in sideloaded source libraries; user can transcode)

---

## Fragile Areas

**PreferencesManager Initialization Race Condition:**
- Issue: Multiple `runBlocking` calls can race if called from different threads simultaneously
- Files: `app/src/main/java/com/pscholer/autoplayer/util/PreferencesManager.kt`
- Scenario: BrowseScreen reads plexConfig while SettingsActivity saves plexConfig
- Risk: Low in practice (DataStore is thread-safe), but violates coroutine safety guarantees
- Safe modification: Always call preference reads from suspended context (not from property getter)
- Test coverage: No unit tests; manual testing only
- Priority: Medium (unlikely to hit in practice, but violates best practices)

**Surface Lifecycle Edge Cases in VideoSurfaceRenderer:**
- Issue: Rapid orientation changes or car disconnect/reconnect can race Surface callbacks
- Files: `app/src/main/java/com/pscholer/autoplayer/car/surface/VideoSurfaceRenderer.kt` (lines 66-121)
- Scenario: `onSurfaceDestroyed()` called while `onSurfaceAvailable()` still executing
- Current protection: None; relies on Android sequential callback guarantees
- Risk: Player could reference dead Surface, causing playback crash
- Safe modification: Add mutex/synchronized block around activeSurface accesses; verify with configuration change stress testing
- Test coverage: No unit tests; manual car testing only
- Priority: Medium (low probability, but fatal if hit)

**BrowseScreen Navigation Stack Leaks:**
- Issue: Each BrowseScreen push creates new instance with new Coil ImageLoader
- Files: `app/src/main/java/com/pscholer/autoplayer/car/screens/BrowseScreen.kt` (line 127)
- Problem: ImageLoader instances not explicitly released; thumbnails map not cleared on pop
- Impact: With deep navigation (10+ levels), multiple Coil caches in memory
- Safe modification: Cache ImageLoader as Singleton; clear thumbnails in onGetTemplate() before returning, not in init
- Priority: Low (unlikely deep navigation in typical AA use case)

**SettingsActivity Credential Display in Text Fields:**
- Issue: Jellyfin password stored in plain EditText; visible in UI without obfuscation
- Files: `app/src/main/java/com/pscholer/autoplayer/SettingsActivity.kt` (line 137)
- Problem: Password appears in plaintext on phone screen; visible over shoulder
- Recommendation: Use PasswordInputType for Jellyfin password field; mask on display
- Priority: Low (phone-side settings only; acceptable risk)

---

## Scaling Limits

**Single MediaPlayerManager Singleton:**
- Design: One global ExoPlayer instance shared across all screens
- Current limit: Can handle continuous playback/seeking in typical AA use
- Scaling issue: Cannot queue/playlist multiple videos without implementing playlist logic
- Path to scale: Add playlist interface to MediaPlayerManager; extend playback screen to show queue
- Priority: Feature gap, not a bottleneck (MVP does not require playlists)

**No Caching Layer for Remote Metadata:**
- Issue: Each BrowseScreen re-queries Plex/Jellyfin API for items list
- Files: MediaRepository.getItems() (line 24) calls API every time
- Scenario: User navigates Movies → Back → Movies again; makes duplicate requests
- Performance: With slow network (3G), each back/forward takes 2-3 seconds
- Fix approach: Add in-memory cache with TTL (cache for 60s, then refresh)
- Priority: Medium (noticeable lag on slow networks; quick fix)

**Unbounded Thumbnail Cache:**
- Issue: `thumbnails` map in BrowseScreen grows unbounded as user browses
- Files: `app/src/main/java/com/pscholer/autoplayer/car/screens/BrowseScreen.kt` (line 51)
- Scenario: Navigate through 500 different videos; keep 500 bitmaps in memory
- Impact: Eventually hits OOM on low-RAM devices (<2GB)
- Fix approach: Use LRU cache with max size (e.g., 100 bitmaps = ~25MB); rely on Coil disk cache for rest
- Priority: Low (typical session has <50 browses; OOM unlikely)

---

## Dependencies at Risk

**ALLOW_ALL_HOSTS_VALIDATOR Bypass:**
- Risk: Car App Library security check is completely disabled
- Package: androidx.car.app:app:1.7.0
- Migration: Play Store submission requires custom HostValidator
- Alternative: Use signed cert pinning for known car units if expanding beyond sideload
- Priority: Known limitation; acceptable for sideload-only app

**Retrofit2 + Gson with Lenient Mode:**
- Risk: Lenient Gson tolerates malformed JSON; can mask API contract changes
- Files: `app/src/main/java/com/pscholer/autoplayer/data/remote/jellyfin/JellyfinRepository.kt` (line 75)
- Mitigation: Used intentionally to handle Jellyfin API variations; Response<T> wrapper prevents body parse failures
- Recommendation: Add schema validation tests to catch API breaking changes early
- Priority: Low (mitigation in place; acceptable for third-party API)

**OkHttp Logging in Release Builds:**
- Risk: Credentials logged to Logcat in production APK
- Package: com.squareup.okhttp3:logging-interceptor:4.12.0
- Fix: Already noted in Performance section; must move to debug-only
- Priority: High

---

## Missing Functionality

**No Media Notifications / Now-Playing:**
- Gap: App plays video silently; no system notification, lockscreen controls, or media session
- Impact: Cannot control playback from car steering wheel buttons, system media controls, or smartwatch
- Files needed: MediaSessionCompat integration or Media3 session implementation
- Effort: Medium (requires Service-based architecture change)
- Priority: Medium (expected for car media apps)

**No Playback History / Resume Position:**
- Gap: Closing app loses playback position; restarting always starts from 0:00
- Impact: Long videos (movies) require user to manually skip to resume
- Fix: Store (itemId, positionMs) in DataStore on pause; restore on play
- Effort: Low
- Priority: Low (nice-to-have)

**No Subtitle/Audio Track Selection:**
- Gap: ExoPlayer supports multiple tracks, but UI has no selector
- Impact: Cannot switch languages or disable subtitles
- Fix: Add track selection menu to VideoPlaybackScreen
- Effort: Medium
- Priority: Low (edge case; rare in sideloaded sources)

**No Full-Text Search:**
- Gap: Must browse through hierarchy; cannot search for specific video
- Impact: Library with 1000+ items is hard to navigate
- Fix: Add search endpoint to MediaRepository; implement search screen
- Effort: Medium (Plex/Jellyfin support search APIs)
- Priority: Low-Medium (quality of life; MVP does not require)

**No Quality/Bitrate Selection:**
- Gap: Plex/Jellyfin servers support transcoding, but app always requests direct play
- Impact: Cannot adapt to slow networks; streams at full quality regardless of connection
- Fix: Add bitrate/quality selection UI; pass to stream URL
- Effort: Medium
- Priority: Low (MVP assumes stable local network)

---

## Test Coverage Gaps

**No Unit Tests:**
- Status: Repository layer has no automated tests
- Untested logic: 
  - Authentication retry logic in JellyfinRepository.authenticate()
  - Cross-domain redirect detection in OkHttp interceptor
  - MIME type filtering in LocalMediaRepository
  - Error message mapping in SettingsActivity (lines 63-82)
- Risk: Regression when refactoring authentication flow
- Priority: Medium (should add tests before major changes)

**No Integration Tests:**
- Status: No test Plex/Jellyfin servers; cannot verify API parsing
- Gap: Gson parsing against real server responses; header construction
- Priority: Low (manual testing sufficient for sideload)

**No UI/Instrumentation Tests:**
- Status: No Android instrumentation tests
- Untested: Screen navigation, permission flows, surface lifecycle
- Priority: Low (Car App Library testing is complex; manual QA on real head units is standard)

**No Configuration Change Testing:**
- Issue: App may crash on rapid orientation changes (surface recreation)
- Test: Never verified with Configuration.ORIENTATION_LANDSCAPE → ORIENTATION_PORTRAIT cycling
- Priority: Medium (should test on real car with head unit rotation simulation)

**No Low-Memory/GC Stress Testing:**
- Issue: Thumbnail loading and MediaStore scanning not tested under memory pressure
- Risk: OOM crashes on low-RAM devices not caught until production
- Priority: Low (user base likely has mid-range+ devices)

---

## Future Scalability Concerns

**Authentication Token Refresh:**
- Current: Tokens never refresh; long-lived session tokens from initial auth
- Limitation: Plex/Jellyfin session tokens can expire if server restarts or token invalidated
- Scaling issue: Expired token causes silent auth failures (empty results)
- Path: Implement refresh token flow; automatically re-auth on 401
- Priority: Medium (reliability improvement for long-running playback sessions)

**Multiple Media Sources Selection:**
- Current: User can configure all three sources, but app only shows one source at root
- Limitation: RootScreen hard-coded (not shown in provided files, but mentioned in BrowseScreen line 34)
- Path: Display combined root with all configured sources
- Priority: Low (MVP feature parity)

**Multi-User Support:**
- Current: App assumes single Plex/Jellyfin user account per app installation
- Limitation: Family with multiple users must share one account or uninstall/reinstall
- Path: Add user switching in settings; cache per-user tokens
- Priority: Low (edge case; acceptable for sideload)

**Car Unit Pairing:**
- Current: App works with any Android Auto head unit (ALLOW_ALL_HOSTS)
- Limitation: Cannot distinguish between owner's car and valet's; no per-car config
- Path: Implement device pairing via Bluetooth ID or VIN; restrict API access to paired car
- Priority: Very Low (security theater; credentials stored on phone anyway)

---

*Concerns audit: 2026-04-17*
