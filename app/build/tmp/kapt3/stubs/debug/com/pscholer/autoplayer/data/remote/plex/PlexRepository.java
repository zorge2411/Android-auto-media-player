package com.pscholer.autoplayer.data.remote.plex;

import android.util.Log;
import com.pscholer.autoplayer.data.models.MediaItem;
import com.pscholer.autoplayer.util.PreferencesManager;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u0000 \u001e2\u00020\u0001:\u0001\u001eB\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\t\u001a\u00020\u0006H\u0002J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\bH\u0002J\u001c\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000e2\u0006\u0010\u0010\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\u0011J\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eH\u0086@\u00a2\u0006\u0002\u0010\u0013J\u0010\u0010\u0014\u001a\u00020\b2\u0006\u0010\u0015\u001a\u00020\bH\u0002J\u001c\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000e2\u0006\u0010\u0017\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\u0011J\u0018\u0010\u0018\u001a\u00020\b2\u0006\u0010\u0019\u001a\u00020\b2\u0006\u0010\u001a\u001a\u00020\bH\u0002J\u001c\u0010\u001b\u001a\u00020\u000f*\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\b2\u0006\u0010\u001a\u001a\u00020\bH\u0002R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001f"}, d2 = {"Lcom/pscholer/autoplayer/data/remote/plex/PlexRepository;", "", "prefs", "Lcom/pscholer/autoplayer/util/PreferencesManager;", "(Lcom/pscholer/autoplayer/util/PreferencesManager;)V", "cachedApi", "Lcom/pscholer/autoplayer/data/remote/plex/PlexApi;", "cachedServerUrl", "", "api", "buildRetrofit", "Lretrofit2/Retrofit;", "baseUrl", "getChildren", "", "Lcom/pscholer/autoplayer/data/models/MediaItem;", "ratingKey", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getLibraries", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getSanitizedUrl", "url", "getSectionItems", "sectionId", "thumbUrl", "path", "token", "toMediaItem", "Lcom/pscholer/autoplayer/data/remote/plex/PlexMetadata;", "serverUrl", "Companion", "app_debug"})
public final class PlexRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.util.PreferencesManager prefs = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "PlexRepository";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CLIENT_IDENTIFIER = "auto-player-android-auto";
    @org.jetbrains.annotations.Nullable()
    private com.pscholer.autoplayer.data.remote.plex.PlexApi cachedApi;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String cachedServerUrl = "";
    @org.jetbrains.annotations.NotNull()
    public static final com.pscholer.autoplayer.data.remote.plex.PlexRepository.Companion Companion = null;
    
    @javax.inject.Inject()
    public PlexRepository(@org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.util.PreferencesManager prefs) {
        super();
    }
    
    private final java.lang.String getSanitizedUrl(java.lang.String url) {
        return null;
    }
    
    private final com.pscholer.autoplayer.data.remote.plex.PlexApi api() {
        return null;
    }
    
    private final retrofit2.Retrofit buildRetrofit(java.lang.String baseUrl) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getLibraries(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pscholer.autoplayer.data.models.MediaItem>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getSectionItems(@org.jetbrains.annotations.NotNull()
    java.lang.String sectionId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pscholer.autoplayer.data.models.MediaItem>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getChildren(@org.jetbrains.annotations.NotNull()
    java.lang.String ratingKey, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pscholer.autoplayer.data.models.MediaItem>> $completion) {
        return null;
    }
    
    private final java.lang.String thumbUrl(java.lang.String path, java.lang.String token) {
        return null;
    }
    
    private final com.pscholer.autoplayer.data.models.MediaItem toMediaItem(com.pscholer.autoplayer.data.remote.plex.PlexMetadata $this$toMediaItem, java.lang.String serverUrl, java.lang.String token) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/pscholer/autoplayer/data/remote/plex/PlexRepository$Companion;", "", "()V", "CLIENT_IDENTIFIER", "", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}