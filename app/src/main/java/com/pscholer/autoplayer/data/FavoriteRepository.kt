package com.pscholer.autoplayer.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pscholer.autoplayer.data.models.Favorite
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.builtins.ListSerializer

val Context.favoriteDataStore by preferencesDataStore("favorites")

@Singleton
class FavoriteRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val json = Json
    private val favoritesKey = stringPreferencesKey("all_favorites")

    suspend fun add(favorite: Favorite) {
        val current = list().toMutableList()
        current.add(favorite)
        save(current)
    }

    suspend fun remove(mediaId: String) {
        val current = list().filter { it.mediaId != mediaId }
        save(current)
    }

    suspend fun isFavorite(mediaId: String): Boolean {
        return list().any { it.mediaId == mediaId }
    }

    suspend fun list(): List<Favorite> {
        val prefs = context.favoriteDataStore.data.first()
        return prefs[favoritesKey]?.let {
            json.decodeFromString(ListSerializer(Favorite.serializer()), it)
        } ?: emptyList()
    }

    suspend fun listBySource(source: String): List<Favorite> {
        return list().filter { it.source == source }
    }

    private suspend fun save(favorites: List<Favorite>) {
        context.favoriteDataStore.edit { prefs ->
            prefs[favoritesKey] = json.encodeToString(ListSerializer(Favorite.serializer()), favorites)
        }
    }
}
