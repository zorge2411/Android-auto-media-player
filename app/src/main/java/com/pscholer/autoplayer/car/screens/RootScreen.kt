package com.pscholer.autoplayer.car.screens

import android.content.Intent
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.pscholer.autoplayer.R
import com.pscholer.autoplayer.SettingsActivity
import com.pscholer.autoplayer.data.MediaSource

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
class RootScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val sources = listOf(
            Triple(MediaSource.LOCAL,    R.drawable.ic_folder,   "Phone storage"),
            Triple(MediaSource.PLEX,     R.drawable.ic_plex,     "Plex media server"),
            Triple(MediaSource.JELLYFIN, R.drawable.ic_jellyfin, "Jellyfin media server"),
        )

        val items = ItemList.Builder().apply {
            sources.forEach { (source, iconRes, subtitle) ->
                addItem(
                    GridItem.Builder()
                        .setTitle(source.displayName)
                        .setText(subtitle)
                        .setImage(
                            CarIcon.Builder(
                                IconCompat.createWithResource(carContext, iconRes)
                            ).build(),
                            GridItem.IMAGE_TYPE_ICON
                        )
                        .setOnClickListener {
                            screenManager.push(BrowseScreen(carContext, source, parentId = null))
                        }
                        .build()
                )
            }
        }.build()

        // Settings action opens SettingsActivity on the phone.
        // carContext.startActivity() is the correct API for launching phone-side
        // Activities from a Car App screen. FLAG_ACTIVITY_NEW_TASK is mandatory
        // (CarContext is not an Activity context). No extras are attached — the
        // Car App host's component resolver rejects explicit-component intents
        // that carry extras when the target activity has no matching intent-filter
        // action beyond MAIN/LAUNCHER.
        val settingsAction = Action.Builder()
            .setIcon(
                CarIcon.Builder(
                    IconCompat.createWithResource(carContext, R.drawable.ic_settings)
                ).build()
            )
            .setOnClickListener {
                val intent = Intent(carContext, SettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                carContext.startActivity(intent)
            }
            .build()

        return GridTemplate.Builder()
            .setTitle("Auto Player")
            .setHeaderAction(Action.APP_ICON)
            .setActionStrip(ActionStrip.Builder().addAction(settingsAction).build())
            .setSingleList(items)
            .build()
    }
}
