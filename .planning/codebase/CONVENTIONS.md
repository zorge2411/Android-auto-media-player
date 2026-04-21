# Coding Conventions

**Analysis Date:** 2026-04-17

## Naming Patterns

**Files:**
- Kotlin files use PascalCase for class/object names: `AutoPlayerApp.kt`, `MediaRepository.kt`, `LocalMediaRepository.kt`
- Model/data classes grouped with shared responsibility (e.g., `JellyfinModels.kt` contains all Jellyfin DTOs)
- API interfaces: `JellyfinApi.kt`, `PlexApi.kt`
- Repository classes follow `[ServiceName]Repository.kt` pattern: `PlexRepository.kt`, `JellyfinRepository.kt`
- Utility/helper classes: `PreferencesManager.kt`, `VideoSurfaceRenderer.kt`
- Screen classes: `RootScreen.kt`, `BrowseScreen.kt`, `VideoPlaybackScreen.kt`

**Package Structure:**
- Deeply nested by domain: `com.pscholer.autoplayer.data.remote.jellyfin`, `com.pscholer.autoplayer.car.screens`
- Pattern: `[domain].[subdomain].[responsibility]`
- Separates concerns clearly: `data.local`, `data.remote.jellyfin`, `data.remote.plex`, `car.screens`, `car.surface`

**Classes:**
- PascalCase: `MediaRepository`, `JellyfinRepository`, `VideoPlaybackScreen`
- Data classes: `MediaItem`, `JellyfinAuthRequest`, `JellyfinAuthResponse`
- Sealed classes for type-safe unions: `sealed class PlaybackState { object Idle : PlaybackState() ... }`
- Companion objects for constants: `companion object { private const val TAG = "ClassName" }`

**Functions/Methods:**
- camelCase for all functions: `scanMediaStore()`, `getItems()`, `prefetchThumbnails()`, `buildGridItem()`
- Private functions prefixed with underscore convention for backing properties: `_playbackState` (MutableStateFlow), `playbackState` (public StateFlow)
- Builder pattern used for complex objects: `MediaItem.Builder()`, `Action.Builder()`
- Extension function pattern for transformations: `JellyfinItem.toMediaItem()` (extension method on data class)

**Variables:**
- camelCase for all variables: `mediaItem`, `thumbnailUrl`, `isFolder`, `errorMessage`
- Backing field pattern: `private val _playbackState` with `val playbackState: StateFlow` getter
- Mutable collections use specific names: `results: MutableList`, `thumbnails: MutableMap`
- Boolean variables clearly indicate state: `isLoading`, `isPlaying`, `isFolder`, `isSuccessful`

**Constants:**
- UPPER_SNAKE_CASE in companion objects: `const val TAG = "ClassName"`, `const val CLIENT_HEADER = "..."`
- SUPPORTED_MIME for sets of constants: `val SUPPORTED_MIME = setOf("video/mp4", ...)`

**Types/Interfaces:**
- Data classes: `data class MediaItem(...)`
- Sealed hierarchies: `sealed class PlaybackState`
- Objects for singletons: `object Jellyfin { ... }`
- Enum-like objects: `object MediaSource { ... }`

## Code Style

**Formatting:**
- No explicit linting config detected; follows Kotlin conventions by default
- Indentation: 4 spaces (standard Kotlin/Android)
- Line length: ~100 chars (some lines reach ~150 in comments)
- Function parameters on same line if short; new lines for multi-line signatures

**Null Safety:**
- Extensive use of nullable types with `?` operator: `String?`, `List<T>?`
- Safe calls with `?.`: `item.thumbnail?.let { ... }`
- Non-null assertion `!!` used only after explicit null checks: `api!!.getViews()` (after `ensureAuthenticated()`)
- Elvis operator `?:` for defaults: `p[Plex.SERVER_URL] ?: ""`, `error.message ?: "Unknown error"`
- `?.let {}` block pattern for optional transformations
- `?.use {}` for resource management (Cursor, Response bodies): `cursor?.use { ... }`, `response.body?.close()`

**Kotlin Idioms:**
- Extension functions for transformations: `private fun JellyfinItem.toMediaItem(): MediaItem`
- Data classes with default parameters: `data class MediaItem(id: String, title: String, isFolder: Boolean = false, ...)`
- String interpolation with template: `"Auth HTTP ${httpResponse.code()}: $errorBody"`
- String building: `buildString { append(...) if (isNotEmpty()) append(...) }`
- When expressions for exhaustive branching:
  ```kotlin
  when (source) {
      MediaSource.LOCAL -> { ... }
      MediaSource.PLEX -> { ... }
      MediaSource.JELLYFIN -> { ... }
  }
  ```
- Scope functions: `player.apply { ... }` for bulk state changes
- `runCatching { ... }.getOrElse { e -> ... }` for error handling in suspend functions
- Sealed classes for type-safe state: `sealed class PlaybackState { object Idle : PlaybackState() ... }`

## Import Organization

**Order (observed):**
1. Android framework imports: `import android.*`
2. AndroidX framework imports: `import androidx.*`
3. Jetbrains/Kotlin stdlib imports: `import kotlinx.*`, `import kotlin.*`
4. Third-party libraries: `import com.google.*, import com.squareup.*, import retrofit2.*, import okhttp3.*`
5. Project-internal imports: `import com.pscholer.autoplayer.*`
6. Specific class imports before wildcard imports (no wildcard imports observed)

**Path Aliases:**
- No path aliases detected (no gradle-level import aliases configured)
- Fully qualified imports used throughout: `com.pscholer.autoplayer.data.MediaRepository`

## Error Handling

**Primary Pattern:**
- `runCatching { ... }.getOrElse { e -> Log.e(...); emptyList() }` — Returns empty collection on failure
  - Used in: `PlexRepository.getLibraries()`, `JellyfinRepository.getViews()`, `JellyfinRepository.authenticate()`
  - Logs the exception with `Log.e(TAG, "Operation failed", e)` then returns safe default

**Exception Throwing:**
- Custom exceptions extend from standard types: `class CrossDomainRedirectException(...) : IOException(...)`
  - Must extend `IOException` so OkHttp interceptors propagate correctly through dispatcher thread
  - Includes helpful error message for user display

**Validation Errors:**
- `IllegalStateException` thrown with descriptive message when validation fails:
  ```kotlin
  throw IllegalStateException(
      "Jellyfin authentication failed — check server URL and credentials in settings"
  )
  ```
- Used in `JellyfinRepository.testAndSave()` and `JellyfinRepository.ensureAuthenticated()`

**HTTP Response Validation:**
- Check `response.isSuccessful` first
- If not successful, extract error body: `httpResponse.errorBody()?.string() ?: "(empty error body)"`
- Check for HTML content: `contentType.contains("text/html", ignoreCase = true)` — indicates auth proxy redirect
- Provide user-actionable error messages referencing specific misconfiguration scenarios

**Logging Strategy:**
- `Log.e(TAG, "message", exception)` for caught exceptions with context
- `Log.i(TAG, "message")` for informational milestones (successful auth, playback start)
- `Log.w(TAG, "message")` for warnings (blank URLs, missing config)
- `Log.d(TAG, "message")` for debug info (detailed state transitions)
- No stack traces logged unless exception passed to Log method

## Logging

**Framework:** Android's `android.util.Log`

**Patterns:**
- Every class has a companion object with `private const val TAG = "ClassName"`
- TAG used consistently in all log calls
- Informational logs: `Log.i(TAG, "Authenticated as ${authBody.user.name}")`
- Error logs always include exception: `Log.e(TAG, "Operation failed", e)`
- Debug logs for intermediate state: `Log.d(TAG, "Attaching video surface")`
- Warning logs for recoverable issues: `Log.w(TAG, "Authenticate called but server URL is blank")`
- No structured logging; simple string messages with context variable interpolation

## Comments

**Documentation Style:**
- Block comments using `/** ... */` for class/function documentation
- Example: `/** Start playback of a simple URL with optional MIME type. ... */`
- Multi-line comments preserve formatting: explanations of auth header format, data structure semantics

**Comment Frequency:**
- Moderate; comments explain WHY, not WHAT
- Complex sections include multi-line explanatory blocks before code
- ASCII art diagrams used to visualize architecture:
  ```kotlin
  /**
   * ┌────────────────────────────────────────────────────────┐
   * │ Audio focus configuration...                           │
   * └────────────────────────────────────────────────────────┘
   */
  ```

**Inline Comments:**
- Limited inline comments; clear code is preferred
- Used for non-obvious parameter meanings: `/* handleAudioFocus = */ true`
- Used for critical logic: `// Re-attach surface in case it was set before this call`

**Comment Sections:**
- Divider comments separate logical sections: `// ─────────────────────────────`
- Section headers: `// ── Playback state ──────────────────────────────────────────`
- Clear visual hierarchy for large classes

**Data Class Documentation:**
- @SerializedName annotations documented in comments when mapping is non-obvious
- Example: `@SerializedName("RunTimeTicks") val runtimeTicks: Long? = null,   // 10,000,000 ticks/sec`

## Function Design

**Size:**
- Small focused functions (10-40 lines typical)
- Longer functions (50-100 lines) used only for complex state machines or initialization
- Example: `JellyfinRepository.buildApi()` is 50 lines for Retrofit setup with interceptor logic

**Parameters:**
- Minimal parameters (1-4 typical)
- No primitive type wrappers; use Kotlin types directly
- Suspend functions used extensively: `suspend fun getItems(...): List<MediaItem>`
- Builder pattern for complex construction: `MediaItem.Builder()...build()`
- Named parameters used when calling functions with multiple boolean/string arguments

**Return Values:**
- Explicit return types on all public functions
- Collection return types: `List<MediaItem>`, `Map<String, String>`, `Set<Uri>`
- Nullable returns when value might not exist: `String?`, `NowPlaying?`
- Empty collections returned instead of null for lists: `emptyList()`, `emptyMap()`
- StateFlow for reactive state: `val playbackState: StateFlow<PlaybackState>`

**Access Modifiers:**
- Default to private; explicitly mark public only when needed
- Private functions in repositories: `private fun getSanitizedUrl()`, `private fun buildRetrofit()`
- Companion objects mark private constants: `private const val TAG`
- Lazy initialization for expensive objects: `private val repo: MediaRepository by lazy { ... }`

## Module Design

**Exports:**
- Classes exported from package (public by default)
- Internal objects used in companion blocks: `private object Jellyfin { val SERVER_URL = ... }`
- Sealed classes exported for type-safe union handling
- No barrel exports or re-exports observed (each file exports its primary class)

**Separation of Concerns:**
- Repositories handle data access; screens handle UI
- Models (in `data.models`) are pure data with no business logic
- Extensions defined near their primary type: `JellyfinItem.toMediaItem()` in JellyfinRepository.kt
- Factories/builders for complex objects: `buildRetrofit()`, `buildGridItem()`

**Dependency Injection:**
- Constructor injection with `@Inject annotation: `@Inject constructor(...)`
- Qualifier: `@ApplicationContext private val context: Context`
- `@Singleton` marker for single-instance objects
- Lazy injection in screens: `private val playerManager: MediaPlayerManager by lazy { EntryPointAccessors... }`
- Hilt entry points for Screen access to singletons: `AppEntryPoint::class.java`

**State Management:**
- MutableStateFlow for reactive state: `private val _playbackState = MutableStateFlow<PlaybackState>(...)`
- Public StateFlow exposure: `val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()`
- Collected with `.collectLatest { }` in coroutine context
- Lazy properties for expensive one-time setup: `private val repo: MediaRepository by lazy { ... }`

---

*Convention analysis: 2026-04-17*
