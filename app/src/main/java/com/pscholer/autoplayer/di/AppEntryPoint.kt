package com.pscholer.autoplayer.di

import com.pscholer.autoplayer.data.FavoriteRepository
import com.pscholer.autoplayer.data.MediaRepository
import com.pscholer.autoplayer.data.PlaybackRepository
import com.pscholer.autoplayer.player.MediaPlayerManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point for Car App Library's Screen and Session classes,
 * which cannot use constructor injection.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppEntryPoint {
    fun mediaRepository(): MediaRepository
    fun playerManager(): MediaPlayerManager
    fun playbackRepository(): PlaybackRepository
    fun favoriteRepository(): FavoriteRepository
}
