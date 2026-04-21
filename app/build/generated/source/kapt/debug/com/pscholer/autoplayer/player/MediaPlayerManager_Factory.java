package com.pscholer.autoplayer.player;

import android.content.Context;
import com.pscholer.autoplayer.data.PlaybackRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class MediaPlayerManager_Factory implements Factory<MediaPlayerManager> {
  private final Provider<Context> contextProvider;

  private final Provider<PlaybackRepository> playbackRepositoryProvider;

  public MediaPlayerManager_Factory(Provider<Context> contextProvider,
      Provider<PlaybackRepository> playbackRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.playbackRepositoryProvider = playbackRepositoryProvider;
  }

  @Override
  public MediaPlayerManager get() {
    return newInstance(contextProvider.get(), playbackRepositoryProvider.get());
  }

  public static MediaPlayerManager_Factory create(Provider<Context> contextProvider,
      Provider<PlaybackRepository> playbackRepositoryProvider) {
    return new MediaPlayerManager_Factory(contextProvider, playbackRepositoryProvider);
  }

  public static MediaPlayerManager newInstance(Context context,
      PlaybackRepository playbackRepository) {
    return new MediaPlayerManager(context, playbackRepository);
  }
}
