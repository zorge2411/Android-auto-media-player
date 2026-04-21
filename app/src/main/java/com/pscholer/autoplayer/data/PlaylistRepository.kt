package com.pscholer.autoplayer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pscholer.autoplayer.data.models.Playlist
import com.pscholer.autoplayer.data.models.PlaylistItem
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.builtins.ListSerializer

val Context.playlistDataStore by preferencesDataStore("playlists")

class PlaylistRepository(private val context: Context) {

    private val json = Json
    private val playlistsKey = stringPreferencesKey("all_playlists")

    suspend fun create(playlist: Playlist) {
        val current = list().toMutableList()
        current.add(playlist)
        saveAll(current)
    }

    suspend fun list(): List<Playlist> {
        val prefs = context.playlistDataStore.data.first()
        return prefs[playlistsKey]?.let {
            json.decodeFromString(ListSerializer(Playlist.serializer()), it)
        } ?: emptyList()
    }

    suspend fun get(id: String): Playlist? {
        return list().find { it.id == id }
    }

    suspend fun update(playlist: Playlist) {
        val current = list().toMutableList()
        val index = current.indexOfFirst { it.id == playlist.id }
        if (index >= 0) {
            current[index] = playlist
            saveAll(current)
        }
    }

    suspend fun delete(id: String) {
        val current = list().filter { it.id != id }
        saveAll(current)
    }

    suspend fun addVideo(playlistId: String, item: PlaylistItem) {
        val playlist = get(playlistId) ?: return
        val updated = playlist.copy(videos = playlist.videos + item)
        update(updated)
    }

    suspend fun removeVideo(playlistId: String, mediaId: String) {
        val playlist = get(playlistId) ?: return
        val updated = playlist.copy(videos = playlist.videos.filter { it.mediaId != mediaId })
        update(updated)
    }

    private suspend fun saveAll(playlists: List<Playlist>) {
        context.playlistDataStore.edit { prefs ->
            prefs[playlistsKey] = json.encodeToString(ListSerializer(Playlist.serializer()), playlists)
        }
    }
}
