---
created: 2026-04-27T18:15:32Z
title: Switch stop button icon from pause to relevant icon
area: ui
files:
  - app/src/main/java/com/pscholer/autoplayer/car/screens/VideoPlaybackScreen.kt
  - app/src/main/res/drawable/ic_pause.xml (or equivalent pause drawable resource)
---

## Problem

VideoPlaybackScreen's stop button (mapStrip) currently uses `R.drawable.ic_pause` as the icon. The pause icon is semantically confusing for a button that both stops playback AND pops back to the browse context. Users may expect a stop icon (square) or an exit/back icon instead of pause (two vertical bars).

## Solution

Investigate available drawables in the project:
- `ic_stop` or similar stop icon (square shape)
- `ic_exit` or `ic_back` or `ic_close` for the exit semantic
- Consider project's Material Design icon set and consistency with other exit buttons

Update VideoPlaybackScreen.kt line ~202 to use the new drawable. Keep the button behavior unchanged (playerManager.stop() + screenManager.pop()).
