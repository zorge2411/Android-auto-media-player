package com.pscholer.autoplayer.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import com.pscholer.autoplayer.data.FavoriteRepository
import com.pscholer.autoplayer.data.models.Favorite
import kotlinx.coroutines.launch

import androidx.car.app.CarToast
import com.pscholer.autoplayer.data.MediaSource
import com.pscholer.autoplayer.data.MediaRepository
import com.pscholer.autoplayer.di.AppEntryPoint
import dagger.hilt.android.EntryPointAccessors

/**
 * Browse all favorite videos across LOCAL/PLEX/JELLYFIN sources.
 * Tapping a video plays it. Heart icon toggles favorite status.
 */
class FavoritesScreen(carContext: CarContext) : Screen(carContext) {

    private val favoriteRepository = FavoriteRepository(carContext.applicationContext)
    private var favorites: List<Favorite> = emptyList()
    private var isLoading = true

    private val repo: MediaRepository by lazy {
        EntryPointAccessors.fromApplication(
            carContext.applicationContext,
            AppEntryPoint::class.java
        ).mediaRepository()
    }

    init {
        lifecycleScope.launch {
            favorites = favoriteRepository.list()
            isLoading = false
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val items = ItemList.Builder().apply {
            when {
                isLoading -> setNoItemsMessage("Loading favorites…")
                favorites.isEmpty() -> setNoItemsMessage("No favorites yet")
                else -> {
                    favorites.forEach { favorite ->
                        addItem(
                            Row.Builder()
                                .setTitle(favorite.mediaTitle)
                                .addText(favorite.source)
                                .setOnClickListener {
                                    lifecycleScope.launch {
                                        CarToast.makeText(carContext, "Loading...", CarToast.LENGTH_SHORT).show()
                                        val sourceEnum = runCatching { MediaSource.valueOf(favorite.source) }.getOrNull()
                                        if (sourceEnum == null) {
                                            CarToast.makeText(carContext, "Unknown source", CarToast.LENGTH_SHORT).show()
                                            return@launch
                                        }
                                        val item = repo.getItem(sourceEnum, favorite.mediaId, favorite.mediaTitle)
                                        if (item != null) {
                                            screenManager.push(VideoPlaybackScreen(carContext, item))
                                        } else {
                                            CarToast.makeText(carContext, "Failed to load media", CarToast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .build()
                        )
                    }
                }
            }
        }.build()

        val header = Header.Builder()
            .setTitle("Favorites")
            .setStartHeaderAction(Action.BACK)
            .build()

        return ListTemplate.Builder()
            .setHeader(header)
            .setSingleList(items)
            .build()
    }
}
