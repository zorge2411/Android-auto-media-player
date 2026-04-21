package com.pscholer.autoplayer.car.screens;

import android.content.Intent;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.Template;
import androidx.core.graphics.drawable.IconCompat;
import com.pscholer.autoplayer.R;
import com.pscholer.autoplayer.SettingsActivity;
import com.pscholer.autoplayer.data.MediaSource;

/**
 * First screen shown when Android Auto connects.
 * Presents three source tiles: Local Files, Plex, Jellyfin.
 *
 * GridTemplate supports up to 6 items while driving (AA driving restriction).
 * Thumbnails / icons are loaded from local drawables; no async needed here.
 *
 * The header ActionStrip includes a settings button that opens SettingsActivity
 * on the phone screen via carContext.startActivity(). FLAG_ACTIVITY_NEW_TASK is
 * required because the intent is launched from a non-Activity context. No extras
 * are passed — the Car App host rejects explicit-component intents that carry
 * extras unless the target is exported with a matching intent-filter action.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\u0005\u001a\u00020\u0006H\u0016\u00a8\u0006\u0007"}, d2 = {"Lcom/pscholer/autoplayer/car/screens/RootScreen;", "Landroidx/car/app/Screen;", "carContext", "Landroidx/car/app/CarContext;", "(Landroidx/car/app/CarContext;)V", "onGetTemplate", "Landroidx/car/app/model/Template;", "app_debug"})
public final class RootScreen extends androidx.car.app.Screen {
    
    public RootScreen(@org.jetbrains.annotations.NotNull()
    androidx.car.app.CarContext carContext) {
        super(null);
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public androidx.car.app.model.Template onGetTemplate() {
        return null;
    }
}