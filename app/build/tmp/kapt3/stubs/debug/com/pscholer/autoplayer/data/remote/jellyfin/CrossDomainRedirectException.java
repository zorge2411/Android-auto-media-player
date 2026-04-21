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

/**
 * Thrown when OkHttp follows a redirect to a different host than the original request.
 * This indicates the server URL routes through an auth proxy (e.g. Pangolin, Authentik,
 * Authelia) that intercepts unauthenticated requests and redirects them to a login portal.
 *
 * Must extend IOException so OkHttp interceptors propagate it correctly through the
 * call chain instead of crashing the Dispatcher thread.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0007\u00a8\u0006\t"}, d2 = {"Lcom/pscholer/autoplayer/data/remote/jellyfin/CrossDomainRedirectException;", "Ljava/io/IOException;", "originalHost", "", "redirectedHost", "(Ljava/lang/String;Ljava/lang/String;)V", "getOriginalHost", "()Ljava/lang/String;", "getRedirectedHost", "app_debug"})
public final class CrossDomainRedirectException extends java.io.IOException {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String originalHost = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String redirectedHost = null;
    
    public CrossDomainRedirectException(@org.jetbrains.annotations.NotNull()
    java.lang.String originalHost, @org.jetbrains.annotations.NotNull()
    java.lang.String redirectedHost) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getOriginalHost() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getRedirectedHost() {
        return null;
    }
}