package com.pscholer.autoplayer.data.remote.jellyfin

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface JellyfinApi {

    /**
     * Authenticate and retrieve an access token.
     * The X-Emby-Authorization header must follow the Emby/Jellyfin client format.
     *
     * Returns Response<T> so callers can inspect the HTTP status code and raw
     * error body before Gson attempts to parse it.  This prevents
     * MalformedJsonException from being thrown when the server returns an HTML
     * error page (or any non-JSON body) with a 2xx status code.
     */
    @POST("/Users/AuthenticateByName")
    suspend fun authenticate(
        @Body request: JellyfinAuthRequest,
        @Header("X-Emby-Authorization") clientHeader: String
    ): Response<JellyfinAuthResponse>

    /** Top-level views for the user (Movies, TV Shows, etc.) */
    @GET("/Users/{userId}/Views")
    suspend fun getViews(
        @Path("userId") userId: String,
        @Header("X-Emby-Token") token: String
    ): JellyfinItemsResponse

    /** Items inside a parent (library, season, etc.) */
    @GET("/Users/{userId}/Items")
    suspend fun getItems(
        @Path("userId") userId: String,
        @Header("X-Emby-Token") token: String,
        @Query("ParentId") parentId: String? = null,
        @Query("IncludeItemTypes") types: String = "Movie,Series,Episode,Season",
        @Query("Recursive") recursive: Boolean = false,
        @Query("Fields") fields: String = "MediaSources,Overview",
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 100,
        @Query("SortBy") sortBy: String = "SortName",
    @Query("SortOrder") sortOrder: String = "Ascending"
    ): JellyfinItemsResponse

    /** Fetch a single item by ID */
    @GET("/Users/{userId}/Items/{itemId}")
    suspend fun getItem(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
        @Header("X-Emby-Token") token: String
    ): JellyfinItem

    /*
     * Direct Stream URL (constructed client-side):
     *   {serverUrl}/Videos/{itemId}/stream?static=true&api_key={token}
     *
     * For transcoded stream (fallback):
     *   {serverUrl}/Videos/{itemId}/stream.mp4?api_key={token}&...transcode params
     */
}
