package com.pscholer.autoplayer.data.remote.plex

import com.google.gson.annotations.SerializedName

data class PlexLibraryResponse(
    @SerializedName("MediaContainer") val container: PlexLibraryContainer
)

data class PlexLibraryContainer(
    @SerializedName("Directory") val sections: List<PlexSection> = emptyList()
)

data class PlexSection(
    val key: String,
    val title: String,
    val type: String,       // "movie" | "show" | "music"
    val thumb: String? = null,
    val art: String? = null
)

data class PlexMediaResponse(
    @SerializedName("MediaContainer") val container: PlexMediaContainer
)

data class PlexMediaContainer(
    @SerializedName("Metadata") val items: List<PlexMetadata> = emptyList(),
    val totalSize: Int = 0,
    val size: Int = 0
)

data class PlexMetadata(
    val ratingKey: String,
    val key: String,
    val title: String,
    val type: String,           // "movie" | "show" | "season" | "episode"
    val thumb: String? = null,
    val art: String? = null,
    val year: Int? = null,
    val duration: Long? = null,
    @SerializedName("parentTitle") val parentTitle: String? = null,
    @SerializedName("grandparentTitle") val grandparentTitle: String? = null,
    @SerializedName("index") val index: Int? = null,            // episode/season number
    @SerializedName("parentIndex") val parentIndex: Int? = null,
    @SerializedName("Media") val media: List<PlexMedia>? = null
)

data class PlexMedia(
    val id: String,
    val duration: Long? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val container: String? = null,
    @SerializedName("Part") val parts: List<PlexPart> = emptyList()
)

data class PlexPart(
    val id: String,
    val key: String,            // e.g. /library/parts/12345/file.mkv
    val file: String,
    val size: Long = 0L,
    val container: String? = null
)
