package com.pscholer.autoplayer.di;

import com.pscholer.autoplayer.data.FavoriteRepository;
import com.pscholer.autoplayer.data.MediaRepository;
import com.pscholer.autoplayer.data.PlaybackRepository;
import com.pscholer.autoplayer.player.MediaPlayerManager;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt entry point for Car App Library's Screen and Session classes,
 * which cannot use constructor injection.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\bg\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\b\u0010\u0004\u001a\u00020\u0005H&J\b\u0010\u0006\u001a\u00020\u0007H&J\b\u0010\b\u001a\u00020\tH&\u00a8\u0006\n"}, d2 = {"Lcom/pscholer/autoplayer/di/AppEntryPoint;", "", "favoriteRepository", "Lcom/pscholer/autoplayer/data/FavoriteRepository;", "mediaRepository", "Lcom/pscholer/autoplayer/data/MediaRepository;", "playbackRepository", "Lcom/pscholer/autoplayer/data/PlaybackRepository;", "playerManager", "Lcom/pscholer/autoplayer/player/MediaPlayerManager;", "app_debug"})
@dagger.hilt.EntryPoint()
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public abstract interface AppEntryPoint {
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pscholer.autoplayer.data.MediaRepository mediaRepository();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pscholer.autoplayer.player.MediaPlayerManager playerManager();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pscholer.autoplayer.data.PlaybackRepository playbackRepository();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pscholer.autoplayer.data.FavoriteRepository favoriteRepository();
}