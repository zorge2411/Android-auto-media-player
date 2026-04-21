# Testing Patterns

**Analysis Date:** 2026-04-17

## Test Structure

**Location:**
- No `/test` or `/androidTest` directories present in source tree
- Project structure shows only `app/src/main` — no test source sets configured
- Test framework dependencies not declared in build.gradle.kts

**Naming Convention:**
- Not applicable — no test files exist in codebase

**Current State:**
- **Zero test coverage** — codebase is untested
- No JUnit, Mockito, or other test framework dependencies listed in build.gradle

## Testing Frameworks

**Not Configured:**
- No test runner framework installed (JUnit 4, JUnit 5, Kotest, etc.)
- No assertion library (Hamcrest, AssertJ, Kotest assertions, etc.)
- No mocking framework (Mockito, MockK, etc.)
- No Android instrumentation test framework (Espresso, Robolectric, etc.)

**Available in build.gradle (if testing were added):**
```
// These would be needed to add testing:
// testImplementation("junit:junit:4.13.2")
// testImplementation("org.mockito:mockito-core:5.x.x")
// testImplementation("org.mockito.kotlin:mockito-kotlin:5.x.x")
// androidTestImplementation("androidx.test.espresso:espresso-core:3.x.x")
// testImplementation("io.kotest:kotest-runner-junit5:5.x.x")
```

**Run Commands:**
```bash
# No tests can be run
./gradlew test              # Would run unit tests (not configured)
./gradlew connectedAndroidTest  # Would run instrumentation tests (not configured)
```

## Coverage Areas

**Not Tested:**
All code paths are untested, including critical business logic:

**Data Layer (100% untested):**
- `MediaRepository.kt` — core abstraction that routes between local/Plex/Jellyfin sources
- `LocalMediaRepository.kt` — MediaStore scanning, SAF directory traversal, MIME type filtering
- `JellyfinRepository.kt` — auth flow, cross-domain redirect detection, Gson lenient parsing, token refresh
- `PlexRepository.kt` — library enumeration, metadata transformation, URL sanitization
- Model DTOs (`MediaItem.kt`, `JellyfinModels.kt`, `PlexModels.kt`) — no serialization/deserialization tests

**Remote Integration (100% untested):**
- HTTP request building and header injection
- Error handling for 2xx responses with HTML bodies (auth proxy detection)
- Token-based auth flows (Jellyfin, Plex)
- Response parsing edge cases (missing fields, null bodies, Content-Type mismatches)

**Playback Manager (100% untested):**
- `MediaPlayerManager.kt` — ExoPlayer state transitions, surface management, playback control
- Audio focus handling (LOSS, LOSS_TRANSIENT, LOSS_TRANSIENT_CAN_DUCK, GAIN)
- Error propagation from ExoPlayer listener to StateFlow

**UI Layer (100% untested):**
- `RootScreen.kt` — grid item construction, click listeners
- `BrowseScreen.kt` — thumbnail prefetching, error message display, navigation
- `VideoPlaybackScreen.kt` — playback state UI updates, action binding
- `AutoMediaSession.kt` — session initialization, surface callback registration

**Utilities (100% untested):**
- `PreferencesManager.kt` — DataStore read/write operations, preference key management
- `VideoSurfaceRenderer.kt` — surface lifecycle management

## Test Patterns

**Not Applicable:**

No test code exists to establish patterns. If tests were to be written, recommended patterns:

**Unit Tests (recommended):**
- Mock remote APIs (Jellyfin, Plex) to test repository response parsing
- Mock PreferencesManager to test authentication state management
- Test data model transformations (JellyfinItem → MediaItem)
- Test error handling paths (malformed JSON, network errors, auth failures)

**Integration Tests (recommended):**
- Test data flow through MediaRepository with mocked upstream sources
- Verify correct upstream repository is selected per MediaSource enum
- Test Auth flow end-to-end with mocked HTTP responses

**Instrumentation Tests (recommended):**
- Test PreferencesManager DataStore operations with real context
- Test Screen lifecycle and template rendering
- Test Surface callback registration and delivery
- Test Coil image loading on actual device/emulator

**Mock Strategy (if implemented):**
```kotlin
// Recommended: MockK for Kotlin suspend functions
testImplementation("io.mockk:mockk:1.x.x")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.x.x")

// Mock repositories in tests:
val mockPlex = mockk<PlexRepository>()
coEvery { mockPlex.getLibraries() } returns listOf(mediaItem1, mediaItem2)

val repo = MediaRepository(local = mockLocal, plex = mockPlex, jellyfin = mockJellyfin)
```

## Fixtures and Factories

**Not Present:**

No test fixtures, factories, or test data builders exist. 

**Recommended Locations (if added):**
```
app/src/test/java/com/pscholer/autoplayer/
├── fixtures/
│   ├── MediaItemFixtures.kt    # Test data builders
│   └── JellyfinResponseFixtures.kt
├── fakes/
│   ├── FakeMediaRepository.kt
│   ├── FakePlexRepository.kt
│   └── FakeJellyfinRepository.kt
└── [Feature]Tests.kt

app/src/androidTest/java/com/pscholer/autoplayer/
├── screens/
│   ├── RootScreenTest.kt
│   └── BrowseScreenTest.kt
└── ui/
    └── VideoPlaybackScreenTest.kt
```

**Recommended Fixture Pattern:**
```kotlin
// Factories for building test data
object MediaItemFixtures {
    fun aMediaItem(
        id: String = "test-id",
        title: String = "Test Video",
        isFolder: Boolean = false,
        thumbnailUrl: String? = null
    ) = MediaItem(
        id = id,
        title = title,
        isFolder = isFolder,
        thumbnailUrl = thumbnailUrl
    )
}

// Usage in tests
val videos = listOf(
    MediaItemFixtures.aMediaItem(title = "Video 1"),
    MediaItemFixtures.aMediaItem(title = "Video 2")
)
```

## Coverage

**Current Status:** 0% — No tests configured or present

**Requirements:** None enforced in build.gradle or CI/CD

**View Coverage (not applicable):**
```bash
# If Jacoco were configured:
# ./gradlew testCoverageReport
```

## Test Types

**Unit Tests:**
- Not present
- **Scope (if added):** Test individual repository methods, model transformations, preference operations
- **Approach (recommended):** Mock external APIs, test with local test data

**Integration Tests:**
- Not present
- **Scope (if added):** Test MediaRepository routing to correct upstream, full auth flows with mocked responses
- **Approach (recommended):** Create test doubles for remote APIs, verify correct dependency composition

**Instrumentation Tests:**
- Not present
- **Framework needed:** Espresso, Hilt testing support
- **Scope (if added):** Test Screen rendering, DataStore operations, Surface callback delivery
- **Approach (recommended):** Use Robolectric for unit-level UI testing or real device for integration tests

**E2E Tests:**
- Not used
- Would require physical Android Auto head unit or car app emulator
- Not typical for Android App Library projects (car display is provided by host)

## Common Patterns

**Async Testing:**
Not present, but if implemented:
```kotlin
// Recommended: runTest from kotlinx-coroutines-test
@Test
fun testGetItems() = runTest {
    val repo = createRepository()
    val items = repo.getItems(MediaSource.LOCAL, parentId = null)
    assertThat(items).isNotEmpty()
}

// Or with suspend test:
@Test
fun testAuthenticate() = runTest {
    val repo = JellyfinRepository(mockPrefs)
    val result = repo.authenticate()
    assertTrue(result)
}
```

**Error Testing:**
Not present, but if implemented:
```kotlin
// Recommended: Test exception handling with Mockito.doThrow()
@Test
fun testGetItemsHandlesNetworkError() = runTest {
    coEvery { mockApi.getItems(...) } throws IOException("Network error")
    
    val repo = JellyfinRepository(mockPrefs)
    val result = repo.getItems()
    
    assertThat(result).isEmpty()  // Should return empty list, not throw
}
```

## Recommendations

**Priority 1 — Add Testing Infrastructure:**
1. Add JUnit 5 and Kotlin coroutines-test to build.gradle
2. Configure `app/src/test` and `app/src/androidTest` source sets
3. Add MockK for mocking suspend functions (Plex/Jellyfin APIs)
4. Add Hilt testing support for dependency injection in tests

**Priority 2 — Unit Test High-Risk Areas:**
1. `JellyfinRepository.kt` — Complex auth, HTML error detection, cross-domain redirect handling
   - Test: `testAndSave()` with valid/invalid credentials, HTML response detection
   - Test: `authenticate()` with missing config, network failures
   - Test: Auth token refresh and caching logic
   
2. `LocalMediaRepository.kt` — Media scanning and SAF traversal
   - Test: `scanMediaStore()` with various MIME types
   - Test: `scanSafDirectory()` with nested folder structures
   - Test: MIME type filtering (unsupported formats excluded)

3. `MediaPlayerManager.kt` — State machine and audio focus
   - Test: `PlaybackState` transitions (Idle → Buffering → Ready → Ended)
   - Test: Surface attachment/detachment without crashes
   - Test: Error handling (PlaybackException propagation)

**Priority 3 — Integration Tests:**
1. `MediaRepository` routing logic — verify correct source is queried
2. Full auth flow — credentials saved, tokens cached, re-auth on token expiry
3. Screen navigation and template rendering (with Robolectric)

**Priority 4 — Instrumentation Tests:**
1. `PreferencesManager` DataStore operations on real Android context
2. `BrowseScreen` thumbnail prefetching (Coil image loading)
3. `VideoPlaybackScreen` playback state UI updates
4. `AutoMediaSession` surface callback delivery

**Gaps to Address:**
- No validation of HTTP response Content-Type (HTML detection for auth proxies)
- No testing of Coil image loader error handling
- No testing of Player listener callbacks and StateFlow emission
- No testing of SAF tree URI permission persistence
- No CI/CD test automation (no GitHub Actions or equivalent)

---

*Testing analysis: 2026-04-17*
