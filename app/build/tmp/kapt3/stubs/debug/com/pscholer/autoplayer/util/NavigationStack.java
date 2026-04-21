package com.pscholer.autoplayer.util;

import com.pscholer.autoplayer.data.models.MediaItem;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0007\n\u0002\u0010\b\n\u0002\b\u0002\u0018\u00002\u00020\u0001:\u0001\u0012B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\u0006\u001a\u00020\u0007J\u0006\u0010\b\u001a\u00020\tJ\b\u0010\n\u001a\u0004\u0018\u00010\u0005J\b\u0010\u000b\u001a\u0004\u0018\u00010\u0005J\b\u0010\f\u001a\u0004\u0018\u00010\u0005J\b\u0010\r\u001a\u0004\u0018\u00010\u0005J\u000e\u0010\u000e\u001a\u00020\u00072\u0006\u0010\u000f\u001a\u00020\u0005J\u0006\u0010\u0010\u001a\u00020\u0011R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/pscholer/autoplayer/util/NavigationStack;", "", "()V", "stack", "", "Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget;", "clear", "", "isEmpty", "", "peek", "peekPrevious", "pop", "popToRoot", "push", "target", "size", "", "NavigationTarget", "app_debug"})
public final class NavigationStack {
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.pscholer.autoplayer.util.NavigationStack.NavigationTarget> stack = null;
    
    public NavigationStack() {
        super();
    }
    
    public final void push(@org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.util.NavigationStack.NavigationTarget target) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.pscholer.autoplayer.util.NavigationStack.NavigationTarget pop() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.pscholer.autoplayer.util.NavigationStack.NavigationTarget peek() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.pscholer.autoplayer.util.NavigationStack.NavigationTarget peekPrevious() {
        return null;
    }
    
    public final void clear() {
    }
    
    public final int size() {
        return 0;
    }
    
    public final boolean isEmpty() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.pscholer.autoplayer.util.NavigationStack.NavigationTarget popToRoot() {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0005\u0003\u0004\u0005\u0006\u0007B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0005\b\t\n\u000b\f\u00a8\u0006\r"}, d2 = {"Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget;", "", "()V", "Browse", "Favorites", "Playlist", "Root", "Video", "Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget$Browse;", "Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget$Favorites;", "Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget$Playlist;", "Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget$Root;", "Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget$Video;", "app_debug"})
    public static abstract class NavigationTarget {
        
        private NavigationTarget() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\t\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0003\b\u0086\b\u0018\u00002\u00020\u0001B\u0019\u0012\b\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000b\u0010\u000b\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010\f\u001a\u00020\u0005H\u00c6\u0003J\u001f\u0010\r\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0011H\u00d6\u0003J\t\u0010\u0012\u001a\u00020\u0005H\u00d6\u0001J\t\u0010\u0013\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0014"}, d2 = {"Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget$Browse;", "Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget;", "parentId", "", "scrollPosition", "", "(Ljava/lang/String;I)V", "getParentId", "()Ljava/lang/String;", "getScrollPosition", "()I", "component1", "component2", "copy", "equals", "", "other", "", "hashCode", "toString", "app_debug"})
        public static final class Browse extends com.pscholer.autoplayer.util.NavigationStack.NavigationTarget {
            @org.jetbrains.annotations.Nullable()
            private final java.lang.String parentId = null;
            private final int scrollPosition = 0;
            
            public Browse(@org.jetbrains.annotations.Nullable()
            java.lang.String parentId, int scrollPosition) {
            }
            
            @org.jetbrains.annotations.Nullable()
            public final java.lang.String getParentId() {
                return null;
            }
            
            public final int getScrollPosition() {
                return 0;
            }
            
            @org.jetbrains.annotations.Nullable()
            public final java.lang.String component1() {
                return null;
            }
            
            public final int component2() {
                return 0;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pscholer.autoplayer.util.NavigationStack.NavigationTarget.Browse copy(@org.jetbrains.annotations.Nullable()
            java.lang.String parentId, int scrollPosition) {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget$Favorites;", "Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget;", "()V", "app_debug"})
        public static final class Favorites extends com.pscholer.autoplayer.util.NavigationStack.NavigationTarget {
            @org.jetbrains.annotations.NotNull()
            public static final com.pscholer.autoplayer.util.NavigationStack.NavigationTarget.Favorites INSTANCE = null;
            
            private Favorites() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget$Playlist;", "Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget;", "playlistId", "", "(Ljava/lang/String;)V", "getPlaylistId", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Playlist extends com.pscholer.autoplayer.util.NavigationStack.NavigationTarget {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String playlistId = null;
            
            public Playlist(@org.jetbrains.annotations.NotNull()
            java.lang.String playlistId) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getPlaylistId() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pscholer.autoplayer.util.NavigationStack.NavigationTarget.Playlist copy(@org.jetbrains.annotations.NotNull()
            java.lang.String playlistId) {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget$Root;", "Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget;", "()V", "app_debug"})
        public static final class Root extends com.pscholer.autoplayer.util.NavigationStack.NavigationTarget {
            @org.jetbrains.annotations.NotNull()
            public static final com.pscholer.autoplayer.util.NavigationStack.NavigationTarget.Root INSTANCE = null;
            
            private Root() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget$Video;", "Lcom/pscholer/autoplayer/util/NavigationStack$NavigationTarget;", "mediaItem", "Lcom/pscholer/autoplayer/data/models/MediaItem;", "(Lcom/pscholer/autoplayer/data/models/MediaItem;)V", "getMediaItem", "()Lcom/pscholer/autoplayer/data/models/MediaItem;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
        public static final class Video extends com.pscholer.autoplayer.util.NavigationStack.NavigationTarget {
            @org.jetbrains.annotations.NotNull()
            private final com.pscholer.autoplayer.data.models.MediaItem mediaItem = null;
            
            public Video(@org.jetbrains.annotations.NotNull()
            com.pscholer.autoplayer.data.models.MediaItem mediaItem) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pscholer.autoplayer.data.models.MediaItem getMediaItem() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pscholer.autoplayer.data.models.MediaItem component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pscholer.autoplayer.util.NavigationStack.NavigationTarget.Video copy(@org.jetbrains.annotations.NotNull()
            com.pscholer.autoplayer.data.models.MediaItem mediaItem) {
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
    }
}