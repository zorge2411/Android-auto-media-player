package com.pscholer.autoplayer.car.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.util.concurrent.ConcurrentHashMap
import com.pscholer.autoplayer.R
import com.pscholer.autoplayer.data.MediaSource
import com.pscholer.autoplayer.data.MediaRepository
import com.pscholer.autoplayer.data.models.MediaItem
import com.pscholer.autoplayer.di.AppEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Browsable grid screen for navigating library hierarchies.
 *
 * Flow:
 *  RootScreen → BrowseScreen(parentId=null) [libraries/root folders]
 *             → BrowseScreen(parentId=X)    [sub-folders / seasons]
 *             → VideoPlaybackScreen          [leaf video item]
 *
 * Thumbnail loading:
 *  We pre-fetch all thumbnails with Coil on a background thread, then call
 *  invalidate() to re-render the template with actual artwork. Android Auto
 *  does NOT support async image loading inside template builders — you must
 *  have the Bitmap ready before building the GridItem.
 */
class BrowseScreen(
    carContext: CarContext,
    private val source: MediaSource,
    private val parentId: String?
) : Screen(carContext) {

    private var items: List<MediaItem> = emptyList()
    private val thumbnails = ConcurrentHashMap<String, Bitmap>()
    private var isLoading = true
    private var errorMessage: String? = null

    private val repo: MediaRepository by lazy {
        EntryPointAccessors.fromApplication(
            carContext.applicationContext,
            AppEntryPoint::class.java
        ).mediaRepository()
    }

    init {
        lifecycleScope.launch {
            try {
                items = repo.getItems(source, parentId)
                prefetchThumbnails()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load media"
            } finally {
                isLoading = false
                invalidate()
            }
        }
    }

    @OptIn(ExperimentalCarApi::class)
    override fun onGetTemplate(): Template {
        if (isLoading) {
            return GridTemplate.Builder()
                .setTitle(parentId?.let { "Loading…" } ?: source.displayName)
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build()
        }

        val listBuilder = ItemList.Builder()
        if (items.isEmpty()) {
            listBuilder.setNoItemsMessage(errorMessage ?: "No media found")
        } else {
            // AA limits grid to 6 items while driving; more when parked
            items.forEach { item -> listBuilder.addItem(buildGridItem(item)) }
        }

        return GridTemplate.Builder()
            .setTitle(parentId?.let { items.firstOrNull()?.subtitle ?: "Browse" } ?: source.displayName)
            .setHeaderAction(Action.BACK)
            .setItemSize(GridTemplate.ITEM_SIZE_LARGE)
            .setSingleList(listBuilder.build())
            .build()
    }

    private fun buildGridItem(item: MediaItem): GridItem {
        val builder = GridItem.Builder().setTitle(item.title)
        item.subtitle?.let { builder.setText(it) }

        // Use pre-fetched bitmap if available, else fall back to static drawable
        val cachedBitmap = thumbnails[item.id]
        val icon = if (cachedBitmap != null) {
            CarIcon.Builder(IconCompat.createWithBitmap(cachedBitmap)).build()
        } else {
            val fallbackRes = if (item.isFolder) R.drawable.ic_folder else R.drawable.ic_video_file
            CarIcon.Builder(IconCompat.createWithResource(carContext, fallbackRes)).build()
        }

        builder.setImage(icon, GridItem.IMAGE_TYPE_LARGE)

        builder.setOnClickListener {
            if (item.isFolder) {
                screenManager.push(BrowseScreen(carContext, source, item.id))
            } else {
                screenManager.push(VideoPlaybackScreen(carContext, item))
            }
        }

        return builder.build()
    }

    private suspend fun prefetchThumbnails() = withContext(Dispatchers.IO) {
        val loader = carContext.imageLoader
        items
            .filter { it.thumbnailUrl != null }
            .map { item ->
                async {
                    try {
                        val request = ImageRequest.Builder(carContext)
                            .data(item.thumbnailUrl)
                            .size(512, 512)
                            .allowHardware(false)
                            .build()
                        val bitmap = (loader.execute(request) as? SuccessResult)
                            ?.drawable as? android.graphics.drawable.BitmapDrawable
                        bitmap?.bitmap?.let { thumbnails[item.id] = it }
                    } catch (_: Exception) {}
                }
            }
            .awaitAll()
    }
}
