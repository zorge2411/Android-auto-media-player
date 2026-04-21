package com.pscholer.autoplayer.data.remote.jellyfin

import com.google.gson.annotations.SerializedName

data class JellyfinAuthRequest(
    @SerializedName("Username") val username: String,
    @SerializedName("Pw") val password: String
)

data class JellyfinAuthResponse(
    @SerializedName("AccessToken") val accessToken: String,
    @SerializedName("User") val user: JellyfinUser
)

data class JellyfinUser(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String
)

data class JellyfinItemsResponse(
    @SerializedName("Items") val items: List<JellyfinItem> = emptyList(),
    @SerializedName("TotalRecordCount") val total: Int = 0
)

data class JellyfinItem(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Type") val type: String,
    // "Movie" | "Series" | "Season" | "Episode" | "CollectionFolder" | "BoxSet"
    @SerializedName("ImageTags") val imageTags: Map<String, String>? = null,
    @SerializedName("SeriesName") val seriesName: String? = null,
    @SerializedName("SeasonName") val seasonName: String? = null,
    @SerializedName("IndexNumber") val indexNumber: Int? = null,      // episode #
    @SerializedName("ParentIndexNumber") val parentIndexNumber: Int? = null, // season #
    @SerializedName("ProductionYear") val year: Int? = null,
    @SerializedName("RunTimeTicks") val runtimeTicks: Long? = null,   // 10,000,000 ticks/sec
    @SerializedName("MediaSources") val mediaSources: List<JellyfinMediaSource>? = null
)

data class JellyfinMediaSource(
    @SerializedName("Id") val id: String,
    @SerializedName("Container") val container: String? = null,
    @SerializedName("SupportsDirectPlay") val supportsDirectPlay: Boolean = false,
    @SerializedName("SupportsDirectStream") val supportsDirectStream: Boolean = false
)
