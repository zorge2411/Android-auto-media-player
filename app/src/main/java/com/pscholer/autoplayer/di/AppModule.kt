package com.pscholer.autoplayer.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for app-level bindings.
 *
 * MediaRepository, MediaPlayerManager, PlexRepository, JellyfinRepository,
 * LocalMediaRepository, and PreferencesManager are all @Singleton with
 * @Inject constructors — Hilt auto-binds them without explicit @Provides.
 *
 * Add @Provides here only for third-party types that can't have @Inject
 * constructors (e.g. Retrofit, OkHttpClient if you want a shared instance).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
