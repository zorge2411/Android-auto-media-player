package com.pscholer.autoplayer.data.remote.plex;

import com.pscholer.autoplayer.util.PreferencesManager;
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
public final class PlexRepository_Factory implements Factory<PlexRepository> {
  private final Provider<PreferencesManager> prefsProvider;

  public PlexRepository_Factory(Provider<PreferencesManager> prefsProvider) {
    this.prefsProvider = prefsProvider;
  }

  @Override
  public PlexRepository get() {
    return newInstance(prefsProvider.get());
  }

  public static PlexRepository_Factory create(Provider<PreferencesManager> prefsProvider) {
    return new PlexRepository_Factory(prefsProvider);
  }

  public static PlexRepository newInstance(PreferencesManager prefs) {
    return new PlexRepository(prefs);
  }
}
