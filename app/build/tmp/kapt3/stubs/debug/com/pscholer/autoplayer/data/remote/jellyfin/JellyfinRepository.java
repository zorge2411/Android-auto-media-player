package com.pscholer.autoplayer.data.remote.jellyfin;

import android.util.Log;
import com.google.gson.GsonBuilder;
import com.pscholer.autoplayer.data.models.MediaItem;
import com.pscholer.autoplayer.util.PreferencesManager;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000  2\u00020\u0001:\u0001 B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u000b\u001a\u00020\fH\u0086@\u00a2\u0006\u0002\u0010\rJ\u0010\u0010\u000e\u001a\u00020\u00062\u0006\u0010\u000f\u001a\u00020\bH\u0002J\u000e\u0010\u0010\u001a\u00020\u0011H\u0082@\u00a2\u0006\u0002\u0010\rJ \u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00140\u00132\n\b\u0002\u0010\u0015\u001a\u0004\u0018\u00010\bH\u0086@\u00a2\u0006\u0002\u0010\u0016J\u0010\u0010\u0017\u001a\u00020\b2\u0006\u0010\u0018\u001a\u00020\bH\u0002J\u0014\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00140\u0013H\u0086@\u00a2\u0006\u0002\u0010\rJ&\u0010\u001a\u001a\u00020\b2\u0006\u0010\u000f\u001a\u00020\b2\u0006\u0010\u001b\u001a\u00020\b2\u0006\u0010\u001c\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010\u001dJ\f\u0010\u001e\u001a\u00020\u0014*\u00020\u001fH\u0002R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006!"}, d2 = {"Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinRepository;", "", "prefs", "Lcom/pscholer/autoplayer/util/PreferencesManager;", "(Lcom/pscholer/autoplayer/util/PreferencesManager;)V", "api", "Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinApi;", "authToken", "", "cachedServerUrl", "userId", "authenticate", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "buildApi", "serverUrl", "ensureAuthenticated", "", "getItems", "", "Lcom/pscholer/autoplayer/data/models/MediaItem;", "parentId", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getSanitizedUrl", "url", "getViews", "testAndSave", "username", "password", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "toMediaItem", "Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinItem;", "Companion", "app_debug"})
public final class JellyfinRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.util.PreferencesManager prefs = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "JellyfinRepository";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CLIENT_HEADER = "MediaBrowser Client=\"AutoPlayer\", Device=\"Android\", DeviceId=\"auto-player-001\", Version=\"1.0\"";
    @org.jetbrains.annotations.Nullable()
    private com.pscholer.autoplayer.data.remote.jellyfin.JellyfinApi api;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String userId = "";
    @org.jetbrains.annotations.NotNull()
    private java.lang.String authToken = "";
    @org.jetbrains.annotations.NotNull()
    private java.lang.String cachedServerUrl = "";
    @org.jetbrains.annotations.NotNull()
    public static final com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository.Companion Companion = null;
    
    @javax.inject.Inject()
    public JellyfinRepository(@org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.util.PreferencesManager prefs) {
        super();
    }
    
    private final java.lang.String getSanitizedUrl(java.lang.String url) {
        return null;
    }
    
    private final com.pscholer.autoplayer.data.remote.jellyfin.JellyfinApi buildApi(java.lang.String serverUrl) {
        return null;
    }
    
    /**
     * Verify credentials by actually connecting to the server before saving anything.
     * Only persists config + session token if auth succeeds.
     * Returns the authenticated display name on success, or throws on failure.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object testAndSave(@org.jetbrains.annotations.NotNull()
    java.lang.String serverUrl, @org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String password, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    /**
     * Re-authenticate using already-persisted credentials.
     * Used internally when the cached session token is missing.
     *
     * Returns true on success, false on any failure.  All failure paths log
     * enough information to diagnose the problem without having to reproduce it.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object authenticate(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getViews(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pscholer.autoplayer.data.models.MediaItem>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getItems(@org.jetbrains.annotations.Nullable()
    java.lang.String parentId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pscholer.autoplayer.data.models.MediaItem>> $completion) {
        return null;
    }
    
    private final java.lang.Object ensureAuthenticated(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final com.pscholer.autoplayer.data.models.MediaItem toMediaItem(com.pscholer.autoplayer.data.remote.jellyfin.JellyfinItem $this$toMediaItem) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2 = {"Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinRepository$Companion;", "", "()V", "CLIENT_HEADER", "", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}