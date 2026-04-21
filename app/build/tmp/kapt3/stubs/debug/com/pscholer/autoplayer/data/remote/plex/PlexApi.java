package com.pscholer.autoplayer.data.remote.plex;

import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\"\u0010\u0002\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\u00052\b\b\u0001\u0010\u0006\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0007J\u0018\u0010\b\u001a\u00020\t2\b\b\u0001\u0010\u0006\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\nJ6\u0010\u000b\u001a\u00020\u00032\b\b\u0001\u0010\f\u001a\u00020\u00052\b\b\u0001\u0010\u0006\u001a\u00020\u00052\b\b\u0003\u0010\r\u001a\u00020\u000e2\b\b\u0003\u0010\u000f\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\u0010\u00a8\u0006\u0011"}, d2 = {"Lcom/pscholer/autoplayer/data/remote/plex/PlexApi;", "", "getChildren", "Lcom/pscholer/autoplayer/data/remote/plex/PlexMediaResponse;", "ratingKey", "", "token", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getLibraries", "Lcom/pscholer/autoplayer/data/remote/plex/PlexLibraryResponse;", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getSectionItems", "sectionId", "start", "", "size", "(Ljava/lang/String;Ljava/lang/String;IILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface PlexApi {
    
    /**
     * List all library sections (Movies, TV Shows, etc.)
     */
    @retrofit2.http.GET(value = "/library/sections")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLibraries(@retrofit2.http.Header(value = "X-Plex-Token")
    @org.jetbrains.annotations.NotNull()
    java.lang.String token, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pscholer.autoplayer.data.remote.plex.PlexLibraryResponse> $completion);
    
    /**
     * All items in a library section. type=1 → Movie, type=4 → TV Show
     */
    @retrofit2.http.GET(value = "/library/sections/{sectionId}/all")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getSectionItems(@retrofit2.http.Path(value = "sectionId")
    @org.jetbrains.annotations.NotNull()
    java.lang.String sectionId, @retrofit2.http.Header(value = "X-Plex-Token")
    @org.jetbrains.annotations.NotNull()
    java.lang.String token, @retrofit2.http.Query(value = "X-Plex-Container-Start")
    int start, @retrofit2.http.Query(value = "X-Plex-Container-Size")
    int size, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pscholer.autoplayer.data.remote.plex.PlexMediaResponse> $completion);
    
    /**
     * Children of a metadata item (seasons under a show, episodes under a season)
     */
    @retrofit2.http.GET(value = "/library/metadata/{ratingKey}/children")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getChildren(@retrofit2.http.Path(value = "ratingKey")
    @org.jetbrains.annotations.NotNull()
    java.lang.String ratingKey, @retrofit2.http.Header(value = "X-Plex-Token")
    @org.jetbrains.annotations.NotNull()
    java.lang.String token, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pscholer.autoplayer.data.remote.plex.PlexMediaResponse> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}