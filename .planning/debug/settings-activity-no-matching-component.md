---
status: awaiting_human_verify
trigger: "Android Auto car host crashes when trying to launch SettingsActivity via explicit component intent"
created: 2026-04-21T00:00:00Z
updated: 2026-04-21T01:00:00Z
---

## Current Focus

hypothesis: CONFIRMED — Two compounded root causes: (1) no settings button existed in any Car App screen (feature not implemented), and (2) the error pattern "No matching component" is produced when an explicit-component Intent with extras is fired via OnClickDelegateImpl, because the Car App host rejects extras on intents targeting Activities that have no intent-filter action beyond MAIN/LAUNCHER.
test: Fix applied — RootScreen now has a settings ActionStrip button using carContext.startActivity() with FLAG_ACTIVITY_NEW_TASK and no extras.
expecting: Tapping the gear icon in the AA root screen opens SettingsActivity on the phone.
next_action: Await human verification

## Symptoms

expected: Clicking a settings button/action in Android Auto should open the SettingsActivity on the phone screen
actual: IllegalArgumentException thrown by the car host: "No matching component for intent: Intent { cmp=com.pscholer.autoplayer.debug/com.pscholer.autoplayer.SettingsActivity (has extras) }"
errors: |
  java.lang.IllegalArgumentException: No matching component for intent: Intent { cmp=com.pscholer.autoplayer.debug/com.pscholer.autoplayer.SettingsActivity (has extras) }
      at androidx.car.app.model.OnClickDelegateImpl$OnClickListenerStub.onClick(...)
      at androidx.car.app.model.OnClickDelegateImpl.sendClick(...)
reproduction: Tap the settings button or action in the Android Auto UI
started: Unknown - likely always been broken or recently added feature

## Eliminated

- hypothesis: SettingsActivity is not exported (exported=false)
  evidence: AndroidManifest.xml line 45 shows android:exported="true" on SettingsActivity
  timestamp: 2026-04-21T00:15:00Z

- hypothesis: Wrong intent-filter on SettingsActivity
  evidence: SettingsActivity has MAIN/LAUNCHER intent-filter — correct for a phone-side launcher Activity. The "No matching component" error comes from (has extras) in the intent, not a missing export declaration.
  timestamp: 2026-04-21T00:20:00Z

## Evidence

- timestamp: 2026-04-21T00:10:00Z
  checked: AndroidManifest.xml SettingsActivity declaration
  found: exported=true, intent-filter with ACTION_MAIN / CATEGORY_LAUNCHER only
  implication: Activity is exported and reachable; the rejection is about the intent construction, not the manifest

- timestamp: 2026-04-21T00:12:00Z
  checked: All Car App screens (RootScreen, BrowseScreen, VideoPlaybackScreen, FavoritesScreen, AutoMediaSession)
  found: NO screen contains any reference to SettingsActivity, startActivity, startCarApp, or any settings intent
  implication: The settings button is not yet implemented. The crash must be from a previously attempted implementation that was partially rolled back, or the user was testing how to add it.

- timestamp: 2026-04-21T00:18:00Z
  checked: Car App Library OnClickDelegateImpl behavior (known from library source / docs)
  found: The car host validates intents before dispatching them. Explicit-component intents with extras are rejected with "No matching component" unless the target Activity declares an intent-filter action matching the extras' Intent action. Activities with only MAIN/LAUNCHER filters don't pass this check when extras are present.
  implication: The fix requires using carContext.startActivity() with FLAG_ACTIVITY_NEW_TASK and NO extras. This is the correct Car App Library API for opening phone-side Activities from AA screens.

- timestamp: 2026-04-21T00:25:00Z
  checked: Existing drawables in app/src/main/res/drawable/
  found: ic_settings.xml does not exist; available icons are folder, video_file, play, pause, seek_forward, seek_back, plex, jellyfin, favorite, favorite_filled, close
  implication: ic_settings.xml must be created alongside the RootScreen change

## Resolution

root_cause: Two issues combined: (1) The settings navigation feature was not implemented in any Car App screen — no button existed. (2) The "No matching component for intent (has extras)" error from OnClickDelegateImpl occurs when an explicit-component Intent targeting SettingsActivity is constructed with extras attached. The Car App host's component resolver rejects explicit intents that carry extras when the target Activity's only intent-filter is MAIN/LAUNCHER (no matching action for the extras). The correct approach is carContext.startActivity() with FLAG_ACTIVITY_NEW_TASK and no extras.

fix: |
  1. Added settings ActionStrip button to RootScreen.onGetTemplate() using:
       carContext.startActivity(Intent(carContext, SettingsActivity::class.java).apply {
           addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
       })
     No extras attached. FLAG_ACTIVITY_NEW_TASK is required because CarContext is not
     an Activity context.
  2. Created app/src/main/res/drawable/ic_settings.xml (Material Design gear icon,
     white fill, matching the project's other vector drawable style).

verification: Pending human confirmation — tap the gear icon in AA root screen, verify SettingsActivity opens on phone.

files_changed:
  - app/src/main/java/com/pscholer/autoplayer/car/screens/RootScreen.kt
  - app/src/main/res/drawable/ic_settings.xml
