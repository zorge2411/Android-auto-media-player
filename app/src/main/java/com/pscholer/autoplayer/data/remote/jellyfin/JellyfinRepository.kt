package com.pscholer.autoplayer.data.remote.jellyfin

import android.util.Log
import com.google.gson.GsonBuilder
import com.pscholer.autoplayer.data.models.MediaItem
import com.pscholer.autoplayer.util.PreferencesManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thrown when OkHttp follows a redirect to a different host than the original request.
 * This indicates the server URL routes through an auth proxy (e.g. Pangolin, Authentik,
 * Authelia) that intercepts unauthenticated requests and redirects them to a login portal.
 *
 * Must extend IOException so OkHttp interceptors propagate it correctly through the
 * call chain instead of crashing the Dispatcher thread.
 */
class CrossDomainRedirectException(
    val originalHost: String,
    val redirectedHost: String
) : IOException(
    "Request to $originalHost was redirected to $redirectedHost — " +
    "the server URL routes through an auth proxy (e.g. Pangolin). " +
    "Enable 'Extended compatibility' in the Pangolin dashboard " +
    "(resource → Authentication → Extended compatibility), or use the direct " +
    "Jellyfin URL (e.g. jellyfin.yourdomain.com) instead of the proxy URL."
)

@Singleton
class JellyfinRepository @Inject constructor(
    private val prefs: PreferencesManager
) {
    companion object {
        private const val TAG = "JellyfinRepository"

        // Required format for the X-Emby-Authorization header
        private const val CLIENT_HEADER =
            "MediaBrowser Client=\"AutoPlayer\", Device=\"Android\", " +
            "DeviceId=\"auto-player-001\", Version=\"1.0\""
    }

    private var api: JellyfinApi? = null
    private var userId: String = ""
    private var authToken: String = ""
    private var cachedServerUrl: String = ""

    private fun getSanitizedUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            // Local IPs use http; domain names default to https
            trimmed.matches(Regex("^(192\\.168\\.|10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.).*")) ->
                "http://$trimmed"
            else -> "https://$trimmed"
        }.trimEnd('/')
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun buildApi(serverUrl: String): JellyfinApi {
        cachedServerUrl = serverUrl
        val baseUrl = getSanitizedUrl(serverUrl) + "/"
        val expectedHost = baseUrl.toHttpUrlOrNull()?.host ?: ""

        // Lenient mode lets Gson tolerate minor JSON deviations (e.g. unquoted
        // keys, trailing commas).  It does NOT suppress the exception when the
        // body is completely non-JSON (HTML page, empty body) — for that we use
        // Response<T> in the API interface so we can inspect the raw body first.
        val lenientGson = GsonBuilder().setLenient().create()

        // Cross-domain redirect detector: if OkHttp follows a redirect to a
        // different host (e.g. Pangolin/Authentik proxy intercepting the request),
        // abort early with a clear error rather than letting Gson try to parse HTML.
        val crossDomainInterceptor = okhttp3.Interceptor { chain ->
            val response = chain.proceed(chain.request())
            val responseHost = response.request.url.host
            if (responseHost != expectedHost && expectedHost.isNotEmpty()) {
                Log.e(TAG, "Cross-domain redirect detected: request to $expectedHost " +
                        "ended up at $responseHost — likely an auth proxy (e.g. Pangolin). " +
                        "Enable 'Extended compatibility' in the Pangolin dashboard, or use " +
                        "the direct Jellyfin URL in Settings.")
                // Close the body to prevent a resource leak before throwing
                response.body?.close()
                throw CrossDomainRedirectException(
                    originalHost = expectedHost,
                    redirectedHost = responseHost
                )
            }
            response
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(crossDomainInterceptor)
                    .addInterceptor(
                        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                    )
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create(lenientGson))
            .build()
            .create(JellyfinApi::class.java)
    }

    /**
     * Verify credentials by actually connecting to the server before saving anything.
     * Only persists config + session token if auth succeeds.
     * Returns the authenticated display name on success, or throws on failure.
     */
    suspend fun testAndSave(serverUrl: String, username: String, password: String): String {
        val testApi = buildApi(serverUrl)
        val httpResponse = testApi.authenticate(
            JellyfinAuthRequest(username, password),
            CLIENT_HEADER
        )

        if (!httpResponse.isSuccessful) {
            val errorBody = httpResponse.errorBody()?.string() ?: "(empty error body)"
            Log.e(TAG, "Auth HTTP ${httpResponse.code()}: $errorBody")
            throw IllegalStateException(
                "Jellyfin server returned HTTP ${httpResponse.code()} — check server URL and credentials"
            )
        }

        val authBody = httpResponse.body()
        if (authBody == null) {
            // Check whether the server returned an HTML page (e.g. a reverse-proxy auth portal
            // such as Pangolin/Authentik/Authelia) rather than a Jellyfin JSON response.
            val contentType = httpResponse.raw().header("Content-Type") ?: ""
            if (contentType.contains("text/html", ignoreCase = true)) {
                Log.e(TAG, "Auth HTTP 2xx but got HTML response (Content-Type: $contentType) — " +
                        "server URL ${getSanitizedUrl(serverUrl)} routes through an auth proxy")
                throw IllegalStateException(
                    "Server URL appears to point to an auth proxy (received an HTML page instead " +
                    "of a Jellyfin response). Try the direct Jellyfin URL (e.g. jellyfin.yourdomain.com) " +
                    "instead of the proxy URL."
                )
            }
            Log.e(TAG, "Auth returned HTTP 2xx but body is null (Content-Type: $contentType)")
            throw IllegalStateException(
                "Jellyfin server returned an empty response — check server URL"
            )
        }

        // Auth succeeded — now persist everything and update internal state
        prefs.saveJellyfinConfig(serverUrl, username, password)
        prefs.saveJellyfinSession(authBody.user.id, authBody.accessToken)
        userId = authBody.user.id
        authToken = authBody.accessToken
        api = testApi
        Log.i(TAG, "Authenticated as ${authBody.user.name}")
        return authBody.user.name
    }

    /**
     * Re-authenticate using already-persisted credentials.
     * Used internally when the cached session token is missing.
     *
     * Returns true on success, false on any failure.  All failure paths log
     * enough information to diagnose the problem without having to reproduce it.
     */
    suspend fun authenticate(): Boolean = runCatching {
        val config = prefs.jellyfinConfig

        if (config.serverUrl.isBlank() || config.username.isBlank()) {
            Log.w(TAG, "Authenticate called but server URL or username is blank — skipping")
            return@runCatching false
        }

        val jellyApi = buildApi(config.serverUrl)
        val httpResponse = jellyApi.authenticate(
            JellyfinAuthRequest(config.username, config.password),
            CLIENT_HEADER
        )

        if (!httpResponse.isSuccessful) {
            val errorBody = httpResponse.errorBody()?.string() ?: "(empty error body)"
            Log.e(TAG, "Auth HTTP ${httpResponse.code()} from ${config.serverUrl}: $errorBody")
            return@runCatching false
        }

        val authBody = httpResponse.body()
        if (authBody == null) {
            val contentType = httpResponse.raw().header("Content-Type") ?: ""
            if (contentType.contains("text/html", ignoreCase = true)) {
                Log.e(TAG, "Auth HTTP 2xx but got HTML response (Content-Type: $contentType) — " +
                        "server URL ${config.serverUrl} routes through an auth proxy " +
                        "(e.g. Pangolin). Use the direct Jellyfin URL in Settings.")
            } else {
                Log.e(TAG, "Auth returned HTTP 2xx but body was null " +
                        "(Content-Type: $contentType) — server URL: ${config.serverUrl}")
            }
            return@runCatching false
        }

        userId = authBody.user.id
        authToken = authBody.accessToken
        api = jellyApi

        prefs.saveJellyfinSession(userId, authToken)
        Log.i(TAG, "Re-authenticated as ${authBody.user.name}")
        true
    }.getOrElse { e ->
        Log.e(TAG, "Authentication failed with exception", e)
        false
    }

    suspend fun getViews(): List<MediaItem> {
        return runCatching {
            ensureAuthenticated()
            api!!.getViews(userId, authToken).items
                .filter { it.type in setOf("CollectionFolder", "UserView") }
                .map { it.toMediaItem() }
        }.getOrElse { e ->
            Log.e(TAG, "getViews failed", e)
            emptyList()
        }
    }

    suspend fun getItems(parentId: String? = null): List<MediaItem> {
        return runCatching {
            ensureAuthenticated()
            api!!.getItems(userId, authToken, parentId = parentId).items
                .map { it.toMediaItem() }
        }.getOrElse { e ->
            Log.e(TAG, "getItems failed", e)
            emptyList()
        }
    }

    suspend fun getItem(itemId: String): MediaItem? {
        return runCatching {
            ensureAuthenticated()
            api!!.getItem(userId, itemId, authToken).toMediaItem()
        }.getOrElse { e ->
            Log.e(TAG, "getItem failed for $itemId", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun ensureAuthenticated() {
        val session = prefs.jellyfinSession
        val config = prefs.jellyfinConfig

        if (session.token.isNotEmpty()) {
            userId = session.userId
            authToken = session.token
            if (api == null || config.serverUrl != cachedServerUrl) {
                api = buildApi(config.serverUrl)
            }
        } else {
            val ok = authenticate()
            if (!ok || api == null) {
                throw IllegalStateException(
                    "Jellyfin authentication failed — check server URL and credentials in settings"
                )
            }
        }
    }

    private fun JellyfinItem.toMediaItem(): MediaItem {
        val serverUrl = getSanitizedUrl(prefs.jellyfinConfig.serverUrl)
        val isFolder = type in setOf("Series", "Season", "CollectionFolder", "UserView", "BoxSet")

        val subtitle = when (type) {
            "Episode" -> buildString {
                parentIndexNumber?.let { append("S$it") }
                indexNumber?.let { if (isNotEmpty()) append(" · "); append("E$it") }
                seriesName?.let { if (isNotEmpty()) append(" — "); append(it) }
            }.ifEmpty { null }
            "Movie"  -> year?.toString()
            "Series" -> year?.toString()
            else -> null
        }

        // Prefer Primary image tag; fall back to backdrop
        val thumbTag = imageTags?.get("Primary")
        val thumbUrl = thumbTag?.let {
            "$serverUrl/Items/$id/Images/Primary?tag=$it&maxWidth=256"
        }

        // Direct stream: static=true serves the original file without transcoding
        val streamUrl = if (!isFolder) {
            "$serverUrl/Videos/$id/stream?static=true&api_key=$authToken"
        } else null

        return MediaItem(
            id = id,
            title = name,
            subtitle = subtitle,
            isFolder = isFolder,
            thumbnailUrl = thumbUrl,
            streamUrl = streamUrl,
            durationMs = runtimeTicks?.div(10_000) ?: 0L,   // ticks → ms
            headers = mapOf("X-Emby-Token" to authToken)
        )
    }
}
