package com.pscholer.autoplayer.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaItem(
    val id: String,
    val title: String,
    val source: String = "LOCAL",  // LOCAL, PLEX, JELLYFIN
    val isFolder: Boolean = false,
    val thumbnailUrl: String? = null,
    val streamUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val durationMs: Long = 0L,
    val mimeType: String? = null,
    val subtitle: String? = null   // e.g. "Season 2 · Episode 4"
) : Parcelable
