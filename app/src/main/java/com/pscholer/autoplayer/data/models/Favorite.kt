package com.pscholer.autoplayer.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Favorite(
    val id: String = UUID.randomUUID().toString(),
    val mediaId: String,
    val source: String,  // LOCAL, PLEX, JELLYFIN
    val mediaTitle: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
