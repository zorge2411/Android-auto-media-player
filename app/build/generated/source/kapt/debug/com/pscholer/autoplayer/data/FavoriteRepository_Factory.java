package com.pscholer.autoplayer.data;

import android.content.Context;
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
public final class FavoriteRepository_Factory implements Factory<FavoriteRepository> {
  private final Provider<Context> contextProvider;

  public FavoriteRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public FavoriteRepository get() {
    return newInstance(contextProvider.get());
  }

  public static FavoriteRepository_Factory create(Provider<Context> contextProvider) {
    return new FavoriteRepository_Factory(contextProvider);
  }

  public static FavoriteRepository newInstance(Context context) {
    return new FavoriteRepository(context);
  }
}
