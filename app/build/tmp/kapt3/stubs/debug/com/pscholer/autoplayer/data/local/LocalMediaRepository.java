package com.pscholer.autoplayer.data.local;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import androidx.documentfile.provider.DocumentFile;
import com.pscholer.autoplayer.data.models.MediaItem;
import com.pscholer.autoplayer.util.PreferencesManager;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.Dispatchers;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\b\u0002\b\u0007\u0018\u0000 \u00162\u00020\u0001:\u0001\u0016B\u0019\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bJ\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\t0\bH\u0086@\u00a2\u0006\u0002\u0010\u000bJ\u001c\u0010\f\u001a\b\u0012\u0004\u0012\u00020\t0\b2\u0006\u0010\r\u001a\u00020\u000eH\u0086@\u00a2\u0006\u0002\u0010\u000fJ\u001e\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\t0\u0015H\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0017"}, d2 = {"Lcom/pscholer/autoplayer/data/local/LocalMediaRepository;", "", "context", "Landroid/content/Context;", "prefs", "Lcom/pscholer/autoplayer/util/PreferencesManager;", "(Landroid/content/Context;Lcom/pscholer/autoplayer/util/PreferencesManager;)V", "getSafFolders", "", "Lcom/pscholer/autoplayer/data/models/MediaItem;", "scanMediaStore", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "scanSafDirectory", "treeUri", "Landroid/net/Uri;", "(Landroid/net/Uri;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "walkDocumentTree", "", "dir", "Landroidx/documentfile/provider/DocumentFile;", "sink", "", "Companion", "app_debug"})
public final class LocalMediaRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.util.PreferencesManager prefs = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "LocalMediaRepo";
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Set<java.lang.String> SUPPORTED_MIME = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.pscholer.autoplayer.data.local.LocalMediaRepository.Companion Companion = null;
    
    @javax.inject.Inject()
    public LocalMediaRepository(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.util.PreferencesManager prefs) {
        super();
    }
    
    /**
     * Scan device video library using MediaStore.
     *
     * Covers: /Movies, /DCIM, /Downloads and any other folder Android
     * has indexed. On API 29+ uses the scoped VOLUME_EXTERNAL collection.
     * Requires READ_MEDIA_VIDEO (API 33+) or READ_EXTERNAL_STORAGE (pre-33).
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object scanMediaStore(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pscholer.autoplayer.data.models.MediaItem>> $completion) {
        return null;
    }
    
    /**
     * Get list of user-added SAF folders as folder items.
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pscholer.autoplayer.data.models.MediaItem> getSafFolders() {
        return null;
    }
    
    /**
     * Scan a Storage Access Framework (SAF) tree URI.
     *
     * Used for:
     * • External USB drives connected to the phone (OTG)
     * • Custom folders the user explicitly grants access to
     * • Directories outside MediaStore's scan scope
     *
     * The user grants access via Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
     * and the returned URI is persisted with takePersistableUriPermission().
     * Pass that URI here.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object scanSafDirectory(@org.jetbrains.annotations.NotNull()
    android.net.Uri treeUri, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pscholer.autoplayer.data.models.MediaItem>> $completion) {
        return null;
    }
    
    private final void walkDocumentTree(androidx.documentfile.provider.DocumentFile dir, java.util.List<com.pscholer.autoplayer.data.models.MediaItem> sink) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\"\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/pscholer/autoplayer/data/local/LocalMediaRepository$Companion;", "", "()V", "SUPPORTED_MIME", "", "", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}