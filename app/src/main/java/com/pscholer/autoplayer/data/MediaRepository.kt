package com.pscholer.autoplayer.data

import com.pscholer.autoplayer.data.local.LocalMediaRepository
import com.pscholer.autoplayer.data.models.MediaItem
import com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository
import com.pscholer.autoplayer.data.remote.plex.PlexRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified facade — routes browse/list calls to the correct data source.
 *
 * parentId semantics per source:
 *  LOCAL    — null → MediaStore scan; non-null → ignored (flat list only)
 *  PLEX     — null → library sections; non-null → section items or children
 *  JELLYFIN — null → user views; non-null → items under that parent
 */
@Singleton
class MediaRepository @Inject constructor(
    private val local: LocalMediaRepository,
    private val plex: PlexRepository,
    private val jellyfin: JellyfinRepository
) {
    suspend fun getItems(source: MediaSource, parentId: String?): List<MediaItem> =
        when (source) {
            MediaSource.LOCAL -> {
                if (parentId == null) {
                    // Combine MediaStore results (all videos) with any explicitly added SAF folders
                    val mediaStoreItems = local.scanMediaStore()
                    val safFolders = local.getSafFolders()
                    safFolders + mediaStoreItems
                } else {
                    local.scanSafDirectory(android.net.Uri.parse(parentId))
                }
            }

            MediaSource.PLEX -> {
                if (parentId == null) plex.getLibraries()
                else plex.getChildren(parentId)
            }

            MediaSource.JELLYFIN -> {
                if (parentId == null) jellyfin.getViews()
                else jellyfin.getItems(parentId)
            }
        }

    suspend fun getItem(source: MediaSource, id: String, title: String): MediaItem? =
        when (source) {
            MediaSource.LOCAL -> local.getItem(id, title)
            MediaSource.PLEX -> plex.getItem(id)
            MediaSource.JELLYFIN -> jellyfin.getItem(id)
        }
}
