# Android Auto Media Player

A sideloaded Android Auto app that plays local video files directly on your car's head unit display.

## What it does

- Browses and plays local video files from your phone
- Renders video on the Android Auto SurfaceContainer (head unit screen)
- Step-seek controls (±10 seconds) via ActionStrip buttons
- Auto-hiding playback controls with tap-to-reveal
- Timeline display (HH:MM:SS / HH:MM:SS) during playback
- Favorites support
- Smart back-button navigation

## How it works

The app registers as a `NAVIGATION` category app (not `MEDIA`) to gain access to `SurfaceContainer` for direct video rendering — the only way to display video frames on the head unit screen via Car App Library.

## Tech stack

- Kotlin + Car App Library 1.7.0
- ExoPlayer / Media3 1.3.1
- Hilt dependency injection
- Material 3 (teal seed)

## Requirements

- Android phone running Android 6.0+
- Android Auto (sideloaded or installed)
- Head unit with Android Auto support

## Setup

1. Clone the repo
2. Open in Android Studio
3. Build and install on your phone (`./gradlew installDebug`)
4. Connect phone to head unit

## Note

This app uses a NAVIGATION category workaround to access the video surface. It is intended for personal/experimental use.
