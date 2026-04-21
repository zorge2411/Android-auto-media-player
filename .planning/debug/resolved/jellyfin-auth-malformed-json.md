---
status: resolved
trigger: "Jellyfin authentication fails with MalformedJsonException — the server response at login time is not valid JSON (likely an HTML error page or empty body), which causes ensureAuthenticated to throw IllegalStateException blocking all subsequent API calls."
created: 2026-04-17T00:00:00Z
updated: 2026-04-17T12:00:00Z
symptoms_prefilled: true
---

## Current Focus

hypothesis: CONFIRMED AND RESOLVED — cross-domain redirect interceptor correctly catches the Pangolin 302 redirect before Gson sees the HTML body. App code is complete. Remaining action is server-side (user enables Extended compatibility in Pangolin dashboard).
test: N/A — session complete
expecting: N/A
next_action: none — resolved

## Symptoms

expected: Jellyfin authentication succeeds and `getViews` returns media library views
actual: Authentication throws `MalformedJsonException` at line 1 column 1 (start of response), then `ensureAuthenticated` throws IllegalStateException propagating to all callers
errors: |
  com.google.gson.stream.MalformedJsonException: Use JsonReader.setLenient(true) to accept malformed JSON at line 1 column 1 path $
    at retrofit2.converter.gson.GsonResponseBodyConverter.convert(GsonResponseBodyConverter.java:40)
    at retrofit2.OkHttpCall.parseResponse(OkHttpCall.java:246)
    at retrofit2.OkHttpCall$1.onResponse(OkHttpCall.java:156)

  java.lang.IllegalStateException: Jellyfin authentication failed — check server URL and credentials in settings
    at com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository.ensureAuthenticated(JellyfinRepository.kt:124)
reproduction: Launch app → Jellyfin auth attempted → fails immediately
started: 2026-04-17 (app startup)

## Eliminated

(none — root cause identified early and confirmed by logcat)

## Evidence

- timestamp: 2026-04-17T01:00:00Z
  checked: Logcat from human-verify checkpoint
  found: App POSTs to /Users/AuthenticateByName, gets 200 OK with Content-Type: text/html; charset=utf-8 and body "<title>Auth - Pangolin</title>". The actual host in the HTTP log is wireguard.schoeler.pro, not jellyfin.schoeler.pro. Pangolin reverse proxy intercepts unauthenticated API calls and returns its own HTML login portal with a 200 status.
  implication: Response<T>.body() is null because Gson cannot parse HTML as JellyfinAuthResponse. The isSuccessful check (line 81) passes (200 is 2xx) so execution falls through to the null-body branch — currently logs a warning but does NOT throw a user-facing error from testAndSave(). The Settings UI just silently fails with no Toast shown.

- timestamp: 2026-04-17T01:00:01Z
  checked: testAndSave() null-body branch (JellyfinRepository.kt:90-95)
  found: Throws IllegalStateException("Jellyfin server returned an empty response body — check server URL"). The message does not mention auth proxies, so the user has no idea the real problem is a Pangolin/WireGuard proxy intercepting the request.
  implication: Need to add: (1) raw-body Content-Type inspection on the successful response to detect HTML, (2) a clearer error message naming the auth-proxy scenario.

- timestamp: 2026-04-17T01:00:02Z
  checked: SettingsViewModel.saveJellyfin() error matching (SettingsActivity.kt:58-70)
  found: Existing matchers for CLEARTEXT, "Unable to resolve host", 401/Unauthorized, timeout. No matcher for HTML / auth-proxy scenario. Falls through to the else branch which just shows e.message.
  implication: A new explicit matcher for "auth proxy" keyword in the error message will let the ViewModel surface a tailored hint. Or we can make testAndSave() throw a message the else-branch passes through clearly enough.

- timestamp: 2026-04-17T01:00:03Z
  checked: How to detect HTML response in Retrofit Response<T> context
  found: When body() is null on a 2xx, the raw body was consumed by Gson (and Gson returned null because it can't deserialize). However, we can use response.raw().body to peek at Content-Type BEFORE Gson runs by adding an OkHttp interceptor, OR we can use a call adapter. Simpler: use response.raw().headers["Content-Type"] to check after the fact — the header is always available even after body consumption. If Content-Type contains "text/html", the response is an HTML page.
  implication: In testAndSave() and authenticate(), after body() returns null on a 2xx response, check response.raw().header("Content-Type"). If it contains "text/html", throw/log with "auth proxy detected" message.

- timestamp: 2026-04-17T01:00:04Z
  checked: SettingsViewModel error-string matching strategy
  found: Adding "auth proxy" or "Pangolin" to the IllegalStateException message from testAndSave() lets SettingsViewModel.saveJellyfin() match on it. But simpler: just make the testAndSave() message self-sufficient so the else-branch in the ViewModel shows it verbatim — no new matcher needed.
  implication: The fix is entirely in JellyfinRepository.testAndSave() and authenticate(). SettingsViewModel and SettingsActivity need no changes.

- timestamp: 2026-04-17T01:00:05Z
  checked: authenticate() null-body branch (JellyfinRepository.kt:134-138)
  found: Logs a warning and returns false, which causes ensureAuthenticated() to throw "Jellyfin authentication failed — check server URL and credentials in settings". No hint about auth proxies.
  implication: Improve the log message in authenticate() to say "received HTML response — server URL may route through an auth proxy (e.g. Pangolin)". The IllegalStateException from ensureAuthenticated() is fine as-is since it's for internal callers (getViews/getItems) who log and return empty.

- timestamp: 2026-04-17T01:00:00Z
  checked: JellyfinApi.authenticate return type
  found: Returns JellyfinAuthResponse directly (not Response<JellyfinAuthResponse>). Retrofit with GsonConverterFactory calls Gson to parse any 2xx response body. If the body is not valid JSON (HTML, empty, BOM), Gson throws MalformedJsonException.
  implication: Any non-JSON 2xx body is fatal — the exception propagates through runCatching in JellyfinRepository.authenticate(), which returns false, which causes ensureAuthenticated() to throw IllegalStateException.

- timestamp: 2026-04-17T00:01:01Z
  checked: GsonConverterFactory configuration in buildApi()
  found: GsonConverterFactory.create() — uses default strict Gson. No lenient mode configured.
  implication: Even slightly malformed JSON (e.g. trailing comma, BOM, empty body) will throw MalformedJsonException. The error "line 1 column 1" specifically indicates the body starts with a non-JSON character (likely '<' for HTML or empty string).

- timestamp: 2026-04-17T00:01:02Z
  checked: JellyfinRepository.authenticate() error handling
  found: Uses runCatching { ... }.getOrElse { false }. This silently swallows all exceptions including MalformedJsonException — no raw body is logged, so it's impossible to know what the server actually returned.
  implication: The real server response (HTML page, error message, etc.) is invisible in logs. Adding raw body logging is essential for diagnosis and future debugging.

- timestamp: 2026-04-17T00:01:03Z
  checked: JellyfinRepository.testAndSave() error handling
  found: Calls testApi.authenticate() with NO try/catch. Any MalformedJsonException or HttpException thrown here crashes the settings save flow entirely.
  implication: testAndSave is also broken for the same reason.

- timestamp: 2026-04-17T00:01:04Z
  checked: Missing Content-Type header
  found: No explicit Content-Type header on the @POST endpoint. GsonConverterFactory automatically sets Content-Type: application/json for @Body parameters — so this is fine.
  implication: Not the cause.

- timestamp: 2026-04-17T00:01:05Z
  checked: Retrofit non-2xx handling
  found: For non-2xx responses with raw return type (not Response<T>), Retrofit throws HttpException — NOT MalformedJsonException. The error is specifically MalformedJsonException, which means the HTTP status IS 2xx but the body is not valid JSON.
  implication: Server is returning 200 OK with a non-JSON body. Most likely a reverse proxy returning an HTML page, or the Jellyfin server itself returning an empty/partial body.

- timestamp: 2026-04-17T02:00:00Z
  checked: New logcat from human verify checkpoint
  found: OkHttp follows 302 redirect cross-domain: jellyfin.schoeler.pro → wireguard.schoeler.pro/auth/resource/... → 200 OK + text/html. Final response.request.url.host is wireguard.schoeler.pro, not jellyfin.schoeler.pro. The existing Content-Type check in testAndSave() would eventually fire, but the cross-domain redirect detection is cleaner and more reliable (works even if Pangolin returns JSON-content-type HTML).
  implication: CrossDomainRedirectException interceptor catches this before Gson sees the body. Compilation confirmed successful (BUILD SUCCESSFUL in 35s, zero errors, only pre-existing deprecation warnings in unrelated files).

- timestamp: 2026-04-17T12:00:00Z
  checked: Human confirmation of completed fix
  found: User confirmed all four code changes are complete and working. Remaining issue is server-side only — user must enable Extended compatibility in Pangolin dashboard for the Jellyfin resource.
  implication: App code is correct and complete. Session resolved.

## Resolution

root_cause: >
  The user's Jellyfin base URL routes through wireguard.schoeler.pro (Pangolin reverse-proxy). Pangolin intercepts unauthenticated API calls with a 302 redirect to its own HTML auth portal on wireguard.schoeler.pro. OkHttp follows the cross-domain redirect automatically. The app receives 200 OK + text/html from a completely different host. Gson throws MalformedJsonException trying to parse HTML, which silently swallows through runCatching → authenticate() returns false → ensureAuthenticated() throws a generic IllegalStateException with no Pangolin-specific guidance.

fix: >
  1. JellyfinApi.authenticate() return type changed to Response<JellyfinAuthResponse> — prevents Gson crash on non-JSON bodies.
  2. Lenient Gson configured in GsonConverterFactory + OkHttp logging upgraded to BODY level for full response visibility.
  3. CrossDomainRedirectException (extends IOException) added — thrown by OkHttp interceptor when response host differs from request host. Correctly propagates through Retrofit/coroutines without crashing. Carries both originalHost and redirectedHost in its message naming Pangolin and the Extended compatibility fix.
  4. SettingsActivity shows user-friendly toast naming Pangolin and explaining the Extended compatibility fix when CrossDomainRedirectException is caught.
  Remaining action is server-side: user must enable Extended compatibility in Pangolin dashboard for the Jellyfin resource.

verification: >
  User confirmed all code changes are complete and the app builds successfully. The app correctly detects the cross-domain redirect and surfaces an actionable toast. Server-side configuration (Pangolin Extended compatibility) is the remaining step — outside the app's control.

files_changed:
  - app/src/main/java/com/pscholer/autoplayer/data/remote/jellyfin/JellyfinRepository.kt
  - app/src/main/java/com/pscholer/autoplayer/SettingsActivity.kt
