package com.pscholer.autoplayer.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackState(
    val mediaId: String,
    val source: String,  // LOCAL, PLEX, JELLYFIN
    val positionMs: Long,
    val durationMs: Long,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun isResumable(watchedThreshold: Float = 0.95f): Boolean =
        durationMs > 0 && positionMs < (durationMs * watchedThreshold)
}
