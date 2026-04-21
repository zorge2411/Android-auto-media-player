package com.pscholer.autoplayer.car.surface;

import android.graphics.Rect;
import android.util.Log;
import android.view.Surface;
import androidx.car.app.CarContext;
import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;
import com.pscholer.autoplayer.player.MediaPlayerManager;
import com.pscholer.autoplayer.util.AspectRatioCalculator;
import kotlinx.coroutines.flow.StateFlow;

/**
 * Bridges the Car App Surface to ExoPlayer. Invariants:
 * - Never hand a Surface to ExoPlayer after onSurfaceDestroyed; always clearVideoSurface first.
 * - Requires ACCESS_SURFACE + NAVIGATION category (MEDIA category sandboxes Surface access).
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\\\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\u0018\u0000 $2\u00020\u0001:\u0002$%B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0006\u0010\u0015\u001a\u00020\u0016J\u0006\u0010\u0017\u001a\u00020\u0018J\u0010\u0010\u0019\u001a\u00020\u00182\u0006\u0010\u001a\u001a\u00020\u001bH\u0016J\u0010\u0010\u001c\u001a\u00020\u00182\u0006\u0010\u001d\u001a\u00020\u001eH\u0016J\u0010\u0010\u001f\u001a\u00020\u00182\u0006\u0010\u001d\u001a\u00020\u001eH\u0016J\u000e\u0010 \u001a\u00020\u00182\u0006\u0010!\u001a\u00020\rJ\u0010\u0010\"\u001a\u00020\u00182\u0006\u0010#\u001a\u00020\u001bH\u0016R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\t0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006&"}, d2 = {"Lcom/pscholer/autoplayer/car/surface/VideoSurfaceRenderer;", "Landroidx/car/app/SurfaceCallback;", "carContext", "Landroidx/car/app/CarContext;", "playerManager", "Lcom/pscholer/autoplayer/player/MediaPlayerManager;", "(Landroidx/car/app/CarContext;Lcom/pscholer/autoplayer/player/MediaPlayerManager;)V", "_state", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pscholer/autoplayer/car/surface/VideoSurfaceRenderer$SurfaceState;", "activeSurface", "Landroid/view/Surface;", "lastVideoSize", "Lcom/pscholer/autoplayer/player/MediaPlayerManager$VideoSize;", "state", "Lkotlinx/coroutines/flow/StateFlow;", "getState", "()Lkotlinx/coroutines/flow/StateFlow;", "surfaceHeight", "", "surfaceWidth", "hasSurface", "", "onConfigurationChanged", "", "onStableAreaChanged", "stableArea", "Landroid/graphics/Rect;", "onSurfaceAvailable", "surfaceContainer", "Landroidx/car/app/SurfaceContainer;", "onSurfaceDestroyed", "onVideoSizeChanged", "videoSize", "onVisibleAreaChanged", "visibleArea", "Companion", "SurfaceState", "app_debug"})
public final class VideoSurfaceRenderer implements androidx.car.app.SurfaceCallback {
    @org.jetbrains.annotations.NotNull()
    private final androidx.car.app.CarContext carContext = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.player.MediaPlayerManager playerManager = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "VideoSurfaceRenderer";
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pscholer.autoplayer.car.surface.VideoSurfaceRenderer.SurfaceState> _state = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.car.surface.VideoSurfaceRenderer.SurfaceState> state = null;
    @org.jetbrains.annotations.Nullable()
    private android.view.Surface activeSurface;
    private int surfaceWidth = 0;
    private int surfaceHeight = 0;
    @org.jetbrains.annotations.Nullable()
    private com.pscholer.autoplayer.player.MediaPlayerManager.VideoSize lastVideoSize;
    @org.jetbrains.annotations.NotNull()
    public static final com.pscholer.autoplayer.car.surface.VideoSurfaceRenderer.Companion Companion = null;
    
    public VideoSurfaceRenderer(@org.jetbrains.annotations.NotNull()
    androidx.car.app.CarContext carContext, @org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.player.MediaPlayerManager playerManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.car.surface.VideoSurfaceRenderer.SurfaceState> getState() {
        return null;
    }
    
    /**
     * Called when the car display Surface becomes ready.
     * Wire it into ExoPlayer immediately so video starts rendering.
     */
    @java.lang.Override()
    public void onSurfaceAvailable(@org.jetbrains.annotations.NotNull()
    androidx.car.app.SurfaceContainer surfaceContainer) {
    }
    
    /**
     * Called when the visible portion of the Surface changes.
     * "Visible" excludes areas hidden by template chrome (status bar, action strip, etc.).
     * Use this rect to letterbox/pillarbox content if desired.
     */
    @java.lang.Override()
    public void onVisibleAreaChanged(@org.jetbrains.annotations.NotNull()
    android.graphics.Rect visibleArea) {
    }
    
    /**
     * The stable area is guaranteed never to be obscured, even transiently
     * (e.g. no overlapping toasts or temporary UI). Safe zone for HUD overlays.
     */
    @java.lang.Override()
    public void onStableAreaChanged(@org.jetbrains.annotations.NotNull()
    android.graphics.Rect stableArea) {
    }
    
    /**
     * Called when the car disconnects, session ends, or surface is recycled
     * due to a configuration change. MUST detach from ExoPlayer immediately.
     */
    @java.lang.Override()
    public void onSurfaceDestroyed(@org.jetbrains.annotations.NotNull()
    androidx.car.app.SurfaceContainer surfaceContainer) {
    }
    
    /**
     * Re-attach the surface to ExoPlayer after a configuration change.
     */
    public final void onConfigurationChanged() {
    }
    
    public final boolean hasSurface() {
        return false;
    }
    
    public final void onVideoSizeChanged(@org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.player.MediaPlayerManager.VideoSize videoSize) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/pscholer/autoplayer/car/surface/VideoSurfaceRenderer$Companion;", "", "()V", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0002\u0003\u0004B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0002\u0005\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/pscholer/autoplayer/car/surface/VideoSurfaceRenderer$SurfaceState;", "", "()V", "Available", "Unavailable", "Lcom/pscholer/autoplayer/car/surface/VideoSurfaceRenderer$SurfaceState$Available;", "Lcom/pscholer/autoplayer/car/surface/VideoSurfaceRenderer$SurfaceState$Unavailable;", "app_debug"})
    public static abstract class SurfaceState {
        
        private SurfaceState() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0013\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B9\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u0012\u0006\u0010\u0007\u001a\u00020\u0005\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u0012\b\b\u0002\u0010\n\u001a\u00020\t\u00a2\u0006\u0002\u0010\u000bJ\t\u0010\u0015\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0019\u001a\u00020\tH\u00c6\u0003J\t\u0010\u001a\u001a\u00020\tH\u00c6\u0003JE\u0010\u001b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\u00052\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\tH\u00c6\u0001J\u0013\u0010\u001c\u001a\u00020\u001d2\b\u0010\u001e\u001a\u0004\u0018\u00010\u001fH\u00d6\u0003J\t\u0010 \u001a\u00020\u0005H\u00d6\u0001J\t\u0010!\u001a\u00020\"H\u00d6\u0001R\u0011\u0010\u0007\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u0011\u0010\n\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0010R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\r\u00a8\u0006#"}, d2 = {"Lcom/pscholer/autoplayer/car/surface/VideoSurfaceRenderer$SurfaceState$Available;", "Lcom/pscholer/autoplayer/car/surface/VideoSurfaceRenderer$SurfaceState;", "surface", "Landroid/view/Surface;", "width", "", "height", "dpi", "visibleArea", "Landroid/graphics/Rect;", "stableArea", "(Landroid/view/Surface;IIILandroid/graphics/Rect;Landroid/graphics/Rect;)V", "getDpi", "()I", "getHeight", "getStableArea", "()Landroid/graphics/Rect;", "getSurface", "()Landroid/view/Surface;", "getVisibleArea", "getWidth", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "", "other", "", "hashCode", "toString", "", "app_debug"})
        public static final class Available extends com.pscholer.autoplayer.car.surface.VideoSurfaceRenderer.SurfaceState {
            @org.jetbrains.annotations.NotNull()
            private final android.view.Surface surface = null;
            private final int width = 0;
            private final int height = 0;
            private final int dpi = 0;
            @org.jetbrains.annotations.NotNull()
            private final android.graphics.Rect visibleArea = null;
            @org.jetbrains.annotations.NotNull()
            private final android.graphics.Rect stableArea = null;
            
            public Available(@org.jetbrains.annotations.NotNull()
            android.view.Surface surface, int width, int height, int dpi, @org.jetbrains.annotations.NotNull()
            android.graphics.Rect visibleArea, @org.jetbrains.annotations.NotNull()
            android.graphics.Rect stableArea) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final android.view.Surface getSurface() {
                return null;
            }
            
            public final int getWidth() {
                return 0;
            }
            
            public final int getHeight() {
                return 0;
            }
            
            public final int getDpi() {
                return 0;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final android.graphics.Rect getVisibleArea() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final android.graphics.Rect getStableArea() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final android.view.Surface component1() {
                return null;
            }
            
            public final int component2() {
                return 0;
            }
            
            public final int component3() {
                return 0;
            }
            
            public final int component4() {
                return 0;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final android.graphics.Rect component5() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final android.graphics.Rect component6() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pscholer.autoplayer.car.surface.VideoSurfaceRenderer.SurfaceState.Available copy(@org.jetbrains.annotations.NotNull()
            android.view.Surface surface, int width, int height, int dpi, @org.jetbrains.annotations.NotNull()
            android.graphics.Rect visibleArea, @org.jetbrains.annotations.NotNull()
            android.graphics.Rect stableArea) {
                return null;
            }
            
            @java.lang.Override()
            public boolean equals(@org.jetbrains.annotations.Nullable()
            java.lang.Object other) {
                return false;
            }
            
            @java.lang.Override()
            public int hashCode() {
                return 0;
            }
            
            @java.lang.Override()
            @org.jetbrains.annotations.NotNull()
            public java.lang.String toString() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/pscholer/autoplayer/car/surface/VideoSurfaceRenderer$SurfaceState$Unavailable;", "Lcom/pscholer/autoplayer/car/surface/VideoSurfaceRenderer$SurfaceState;", "()V", "app_debug"})
        public static final class Unavailable extends com.pscholer.autoplayer.car.surface.VideoSurfaceRenderer.SurfaceState {
            @org.jetbrains.annotations.NotNull()
            public static final com.pscholer.autoplayer.car.surface.VideoSurfaceRenderer.SurfaceState.Unavailable INSTANCE = null;
            
            private Unavailable() {
            }
        }
    }
}