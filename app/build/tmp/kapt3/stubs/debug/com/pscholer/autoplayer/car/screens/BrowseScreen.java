package com.pscholer.autoplayer.car.screens;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.content.pm.PackageManager;
import androidx.annotation.OptIn;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;
import coil.request.ImageRequest;
import coil.request.SuccessResult;
import java.util.concurrent.ConcurrentHashMap;
import com.pscholer.autoplayer.R;
import com.pscholer.autoplayer.data.MediaSource;
import com.pscholer.autoplayer.data.MediaRepository;
import com.pscholer.autoplayer.data.models.MediaItem;
import com.pscholer.autoplayer.di.AppEntryPoint;
import dagger.hilt.android.EntryPointAccessors;
import kotlinx.coroutines.Dispatchers;

/**
 * Browsable grid screen for navigating library hierarchies.
 *
 * Flow:
 * RootScreen → BrowseScreen(parentId=null) [libraries/root folders]
 *            → BrowseScreen(parentId=X)    [sub-folders / seasons]
 *            → VideoPlaybackScreen          [leaf video item]
 *
 * Thumbnail loading:
 * We pre-fetch all thumbnails with Coil on a background thread, then call
 * invalidate() to re-render the template with actual artwork. Android Auto
 * does NOT support async image loading inside template builders — you must
 * have the Bitmap ready before building the GridItem.
 *
 * Size is capped at 256x256 px to stay well under the ~1 MB Binder IPC limit
 * (a 512x512 ARGB_8888 bitmap is exactly 1 MB and causes "Large outgoing
 * transaction" warnings or crashes on some head units).
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u00a2\u0006\u0002\u0010\bJ\u0010\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u000eH\u0002J\b\u0010\u001c\u001a\u00020\u001dH\u0002J\b\u0010\u001e\u001a\u00020\u001dH\u0002J\b\u0010\u001f\u001a\u00020 H\u0017J\u0016\u0010!\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u001d0\rH\u0082@\u00a2\u0006\u0002\u0010\"R\u0010\u0010\t\u001a\u0004\u0018\u00010\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0010\u001a\u00020\u00118BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0014\u0010\u0015\u001a\u0004\b\u0012\u0010\u0013R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0016\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00180\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006#"}, d2 = {"Lcom/pscholer/autoplayer/car/screens/BrowseScreen;", "Landroidx/car/app/Screen;", "carContext", "Landroidx/car/app/CarContext;", "source", "Lcom/pscholer/autoplayer/data/MediaSource;", "parentId", "", "(Landroidx/car/app/CarContext;Lcom/pscholer/autoplayer/data/MediaSource;Ljava/lang/String;)V", "errorMessage", "isLoading", "", "items", "", "Lcom/pscholer/autoplayer/data/models/MediaItem;", "needsPermission", "repo", "Lcom/pscholer/autoplayer/data/MediaRepository;", "getRepo", "()Lcom/pscholer/autoplayer/data/MediaRepository;", "repo$delegate", "Lkotlin/Lazy;", "thumbnails", "Ljava/util/concurrent/ConcurrentHashMap;", "Landroid/graphics/Bitmap;", "buildGridItem", "Landroidx/car/app/model/GridItem;", "item", "checkPermissionsAndLoad", "", "loadMedia", "onGetTemplate", "Landroidx/car/app/model/Template;", "prefetchThumbnails", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class BrowseScreen extends androidx.car.app.Screen {
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.data.MediaSource source = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String parentId = null;
    @org.jetbrains.annotations.NotNull()
    private java.util.List<com.pscholer.autoplayer.data.models.MediaItem> items;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.ConcurrentHashMap<java.lang.String, android.graphics.Bitmap> thumbnails = null;
    private boolean isLoading = true;
    private boolean needsPermission = false;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String errorMessage;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy repo$delegate = null;
    
    public BrowseScreen(@org.jetbrains.annotations.NotNull()
    androidx.car.app.CarContext carContext, @org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.data.MediaSource source, @org.jetbrains.annotations.Nullable()
    java.lang.String parentId) {
        super(null);
    }
    
    private final com.pscholer.autoplayer.data.MediaRepository getRepo() {
        return null;
    }
    
    private final void checkPermissionsAndLoad() {
    }
    
    private final void loadMedia() {
    }
    
    @java.lang.Override()
    @androidx.annotation.OptIn(markerClass = {androidx.car.app.annotations.ExperimentalCarApi.class})
    @org.jetbrains.annotations.NotNull()
    public androidx.car.app.model.Template onGetTemplate() {
        return null;
    }
    
    private final androidx.car.app.model.GridItem buildGridItem(com.pscholer.autoplayer.data.models.MediaItem item) {
        return null;
    }
    
    private final java.lang.Object prefetchThumbnails(kotlin.coroutines.Continuation<? super java.util.List<kotlin.Unit>> $completion) {
        return null;
    }
}