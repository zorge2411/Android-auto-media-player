package com.pscholer.autoplayer.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val owner: String = "LOCAL",
    val videos: List<PlaylistItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class PlaylistItem(
    val mediaId: String,
    val source: String,
    val addedAt: Long = System.currentTimeMillis()
)
