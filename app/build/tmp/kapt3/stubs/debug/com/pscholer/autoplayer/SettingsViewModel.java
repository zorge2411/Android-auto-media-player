package com.pscholer.autoplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.pscholer.autoplayer.data.remote.jellyfin.CrossDomainRedirectException;
import com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository;
import com.pscholer.autoplayer.databinding.ActivitySettingsBinding;
import com.pscholer.autoplayer.databinding.ItemSafFolderBinding;
import com.pscholer.autoplayer.util.PreferencesManager;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000h\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0007\b\u0007\u0018\u00002\u00020\u0001:\u0002,-B\u0017\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u001e\u001a\u00020\u001f2\u0006\u0010 \u001a\u00020\u001cJ\u0006\u0010!\u001a\u00020\"J\u0006\u0010#\u001a\u00020\"J\u000e\u0010$\u001a\u00020\u001f2\u0006\u0010 \u001a\u00020\u001cJ\u001e\u0010%\u001a\u00020\"2\u0006\u0010&\u001a\u00020\'2\u0006\u0010(\u001a\u00020\'2\u0006\u0010)\u001a\u00020\'J\u0016\u0010*\u001a\u00020\u001f2\u0006\u0010&\u001a\u00020\'2\u0006\u0010+\u001a\u00020\'R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000e0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0017\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\t0\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0017\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00160\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0010R\u0017\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0012\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0014R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u001a\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001c0\u001b0\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0010\u00a8\u0006."}, d2 = {"Lcom/pscholer/autoplayer/SettingsViewModel;", "Landroidx/lifecycle/ViewModel;", "prefs", "Lcom/pscholer/autoplayer/util/PreferencesManager;", "jellyfin", "Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinRepository;", "(Lcom/pscholer/autoplayer/util/PreferencesManager;Lcom/pscholer/autoplayer/data/remote/jellyfin/JellyfinRepository;)V", "_jellyfinSaveState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState;", "_plexSaveState", "Lcom/pscholer/autoplayer/SettingsViewModel$PlexSaveState;", "jellyfinConfigFlow", "Lkotlinx/coroutines/flow/Flow;", "Lcom/pscholer/autoplayer/util/PreferencesManager$JellyfinConfig;", "getJellyfinConfigFlow", "()Lkotlinx/coroutines/flow/Flow;", "jellyfinSaveState", "Lkotlinx/coroutines/flow/StateFlow;", "getJellyfinSaveState", "()Lkotlinx/coroutines/flow/StateFlow;", "plexConfigFlow", "Lcom/pscholer/autoplayer/util/PreferencesManager$PlexConfig;", "getPlexConfigFlow", "plexSaveState", "getPlexSaveState", "safUrisFlow", "", "Landroid/net/Uri;", "getSafUrisFlow", "addSafUri", "Lkotlinx/coroutines/Job;", "uri", "consumeJellyfinTransient", "", "consumePlexSaved", "removeSafUri", "saveJellyfin", "serverUrl", "", "username", "password", "savePlex", "token", "JellyfinSaveState", "PlexSaveState", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class SettingsViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.util.PreferencesManager prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository jellyfin = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pscholer.autoplayer.SettingsViewModel.JellyfinSaveState> _jellyfinSaveState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.SettingsViewModel.JellyfinSaveState> jellyfinSaveState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pscholer.autoplayer.SettingsViewModel.PlexSaveState> _plexSaveState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.SettingsViewModel.PlexSaveState> plexSaveState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<com.pscholer.autoplayer.util.PreferencesManager.PlexConfig> plexConfigFlow = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<com.pscholer.autoplayer.util.PreferencesManager.JellyfinConfig> jellyfinConfigFlow = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.util.Set<android.net.Uri>> safUrisFlow = null;
    
    @javax.inject.Inject()
    public SettingsViewModel(@org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.util.PreferencesManager prefs, @org.jetbrains.annotations.NotNull()
    com.pscholer.autoplayer.data.remote.jellyfin.JellyfinRepository jellyfin) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.SettingsViewModel.JellyfinSaveState> getJellyfinSaveState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pscholer.autoplayer.SettingsViewModel.PlexSaveState> getPlexSaveState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<com.pscholer.autoplayer.util.PreferencesManager.PlexConfig> getPlexConfigFlow() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<com.pscholer.autoplayer.util.PreferencesManager.JellyfinConfig> getJellyfinConfigFlow() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.Set<android.net.Uri>> getSafUrisFlow() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.Job savePlex(@org.jetbrains.annotations.NotNull()
    java.lang.String serverUrl, @org.jetbrains.annotations.NotNull()
    java.lang.String token) {
        return null;
    }
    
    public final void consumePlexSaved() {
    }
    
    public final void saveJellyfin(@org.jetbrains.annotations.NotNull()
    java.lang.String serverUrl, @org.jetbrains.annotations.NotNull()
    java.lang.String username, @org.jetbrains.annotations.NotNull()
    java.lang.String password) {
    }
    
    public final void consumeJellyfinTransient() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.Job addSafUri(@org.jetbrains.annotations.NotNull()
    android.net.Uri uri) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.Job removeSafUri(@org.jetbrains.annotations.NotNull()
    android.net.Uri uri) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0004\u0003\u0004\u0005\u0006B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0004\u0007\b\t\n\u00a8\u0006\u000b"}, d2 = {"Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState;", "", "()V", "Checking", "Error", "Idle", "Success", "Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState$Checking;", "Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState$Error;", "Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState$Idle;", "Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState$Success;", "app_debug"})
    public static abstract class JellyfinSaveState {
        
        private JellyfinSaveState() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u00c6\n\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0013\u0010\u0003\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006H\u00d6\u0003J\t\u0010\u0007\u001a\u00020\bH\u00d6\u0001J\t\u0010\t\u001a\u00020\nH\u00d6\u0001\u00a8\u0006\u000b"}, d2 = {"Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState$Checking;", "Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState;", "()V", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
        public static final class Checking extends com.pscholer.autoplayer.SettingsViewModel.JellyfinSaveState {
            @org.jetbrains.annotations.NotNull()
            public static final com.pscholer.autoplayer.SettingsViewModel.JellyfinSaveState.Checking INSTANCE = null;
            
            private Checking() {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState$Error;", "Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState;", "message", "", "(Ljava/lang/String;)V", "getMessage", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Error extends com.pscholer.autoplayer.SettingsViewModel.JellyfinSaveState {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String message = null;
            
            public Error(@org.jetbrains.annotations.NotNull()
            java.lang.String message) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getMessage() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pscholer.autoplayer.SettingsViewModel.JellyfinSaveState.Error copy(@org.jetbrains.annotations.NotNull()
            java.lang.String message) {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u00c6\n\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0013\u0010\u0003\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006H\u00d6\u0003J\t\u0010\u0007\u001a\u00020\bH\u00d6\u0001J\t\u0010\t\u001a\u00020\nH\u00d6\u0001\u00a8\u0006\u000b"}, d2 = {"Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState$Idle;", "Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState;", "()V", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
        public static final class Idle extends com.pscholer.autoplayer.SettingsViewModel.JellyfinSaveState {
            @org.jetbrains.annotations.NotNull()
            public static final com.pscholer.autoplayer.SettingsViewModel.JellyfinSaveState.Idle INSTANCE = null;
            
            private Idle() {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0010"}, d2 = {"Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState$Success;", "Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState;", "displayName", "", "(Ljava/lang/String;)V", "getDisplayName", "()Ljava/lang/String;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "app_debug"})
        public static final class Success extends com.pscholer.autoplayer.SettingsViewModel.JellyfinSaveState {
            @org.jetbrains.annotations.NotNull()
            private final java.lang.String displayName = null;
            
            public Success(@org.jetbrains.annotations.NotNull()
            java.lang.String displayName) {
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String getDisplayName() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final java.lang.String component1() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pscholer.autoplayer.SettingsViewModel.JellyfinSaveState.Success copy(@org.jetbrains.annotations.NotNull()
            java.lang.String displayName) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0002\u0003\u0004B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0002\u0005\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/pscholer/autoplayer/SettingsViewModel$PlexSaveState;", "", "()V", "Idle", "Saved", "Lcom/pscholer/autoplayer/SettingsViewModel$PlexSaveState$Idle;", "Lcom/pscholer/autoplayer/SettingsViewModel$PlexSaveState$Saved;", "app_debug"})
    public static abstract class PlexSaveState {
        
        private PlexSaveState() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u00c6\n\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0013\u0010\u0003\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006H\u00d6\u0003J\t\u0010\u0007\u001a\u00020\bH\u00d6\u0001J\t\u0010\t\u001a\u00020\nH\u00d6\u0001\u00a8\u0006\u000b"}, d2 = {"Lcom/pscholer/autoplayer/SettingsViewModel$PlexSaveState$Idle;", "Lcom/pscholer/autoplayer/SettingsViewModel$PlexSaveState;", "()V", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
        public static final class Idle extends com.pscholer.autoplayer.SettingsViewModel.PlexSaveState {
            @org.jetbrains.annotations.NotNull()
            public static final com.pscholer.autoplayer.SettingsViewModel.PlexSaveState.Idle INSTANCE = null;
            
            private Idle() {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u00c6\n\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0013\u0010\u0003\u001a\u00020\u00042\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006H\u00d6\u0003J\t\u0010\u0007\u001a\u00020\bH\u00d6\u0001J\t\u0010\t\u001a\u00020\nH\u00d6\u0001\u00a8\u0006\u000b"}, d2 = {"Lcom/pscholer/autoplayer/SettingsViewModel$PlexSaveState$Saved;", "Lcom/pscholer/autoplayer/SettingsViewModel$PlexSaveState;", "()V", "equals", "", "other", "", "hashCode", "", "toString", "", "app_debug"})
        public static final class Saved extends com.pscholer.autoplayer.SettingsViewModel.PlexSaveState {
            @org.jetbrains.annotations.NotNull()
            public static final com.pscholer.autoplayer.SettingsViewModel.PlexSaveState.Saved INSTANCE = null;
            
            private Saved() {
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