package com.pscholer.autoplayer.data.remote.jellyfin;

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
public final class JellyfinRepository_Factory implements Factory<JellyfinRepository> {
  private final Provider<PreferencesManager> prefsProvider;

  public JellyfinRepository_Factory(Provider<PreferencesManager> prefsProvider) {
    this.prefsProvider = prefsProvider;
  }

  @Override
  public JellyfinRepository get() {
    return newInstance(prefsProvider.get());
  }

  public static JellyfinRepository_Factory create(Provider<PreferencesManager> prefsProvider) {
    return new JellyfinRepository_Factory(prefsProvider);
  }

  public static JellyfinRepository newInstance(PreferencesManager prefs) {
    return new JellyfinRepository(prefs);
  }
}
