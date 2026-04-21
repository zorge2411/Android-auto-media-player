package com.pscholer.autoplayer.data;

import com.pscholer.autoplayer.data.local.LocalMediaRepository;
import com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository;
import com.pscholer.autoplayer.data.remote.plex.PlexRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class MediaRepository_Factory implements Factory<MediaRepository> {
  private final Provider<LocalMediaRepository> localProvider;

  private final Provider<PlexRepository> plexProvider;

  private final Provider<JellyfinRepository> jellyfinProvider;

  public MediaRepository_Factory(Provider<LocalMediaRepository> localProvider,
      Provider<PlexRepository> plexProvider, Provider<JellyfinRepository> jellyfinProvider) {
    this.localProvider = localProvider;
    this.plexProvider = plexProvider;
    this.jellyfinProvider = jellyfinProvider;
  }

  @Override
  public MediaRepository get() {
    return newInstance(localProvider.get(), plexProvider.get(), jellyfinProvider.get());
  }

  public static MediaRepository_Factory create(Provider<LocalMediaRepository> localProvider,
      Provider<PlexRepository> plexProvider, Provider<JellyfinRepository> jellyfinProvider) {
    return new MediaRepository_Factory(localProvider, plexProvider, jellyfinProvider);
  }

  public static MediaRepository newInstance(LocalMediaRepository local, PlexRepository plex,
      JellyfinRepository jellyfin) {
    return new MediaRepository(local, plex, jellyfin);
  }
}
