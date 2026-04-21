package com.pscholer.autoplayer.data.remote.plex

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface PlexApi {

    /** List all library sections (Movies, TV Shows, etc.) */
    @GET("/library/sections")
    suspend fun getLibraries(
        @Header("X-Plex-Token") token: String
    ): PlexLibraryResponse

    /** All items in a library section. type=1 → Movie, type=4 → TV Show */
    @GET("/library/sections/{sectionId}/all")
    suspend fun getSectionItems(
        @Path("sectionId") sectionId: String,
        @Header("X-Plex-Token") token: String,
        @Query("X-Plex-Container-Start") start: Int = 0,
        @Query("X-Plex-Container-Size") size: Int = 100
    ): PlexMediaResponse

    /** Children of a metadata item (seasons under a show, episodes under a season) */
    @GET("/library/metadata/{ratingKey}/children")
    suspend fun getChildren(
        @Path("ratingKey") ratingKey: String,
        @Header("X-Plex-Token") token: String
    ): PlexMediaResponse

    /** Fetch a single metadata item by ratingKey */
    @GET("/library/metadata/{ratingKey}")
    suspend fun getItem(
        @Path("ratingKey") ratingKey: String,
        @Header("X-Plex-Token") token: String
    ): PlexMediaResponse

    /*
     * Direct Play URL (constructed client-side — not an API call):
     *   {serverUrl}{part.key}?X-Plex-Token={token}
     * e.g.
     *   http://192.168.1.10:32400/library/parts/12345/file.mkv?X-Plex-Token=abc123
     */
}
