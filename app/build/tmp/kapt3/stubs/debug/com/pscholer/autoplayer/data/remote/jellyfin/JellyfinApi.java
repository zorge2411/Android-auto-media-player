package com.pscholer.autoplayer.data.remote.jellyfin;

import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0007\bf\u0018\u00002\u00020\u0001J(\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\tJ,\u0010\n\u001a\u00020\u000b2\b\b\u0001\u0010\f\u001a\u00020\b2\b\b\u0001\u0010\r\u001a\u00020\b2\b\b\u0001\u0010\u000e\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\u000fJt\u0010\u0010\u001a\u00020\u00112\b\b\u0001\u0010\f\u001a\u00020\b2\b\b\u0001\u0010\u000e\u001a\u00020\b2\n\b\u0003\u0010\u0012\u001a\u0004\u0018\u00010\b2\b\b\u0003\u0010\u0013\u001a\u00020\b2\b\b\u0003\u0010\u0014\u001a\u00020\u00152\b\b\u0003\u0010\u0016\u001a\u00020\b2\b\b\u0003\u0010\u0017\u001a\u00020\u00182\b\b\u0003\u0010\u0019\u001a\u00020\u00182\b\b\u0003\u0010\u001a\u001a\u00020\b2\b\b\u0003\u0010\u001b\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\u001cJ\"\u0010\u001d\u001a\u00020\u00112\b\b\u0001\u0010\f\u001a\u00020\b2\b\b\u0001\u0010\u000e\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\u001e\u00a8\u0006\u001f"}, d2 = {"Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinApi;", "", "authenticate", "Lretrofit2/Response;", "Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinAuthResponse;", "request", "Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinAuthRequest;", "clientHeader", "", "(Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinAuthRequest;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getItem", "Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinItem;", "userId", "itemId", "token", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getItems", "Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinItemsResponse;", "parentId", "types", "recursive", "", "fields", "startIndex", "", "limit", "sortBy", "sortOrder", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/lang/String;IILjava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getViews", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface JellyfinApi {
    
    /**
     * Authenticate and retrieve an access token.
     * The X-Emby-Authorization header must follow the Emby/Jellyfin client format.
     *
     * Returns Response<T> so callers can inspect the HTTP status code and raw
     * error body before Gson attempts to parse it.  This prevents
     * MalformedJsonException from being thrown when the server returns an HTML
     * error page (or any non-JSON body) with a 2xx status code.
     */
    @retrofit2.http.POST(value = "/Users/AuthenticateByName")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object authenticate(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.data.remote.jellyfin.JellyfinAuthRequest request, @retrofit2.http.Header(value = "X-Emby-Authorization")
    @org.jetbrains.annotations.NotNull()
    java.lang.String clientHeader, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<com.pscholer.autoplayer.data.remote.jellyfin.JellyfinAuthResponse>> $completion);
    
    /**
     * Top-level views for the user (Movies, TV Shows, etc.)
     */
    @retrofit2.http.GET(value = "/Users/{userId}/Views")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getViews(@retrofit2.http.Path(value = "userId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String userId, @retrofit2.http.Header(value = "X-Emby-Token")
    @org.jetbrains.annotations.NotNull()
    java.lang.String token, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pscholer.autoplayer.data.remote.jellyfin.JellyfinItemsResponse> $completion);
    
    /**
     * Items inside a parent (library, season, etc.)
     */
    @retrofit2.http.GET(value = "/Users/{userId}/Items")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getItems(@retrofit2.http.Path(value = "userId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String userId, @retrofit2.http.Header(value = "X-Emby-Token")
    @org.jetbrains.annotations.NotNull()
    java.lang.String token, @retrofit2.http.Query(value = "ParentId")
    @org.jetbrains.annotations.Nullable()
    java.lang.String parentId, @retrofit2.http.Query(value = "IncludeItemTypes")
    @org.jetbrains.annotations.NotNull()
    java.lang.String types, @retrofit2.http.Query(value = "Recursive")
    boolean recursive, @retrofit2.http.Query(value = "Fields")
    @org.jetbrains.annotations.NotNull()
    java.lang.String fields, @retrofit2.http.Query(value = "StartIndex")
    int startIndex, @retrofit2.http.Query(value = "Limit")
    int limit, @retrofit2.http.Query(value = "SortBy")
    @org.jetbrains.annotations.NotNull()
    java.lang.String sortBy, @retrofit2.http.Query(value = "SortOrder")
    @org.jetbrains.annotations.NotNull()
    java.lang.String sortOrder, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pscholer.autoplayer.data.remote.jellyfin.JellyfinItemsResponse> $completion);
    
    /**
     * Fetch a single item by ID
     */
    @retrofit2.http.GET(value = "/Users/{userId}/Items/{itemId}")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getItem(@retrofit2.http.Path(value = "userId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String userId, @retrofit2.http.Path(value = "itemId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String itemId, @retrofit2.http.Header(value = "X-Emby-Token")
    @org.jetbrains.annotations.NotNull()
    java.lang.String token, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pscholer.autoplayer.data.remote.jellyfin.JellyfinItem> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}