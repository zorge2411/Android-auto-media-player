package com.pscholer.autoplayer.data.local;

import android.content.Context;
import com.pscholer.autoplayer.util.PreferencesManager;
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
public final class LocalMediaRepository_Factory implements Factory<LocalMediaRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<PreferencesManager> prefsProvider;

  public LocalMediaRepository_Factory(Provider<Context> contextProvider,
      Provider<PreferencesManager> prefsProvider) {
    this.contextProvider = contextProvider;
    this.prefsProvider = prefsProvider;
  }

  @Override
  public LocalMediaRepository get() {
    return newInstance(contextProvider.get(), prefsProvider.get());
  }

  public static LocalMediaRepository_Factory create(Provider<Context> contextProvider,
      Provider<PreferencesManager> prefsProvider) {
    return new LocalMediaRepository_Factory(contextProvider, prefsProvider);
  }

  public static LocalMediaRepository newInstance(Context context, PreferencesManager prefs) {
    return new LocalMediaRepository(context, prefs);
  }
}
