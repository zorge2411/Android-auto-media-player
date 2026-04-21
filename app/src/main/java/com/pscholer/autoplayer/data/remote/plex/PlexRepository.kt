package com.pscholer.autoplayer.data.remote.plex

import android.util.Log
import com.pscholer.autoplayer.data.models.MediaItem
import com.pscholer.autoplayer.util.PreferencesManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlexRepository @Inject constructor(
    private val prefs: PreferencesManager
) {
    companion object {
        private const val TAG = "PlexRepository"
        private const val CLIENT_IDENTIFIER = "auto-player-android-auto"
    }

    // Lazily (re)built when server config changes
    private var cachedApi: PlexApi? = null
    private var cachedServerUrl: String = ""

    private fun getSanitizedUrl(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.matches(Regex("^(192\\.168\\.|10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.).*")) ->
                "http://$trimmed"
            else -> "https://$trimmed"
        }.trimEnd('/')
    }

    private fun api(): PlexApi {
        val config = prefs.plexConfig
        if (cachedApi == null || config.serverUrl != cachedServerUrl) {
            cachedServerUrl = config.serverUrl
            cachedApi = buildRetrofit(config.serverUrl).create(PlexApi::class.java)
        }
        return cachedApi!!
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Attach standard Plex client identification headers to every request
                val req = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("X-Plex-Client-Identifier", CLIENT_IDENTIFIER)
                    .addHeader("X-Plex-Product", "Auto Player")
                    .addHeader("X-Plex-Version", "1.0")
                    .addHeader("X-Plex-Platform", "Android")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
            .build()

        return Retrofit.Builder()
            .baseUrl(getSanitizedUrl(baseUrl) + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    suspend fun getLibraries(): List<MediaItem> = runCatching {
        val config = prefs.plexConfig
        api().getLibraries(config.token).container.sections
            .filter { it.type in setOf("movie", "show") }
            .map { section ->
                MediaItem(
                    id = section.key,
                    title = section.title,
                    isFolder = true,
                    thumbnailUrl = section.thumb?.let { thumbUrl(it, config.token) }
                )
            }
    }.getOrElse { e ->
        Log.e(TAG, "getLibraries failed", e)
        emptyList()
    }

    suspend fun getSectionItems(sectionId: String): List<MediaItem> = runCatching {
        val config = prefs.plexConfig
        api().getSectionItems(sectionId, config.token).container.items
            .map { it.toMediaItem(config.serverUrl, config.token) }
    }.getOrElse { e ->
        Log.e(TAG, "getSectionItems failed", e)
        emptyList()
    }

    suspend fun getChildren(ratingKey: String): List<MediaItem> = runCatching {
        val config = prefs.plexConfig
        api().getChildren(ratingKey, config.token).container.items
            .map { it.toMediaItem(config.serverUrl, config.token) }
    }.getOrElse { e ->
        Log.e(TAG, "getChildren failed", e)
        emptyList()
    }

    suspend fun getItem(ratingKey: String): MediaItem? = runCatching {
        val config = prefs.plexConfig
        api().getItem(ratingKey, config.token).container.items
            .firstOrNull()?.toMediaItem(config.serverUrl, config.token)
    }.getOrElse { e ->
        Log.e(TAG, "getItem failed for $ratingKey", e)
        null
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun thumbUrl(path: String, token: String): String =
        "${getSanitizedUrl(prefs.plexConfig.serverUrl)}$path?X-Plex-Token=$token"

    private fun PlexMetadata.toMediaItem(serverUrl: String, token: String): MediaItem {
        val sanitizedServerUrl = getSanitizedUrl(serverUrl)
        val isFolder = type in setOf("show", "season")

        // Build a human-readable subtitle:  "S2 · E4" or "2021"
        val subtitle = when (type) {
            "episode" -> buildString {
                parentIndex?.let { append("S$it") }
                index?.let { if (isNotEmpty()) append(" · "); append("E$it") }
            }.ifEmpty { null }
            "movie" -> year?.toString()
            "show"   -> year?.toString()
            else -> null
        }

        // Direct Play URL — requests the raw file without transcoding
        val part = media?.firstOrNull()?.parts?.firstOrNull()
        val streamUrl = if (!isFolder && part != null) {
            "$sanitizedServerUrl${part.key}?X-Plex-Token=$token"
        } else null

        return MediaItem(
            id = ratingKey,
            title = title,
            subtitle = subtitle,
            isFolder = isFolder,
            thumbnailUrl = thumb?.let { "$sanitizedServerUrl$it?X-Plex-Token=$token" },
            streamUrl = streamUrl,
            durationMs = duration ?: 0L,
            headers = mapOf("X-Plex-Token" to token)
        )
    }
}
