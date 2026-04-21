package com.pscholer.autoplayer.data;

import com.pscholer.autoplayer.data.local.LocalMediaRepository;
import com.pscholer.autoplayer.data.models.MediaItem;
import com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository;
import com.pscholer.autoplayer.data.remote.plex.PlexRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Unified facade — routes browse/list calls to the correct data source.
 *
 * parentId semantics per source:
 * LOCAL    — null → MediaStore scan; non-null → ignored (flat list only)
 * PLEX     — null → library sections; non-null → section items or children
 * JELLYFIN — null → user views; non-null → items under that parent
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ&\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\n2\u0006\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u0086@\u00a2\u0006\u0002\u0010\u0010R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/pscholer/autoplayer/data/MediaRepository;", "", "local", "Lcom/pscholer/autoplayer/data/local/LocalMediaRepository;", "plex", "Lcom/pscholer/autoplayer/data/remote/plex/PlexRepository;", "jellyfin", "Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinRepository;", "(Lcom/pscholer/autoplayer/data/local/LocalMediaRepository;Lcom/pscholer/autoplayer/data/remote/plex/PlexRepository;Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinRepository;)V", "getItems", "", "Lcom/pscholer/autoplayer/data/models/MediaItem;", "source", "Lcom/pscholer/autoplayer/data/MediaSource;", "parentId", "", "(Lcom/pscholer/autoplayer/data/MediaSource;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class MediaRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.data.local.LocalMediaRepository local = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.data.remote.plex.PlexRepository plex = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository jellyfin = null;
    
    @javax.inject.Inject()
    public MediaRepository(@org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.data.local.LocalMediaRepository local, @org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.data.remote.plex.PlexRepository plex, @org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository jellyfin) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getItems(@org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.data.MediaSource source, @org.jetbrains.annotations.Nullable()
    java.lang.String parentId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pscholer.autoplayer.data.models.MediaItem>> $completion) {
        return null;
    }
}