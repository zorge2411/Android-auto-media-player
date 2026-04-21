package com.pscholer.autoplayer.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pscholer.autoplayer.data.models.PlaybackState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

val Context.playbackDataStore by preferencesDataStore("playback_state")

@Singleton
class PlaybackRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val json = Json

    suspend fun save(state: PlaybackState) {
        val key = stringPreferencesKey("playback_${state.mediaId}")
        context.playbackDataStore.edit { prefs ->
            prefs[key] = json.encodeToString(state)
        }
    }

    suspend fun load(mediaId: String): PlaybackState? {
        val key = stringPreferencesKey("playback_$mediaId")
        val prefs = context.playbackDataStore.data.first()
        return prefs[key]?.let { json.decodeFromString(it) }
    }

    suspend fun delete(mediaId: String) {
        val key = stringPreferencesKey("playback_$mediaId")
        context.playbackDataStore.edit { prefs ->
            prefs.remove(key)
        }
    }

    suspend fun deleteIfCompleted(mediaId: String, watchedThreshold: Float = 0.95f) {
        val state = load(mediaId) ?: return
        if (state.durationMs > 0 && state.positionMs >= (state.durationMs * watchedThreshold)) {
            delete(mediaId)
        }
    }
}
