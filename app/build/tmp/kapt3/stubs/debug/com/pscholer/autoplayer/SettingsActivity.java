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

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0082\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\"\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0016H\u0002J\u0010\u0010\u0017\u001a\u00020\u00142\u0006\u0010\u0018\u001a\u00020\u0019H\u0002J\b\u0010\u001a\u001a\u00020\u0014H\u0002J\u0012\u0010\u001b\u001a\u00020\u00142\b\u0010\u001c\u001a\u0004\u0018\u00010\u001dH\u0014J\b\u0010\u001e\u001a\u00020\u0014H\u0002J\b\u0010\u001f\u001a\u00020\u0014H\u0002J\u0016\u0010 \u001a\u00020\u00142\f\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00070\"H\u0002J\u0010\u0010#\u001a\u00020\u00142\u0006\u0010$\u001a\u00020%H\u0002J\u0010\u0010&\u001a\u00020\u00142\u0006\u0010\'\u001a\u00020(H\u0002J\u0010\u0010)\u001a\u00020\u00142\u0006\u0010$\u001a\u00020*H\u0002J\u0010\u0010+\u001a\u00020\u00142\u0006\u0010\'\u001a\u00020,H\u0002J\u0010\u0010-\u001a\u00020\u00142\u0006\u0010.\u001a\u00020\u000bH\u0002J\u001d\u0010/\u001a\u00020\u0014*\u0002002\u000e\b\u0004\u00101\u001a\b\u0012\u0004\u0012\u00020\u001402H\u0082\bJ\f\u00103\u001a\u00020\u000b*\u000200H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0005\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\r\u001a\u00020\u000e8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0011\u0010\u0012\u001a\u0004\b\u000f\u0010\u0010\u00a8\u00064"}, d2 = {"Lcom/pscholer/autoplayer/SettingsActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "binding", "Lcom/pscholer/autoplayer/databinding/ActivitySettingsBinding;", "dirPicker", "Landroidx/activity/result/ActivityResultLauncher;", "Landroid/net/Uri;", "folderAdapter", "Lcom/pscholer/autoplayer/SafFolderAdapter;", "jellyfinDisplayName", "", "requestPermissionLauncher", "vm", "Lcom/pscholer/autoplayer/SettingsViewModel;", "getVm", "()Lcom/pscholer/autoplayer/SettingsViewModel;", "vm$delegate", "Lkotlin/Lazy;", "confirmRemoveFolder", "", "row", "Lcom/pscholer/autoplayer/SafFolderRow;", "maybeRequestPermission", "isFreshLaunch", "", "observeState", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onSaveJellyfinClicked", "onSavePlexClicked", "renderFolders", "uris", "", "renderJellyfin", "cfg", "Lcom/pscholer/autoplayer/util/PreferencesManager$JellyfinConfig;", "renderJellyfinSaveState", "state", "Lcom/pscholer/autoplayer/SettingsViewModel$JellyfinSaveState;", "renderPlex", "Lcom/pscholer/autoplayer/util/PreferencesManager$PlexConfig;", "renderPlexSaveState", "Lcom/pscholer/autoplayer/SettingsViewModel$PlexSaveState;", "showRationaleDialog", "permission", "doOnInput", "Lcom/google/android/material/textfield/TextInputEditText;", "action", "Lkotlin/Function0;", "text", "app_debug"})
public final class SettingsActivity extends androidx.appcompat.app.AppCompatActivity {
    private com.pscholer.autoplayer.databinding.ActivitySettingsBinding binding;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy vm$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.SafFolderAdapter folderAdapter = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> requestPermissionLauncher = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<android.net.Uri> dirPicker = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String jellyfinDisplayName;
    
    public SettingsActivity() {
        super();
    }
    
    private final com.pscholer.autoplayer.SettingsViewModel getVm() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void observeState() {
    }
    
    private final void renderPlex(com.pscholer.autoplayer.util.PreferencesManager.PlexConfig cfg) {
    }
    
    private final void renderJellyfin(com.pscholer.autoplayer.util.PreferencesManager.JellyfinConfig cfg) {
    }
    
    private final void renderFolders(java.util.Set<? extends android.net.Uri> uris) {
    }
    
    private final void renderPlexSaveState(com.pscholer.autoplayer.SettingsViewModel.PlexSaveState state) {
    }
    
    private final void renderJellyfinSaveState(com.pscholer.autoplayer.SettingsViewModel.JellyfinSaveState state) {
    }
    
    private final void onSavePlexClicked() {
    }
    
    private final void onSaveJellyfinClicked() {
    }
    
    private final void confirmRemoveFolder(com.pscholer.autoplayer.SafFolderRow row) {
    }
    
    private final void maybeRequestPermission(boolean isFreshLaunch) {
    }
    
    private final void showRationaleDialog(java.lang.String permission) {
    }
    
    private final java.lang.String text(com.google.android.material.textfield.TextInputEditText $this$text) {
        return null;
    }
    
    private final void doOnInput(com.google.android.material.textfield.TextInputEditText $this$doOnInput, kotlin.jvm.functions.Function0<kotlin.Unit> action) {
    }
}