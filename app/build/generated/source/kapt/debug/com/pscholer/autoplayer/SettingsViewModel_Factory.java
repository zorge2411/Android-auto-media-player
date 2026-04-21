package com.pscholer.autoplayer;

import com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository;
import com.pscholer.autoplayer.util.PreferencesManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<PreferencesManager> prefsProvider;

  private final Provider<JellyfinRepository> jellyfinProvider;

  public SettingsViewModel_Factory(Provider<PreferencesManager> prefsProvider,
      Provider<JellyfinRepository> jellyfinProvider) {
    this.prefsProvider = prefsProvider;
    this.jellyfinProvider = jellyfinProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(prefsProvider.get(), jellyfinProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<PreferencesManager> prefsProvider,
      Provider<JellyfinRepository> jellyfinProvider) {
    return new SettingsViewModel_Factory(prefsProvider, jellyfinProvider);
  }

  public static SettingsViewModel newInstance(PreferencesManager prefs,
      JellyfinRepository jellyfin) {
    return new SettingsViewModel(prefs, jellyfin);
  }
}
