package com.pscholer.autoplayer.car.screens;

import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.Header;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import com.pscholer.autoplayer.data.FavoriteRepository;
import com.pscholer.autoplayer.data.models.Favorite;
import androidx.car.app.CarToast;
import com.pscholer.autoplayer.data.MediaSource;
import com.pscholer.autoplayer.data.MediaRepository;
import com.pscholer.autoplayer.di.AppEntryPoint;
import dagger.hilt.android.EntryPointAccessors;

/**
 * Browse all favorite videos across LOCAL/PLEX/JELLYFIN sources.
 * Tapping a video plays it. Heart icon toggles favorite status.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\u0012\u001a\u00020\u0013H\u0016R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\f\u001a\u00020\r8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0010\u0010\u0011\u001a\u0004\b\u000e\u0010\u000f\u00a8\u0006\u0014"}, d2 = {"Lcom/pscholer/autoplayer/car/screens/FavoritesScreen;", "Landroidx/car/app/Screen;", "carContext", "Landroidx/car/app/CarContext;", "(Landroidx/car/app/CarContext;)V", "favoriteRepository", "Lcom/pscholer/autoplayer/data/FavoriteRepository;", "favorites", "", "Lcom/pscholer/autoplayer/data/models/Favorite;", "isLoading", "", "repo", "Lcom/pscholer/autoplayer/data/MediaRepository;", "getRepo", "()Lcom/pscholer/autoplayer/data/MediaRepository;", "repo$delegate", "Lkotlin/Lazy;", "onGetTemplate", "Landroidx/car/app/model/Template;", "app_debug"})
public final class FavoritesScreen extends androidx.car.app.Screen {
    @org.jetbrains.annotations.NotNull()
    private final com.pscholer.autoplayer.data.FavoriteRepository favoriteRepository = null;
    @org.jetbrains.annotations.NotNull()
    private java.util.List<com.pscholer.autoplayer.data.models.Favorite> favorites;
    private boolean isLoading = true;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy repo$delegate = null;
    
    public FavoritesScreen(@org.jetbrains.annotations.NotNull()
    androidx.car.app.CarContext carContext) {
        super(null);
    }
    
    private final com.pscholer.autoplayer.data.MediaRepository getRepo() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public androidx.car.app.model.Template onGetTemplate() {
        return null;
    }
}