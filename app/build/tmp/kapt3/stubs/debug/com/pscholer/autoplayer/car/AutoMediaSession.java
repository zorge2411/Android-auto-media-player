package com.pscholer.autoplayer.car;

import android.content.Intent;
import android.content.res.Configuration;
import androidx.car.app.AppManager;
import androidx.car.app.Screen;
import androidx.car.app.Session;
import androidx.lifecycle.Lifecycle;
import com.pscholer.autoplayer.car.screens.RootScreen;
import com.pscholer.autoplayer.car.surface.VideoSurfaceRenderer;
import com.pscholer.autoplayer.di.AppEntryPoint;
import com.pscholer.autoplayer.util.NavigationStack;
import dagger.hilt.android.EntryPointAccessors;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0016J\u0010\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0010H\u0016R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006R\u000e\u0010\u0007\u001a\u00020\bX\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/pscholer/autoplayer/car/AutoMediaSession;", "Landroidx/car/app/Session;", "()V", "navigationStack", "Lcom/pscholer/autoplayer/util/NavigationStack;", "getNavigationStack", "()Lcom/pscholer/autoplayer/util/NavigationStack;", "surfaceRenderer", "Lcom/pscholer/autoplayer/car/surface/VideoSurfaceRenderer;", "onCarConfigurationChanged", "", "newConfiguration", "Landroid/content/res/Configuration;", "onCreateScreen", "Landroidx/car/app/Screen;", "intent", "Landroid/content/Intent;", "app_debug"})
public final class AutoMediaSession extends androidx.car.app.Session {
    private com.pscholer.autoplayer.car.surface.VideoSurfaceRenderer surfaceRenderer;
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.util.NavigationStack navigationStack = null;
    
    public AutoMediaSession() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pscholer.autoplayer.util.NavigationStack getNavigationStack() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public androidx.car.app.Screen onCreateScreen(@org.jetbrains.annotations.NotNull()
    android.content.Intent intent) {
        return null;
    }
    
    @java.lang.Override()
    public void onCarConfigurationChanged(@org.jetbrains.annotations.NotNull()
    android.content.res.Configuration newConfiguration) {
    }
}