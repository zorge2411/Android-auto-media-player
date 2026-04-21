package com.pscholer.autoplayer.util

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("autoplayer_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ── Plex ─────────────────────────────────────────────────────────────────
    private object Plex {
        val SERVER_URL = stringPreferencesKey("plex_server_url")
        val TOKEN      = stringPreferencesKey("plex_token")
    }

    val plexConfig: PlexConfig
        get() = runBlocking {
            context.dataStore.data.map { p ->
                PlexConfig(p[Plex.SERVER_URL] ?: "", p[Plex.TOKEN] ?: "")
            }.first()
        }

    val plexConfigFlow: Flow<PlexConfig> = context.dataStore.data.map { p ->
        PlexConfig(p[Plex.SERVER_URL] ?: "", p[Plex.TOKEN] ?: "")
    }

    suspend fun savePlexConfig(serverUrl: String, token: String) {
        context.dataStore.edit { p ->
            p[Plex.SERVER_URL] = serverUrl
            p[Plex.TOKEN]      = token
        }
    }

    // ── Jellyfin ──────────────────────────────────────────────────────────────
    private object Jellyfin {
        val SERVER_URL = stringPreferencesKey("jf_server_url")
        val USERNAME   = stringPreferencesKey("jf_username")
        val PASSWORD   = stringPreferencesKey("jf_password")
        val USER_ID    = stringPreferencesKey("jf_user_id")
        val TOKEN      = stringPreferencesKey("jf_token")
    }

    val jellyfinConfig: JellyfinConfig
        get() = runBlocking {
            context.dataStore.data.map { p ->
                JellyfinConfig(
                    serverUrl = p[Jellyfin.SERVER_URL] ?: "",
                    username  = p[Jellyfin.USERNAME]   ?: "",
                    password  = p[Jellyfin.PASSWORD]   ?: ""
                )
            }.first()
        }

    val jellyfinConfigFlow: Flow<JellyfinConfig> = context.dataStore.data.map { p ->
        JellyfinConfig(
            serverUrl = p[Jellyfin.SERVER_URL] ?: "",
            username  = p[Jellyfin.USERNAME]   ?: "",
            password  = p[Jellyfin.PASSWORD]   ?: ""
        )
    }

    val jellyfinSession: JellyfinSession
        get() = runBlocking {
            context.dataStore.data.map { p ->
                JellyfinSession(p[Jellyfin.USER_ID] ?: "", p[Jellyfin.TOKEN] ?: "")
            }.first()
        }

    suspend fun saveJellyfinConfig(serverUrl: String, username: String, password: String) {
        context.dataStore.edit { p ->
            p[Jellyfin.SERVER_URL] = serverUrl
            p[Jellyfin.USERNAME]   = username
            p[Jellyfin.PASSWORD]   = password
            // Clear cached session so re-auth happens with new credentials
            p.remove(Jellyfin.USER_ID)
            p.remove(Jellyfin.TOKEN)
        }
    }

    suspend fun saveJellyfinSession(userId: String, token: String) {
        context.dataStore.edit { p ->
            p[Jellyfin.USER_ID] = userId
            p[Jellyfin.TOKEN]   = token
        }
    }

    // ── SAF directory URIs ────────────────────────────────────────────────────
    private val SAF_URIS = stringSetPreferencesKey("saf_uris")

    val safUris: Set<Uri>
        get() = runBlocking {
            context.dataStore.data.map { p ->
                (p[SAF_URIS] ?: emptySet()).map { Uri.parse(it) }.toSet()
            }.first()
        }

    val safUrisFlow: Flow<Set<Uri>> = context.dataStore.data.map { p ->
        (p[SAF_URIS] ?: emptySet()).map { Uri.parse(it) }.toSet()
    }

    suspend fun addSafUri(uri: Uri) {
        context.dataStore.edit { p ->
            p[SAF_URIS] = (p[SAF_URIS] ?: emptySet()) + uri.toString()
        }
    }

    suspend fun removeSafUri(uri: Uri) {
        context.dataStore.edit { p ->
            p[SAF_URIS] = (p[SAF_URIS] ?: emptySet()) - uri.toString()
        }
    }

    // ── Data classes ──────────────────────────────────────────────────────────
    data class PlexConfig(val serverUrl: String, val token: String)
    data class JellyfinConfig(val serverUrl: String, val username: String, val password: String)
    data class JellyfinSession(val userId: String, val token: String)
}
