# Phase 2: Stop Other Audio Sources - Research

**Researched:** 2026-04-17  
**Domain:** Android Audio Focus Management, Media3/ExoPlayer Integration  
**Confidence:** HIGH

## Summary

This phase requires implementing audio focus to pause competing audio apps when playback starts. The core finding is that **Media3/ExoPlayer 1.3.1 with handleAudioFocus=true already manages audio focus automatically** — no additional AudioManager calls are needed. The implementation is already partially configured in MediaPlayerManager.kt with correct AudioAttributes (USAGE_MEDIA + AUDIO_CONTENT_TYPE_MOVIE) and handleAudioFocus=true on line 73.

The key discovery is understanding what NOT to do: adding manual requestAudioFocus() calls would create a duplicate request conflict. Instead, focus on verifying the foreground service properly maintains focus while backgrounded and confirming automotive audio routing behavior (car head units have independent focus management).

**Primary recommendation:** Verify existing configuration is correct (it is), add automotive-specific testing for focus retention in car context, and document the automatic behavior so the team understands the implementation.

## Standard Stack

### Core Audio Focus Components

| Component | Version | Purpose | Why Standard |
|-----------|---------|---------|--------------|
| Media3/ExoPlayer | 1.3.1 | Built-in audio focus handling via ExoPlayer.Builder | Standard solution for modern Android playback; handles focus lifecycle automatically |
| AudioAttributes | AndroidX API 29+ | Configure content type (MOVIE) and usage (MEDIA) | Part of Media3 API; tells system how to manage focus based on content |
| Foreground Service | Android 10+ (API 29) | Maintains focus while app backgrounded | Required for persistent audio focus; prevents OS from revoking focus |

### What's Already Configured

The project already has optimal setup:

```kotlin
ExoPlayer.Builder(context)
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)  // Video content hint
            .setUsage(C.USAGE_MEDIA)                      // Foreground media usage
            .build(),
        /* handleAudioFocus = */ true  // Media3 manages full lifecycle
    )
    .setHandleAudioBecomingNoisy(true)  // Pause on BT disconnect
    .build()
```

- **AudioAttributes configured correctly:** USAGE_MEDIA (primary media usage) + AUDIO_CONTENT_TYPE_MOVIE (video routing) tells Android this is standard video playback and to apply normal audio focus rules.
- **handleAudioFocus=true:** Media3 automatically requests focus when playback starts and abandons on stop. No manual calls needed.
- **setHandleAudioBecomingNoisy(true):** Pauses playback if Bluetooth headphones disconnect (separate from focus, but related audio experience).
- **Foreground service declared:** AndroidManifest.xml line 62 declares `android:foregroundServiceType="mediaPlayback"` and minSdk 29 supports foreground service media playback type.

**Permission status:** MODIFY_AUDIO_SETTINGS is NOT required when using ExoPlayer's automatic focus handling (only needed for manual AudioManager calls). Foreground service permissions are already declared (FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK).

### Alternatives Considered

| Instead of | Could Use | Why Not Chosen |
|-----------|-----------|----------------|
| Media3 automatic (handleAudioFocus=true) | Manual AudioManager.requestAudioFocus() | Manual approach requires implementing OnAudioFocusChangeListener, handling all transient/permanent loss cases, and interferes with Media3's automatic system (conflict/double requests). Media3 automatic is simpler and less error-prone. |
| USAGE_MEDIA | USAGE_GAME, USAGE_VOICE_COMMUNICATION, USAGE_ALARM | USAGE_MEDIA is the standard for entertainment content. USAGE_GAME is for games; VoIP for calls; ALARM for alarms. |
| AUDIO_CONTENT_TYPE_MOVIE | AUDIO_CONTENT_TYPE_MUSIC, AUDIO_CONTENT_TYPE_SPEECH | MOVIE provides audio routing optimized for video playback (drivers in car context). MUSIC is for audio-only; SPEECH for podcasts/audiobooks. |

**Installation:** No new packages needed — Media3 1.3.1 is already in build.gradle.kts.

## Architecture Patterns

### Audio Focus Lifecycle in Media3 (Automatic)

When `handleAudioFocus=true`, ExoPlayer manages this lifecycle internally:

```
1. User calls player.play() or player.playWhenReady = true
   ↓
2. ExoPlayer requests AUDIOFOCUS_GAIN from AudioManager
   (tells system: "I'm playing primary audio; pause/duck others")
   ↓
3. System pauses/ducks other apps (if they respect focus)
   ↓
4. Playback proceeds; ExoPlayer listens for focus changes
   ↓
5. If another app requests focus (phone call, nav):
   - AUDIOFOCUS_LOSS: ExoPlayer pauses, stays paused until regain
   - AUDIOFOCUS_LOSS_TRANSIENT: ExoPlayer pauses, resumes on regain
   - AUDIOFOCUS_LOSS_TRANSIENT_MAY_DUCK: Volume reduced, no pause
   ↓
6. User calls player.pause() / player.stop()
   ↓
7. ExoPlayer releases focus (tells system: "I'm done, others can play")
```

**Key insight:** Media3 handles all the callbacks internally. Your code doesn't see AUDIOFOCUS_* constants because ExoPlayer converts them to playback state changes (pause/resume).

### Monitoring Focus Changes (if needed for logging/analytics)

If you need to know WHY playback paused, use Player.Listener.onPlaybackSuppressionReasonChanged:

```kotlin
player.addListener(object : Player.Listener {
    override fun onPlaybackSuppressionReasonChanged(
        @Player.PlaybackSuppressionReason reason: Int
    ) {
        when (reason) {
            Player.PLAYBACK_SUPPRESSION_REASON_NONE 
                → Log.d("Focus", "Playing normally")
            Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS
                → Log.d("Focus", "Paused: transient focus loss (will resume)")
            Player.PLAYBACK_SUPPRESSION_REASON_MUTED
                → Log.d("Focus", "Paused: muted")
            // ... other reasons
        }
    }
})
```

This is for **observability only** — the actual pause/resume behavior is automatic.

### Foreground Service Focus Retention

The foreground service ensures audio focus is maintained even when the app goes to background:

**Why this matters:** Without foreground service, the system can revoke audio focus when the app is backgrounded (not visible to user). The foreground service indicates to the system: "This app is actively providing user-facing service (playback) — keep it alive and maintain audio focus."

**Current setup (already correct):**
- Service declared with `android:foregroundServiceType="mediaPlayback"` (AndroidManifest.xml line 62)
- Service posts a MediaStyle notification (required for all foreground services on Android 8+)
- Mindk 29+ required and configured

**What happens at app exit:**
1. User closes app or navigates away
2. If playback is ongoing, foreground service keeps running (system-managed lifecycle)
3. Audio focus remains held until playback stops or service is destroyed
4. On pause/stop (or 10+ minutes inactivity), service is backgrounded and foreground service state removed

### Automotive Audio Routing (Car Context)

Android Automotive head units have independent audio focus management. Key differences from phone:

- **Multi-zone audio:** Some vehicles have separate driver/passenger zones. Focus is managed per zone independently. This app (NAVIGATION category, single video playback) only affects one zone.
- **System audio hierarchy:** Car head unit firmware may enforce its own audio priority (nav > calls > media). Your audio focus request interacts with system routing, not overrides it.
- **Exclusive focus:** Many automotive systems use exclusive focus (only one app plays at a time) rather than ducking.

**Bottom line for this app:** Audio focus works correctly on car head units because the system respects standard Android focus requests. The AUDIO_CONTENT_TYPE_MOVIE hint helps route video audio appropriately through car speakers.

### Audio Focus Loss Handling in Foreground Service

When focus is lost while service is foreground:

```
Focus Loss Event (transient or permanent)
      ↓
ExoPlayer pauses internally (no code needed)
      ↓
Player.onIsPlayingChanged(false) fires
      ↓
MediaPlayerManager updates _isPlaying StateFlow
      ↓
UI responds to isPlaying state change (pause button shows, playback pauses)
      ↓
Foreground service continues running (notification still shows)
      ↓
User can explicitly tap play to resume (requests focus again)
```

### Anti-Patterns to Avoid

- **Manual requestAudioFocus() calls:** Don't add AudioManager.requestAudioFocus(). Media3 already requests focus automatically. Calling it again creates duplicate/conflicting requests that confuse the system.
- **Implementing OnAudioFocusChangeListener:** Don't listen for raw focus callbacks. Media3 abstracts these as playback state changes (onIsPlayingChanged). Listening directly bypasses Media3's coordination.
- **Trying to silence competing apps:** Android audio focus is cooperative — apps that don't respect focus (some music apps, YouTube offline) won't pause. You can't force them. The system provides the mechanism; app cooperation determines results.
- **Abandoning focus too early:** Don't call abandonAudioFocus(). Media3 calls it automatically. Doing it manually can interfere with Media3's lifecycle.
- **USAGE_MEDIA with transient-only requests:** The automatic audio focus feature only works with permanent requests (USAGE_MEDIA, USAGE_GAME). Trying to use transient focus with automatic handling throws an exception.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Requesting audio focus when playback starts | Custom AudioManager wrapper with requestAudioFocus() callbacks | Media3's built-in handleAudioFocus=true | Media3 handles all state transitions (buffering → ready, pause ↔ play). Manual implementation misses these nuances and conflicts with Media3's internal focus management. |
| Pausing when another app takes focus | OnAudioFocusChangeListener implementation with pause/resume logic | Media3's automatic response to focus loss | Media3 maps focus loss types to playback state changes. You'd be reimplementing what ExoPlayer already does. |
| Detecting why playback paused | Manual AudioManager monitoring | Player.onPlaybackSuppressionReasonChanged() | ExoPlayer provides suppression reason via listener (AUDIO_FOCUS_LOSS, MUTED, etc.). AudioManager focus callbacks are lower-level and harder to correlate with playback. |
| Managing focus lifecycle (request → hold → release) | Manual lifecycle tracking across play/pause/stop | Media3's transparent lifecycle tied to playWhenReady | Media3 automatically requests on play, releases on stop. Manual tracking introduces edge cases (buffering state, error recovery, background transition). |

**Key insight:** Audio focus is already managed. Your job is not to "implement" focus — it's to verify ExoPlayer's built-in system is working and test it in the car context.

## Common Pitfalls

### Pitfall 1: Double Request Conflicts (Manual + Automatic)

**What goes wrong:** Developer adds AudioManager.requestAudioFocus() calls, then calls setAudioAttributes(..., handleAudioFocus=true). System receives duplicate requests. One succeeds, others fail or create state confusion. Playback may pause unexpectedly or not pause when it should.

**Why it happens:** Developer doesn't realize Media3 is already requesting. Sees focus mentioned in requirements and thinks "I need to call AudioManager." 

**How to avoid:** Understand that handleAudioFocus=true is a permission grant to Media3 to manage focus on the app's behalf. Don't manually request. If debugging, check logcat for "audio focus" — you'll see Media3's automatic requests.

**Warning signs:** 
- AudioManager method calls in MediaPlayerManager
- OnAudioFocusChangeListener callbacks firing unexpectedly
- Focus request failures in logcat (AudioManager logs "requestAudioFocus() FAILED")

### Pitfall 2: Focus Loss Not Triggering Pause

**What goes wrong:** Another app requests focus, Media3 gets the callback, but playback doesn't pause. User hears video audio overlapping with nav instructions or music.

**Why it happens:** 
- AudioAttributes set incorrectly (USAGE_VOICE_COMMUNICATION can't use automatic focus)
- handleAudioFocus=false or not set
- Another app doesn't respect focus (some premium music apps ignore focus to keep playing)

**How to avoid:** 
- Verify AudioAttributes match content type (USAGE_MEDIA for video)
- Check handleAudioFocus=true in Builder
- Test on real car head unit (emulator focus behavior differs)
- Understand some apps are non-compliant; you can't force them to stop

**Warning signs:**
- Audio overlap in car playback
- onIsPlayingChanged(false) never fires when another app plays
- Player.PLAYBACK_SUPPRESSION_REASON_* never changes from NONE

### Pitfall 3: Focus Release on Background

**What goes wrong:** App backgrounded, playback is ongoing, focus is released prematurely. User hears music resume from another app while driving.

**Why it happens:** 
- Foreground service not declared properly
- Service stops when app backgrounded
- Code calls abandonAudioFocus() on Activity onPause (wrong place to release)

**How to avoid:**
- Ensure AndroidManifest.xml declares foregroundServiceType="mediaPlayback" ✓ (already done)
- Never call abandonAudioFocus() in Activity lifecycle
- Let foreground service manage focus lifecycle
- Test: start playback, background app, verify playback + focus continue

**Warning signs:**
- Playback pauses when app backgrounded
- Other audio apps resume when app goes to background
- Service stops when Activity destroyed

### Pitfall 4: Automotive-Specific: Car Audio Routing Mismatch

**What goes wrong:** Audio plays but routes to wrong car speaker (cabin speakers vs. center console; or only one zone in multi-zone vehicle). Focus appears to work (other apps pause) but audio is inaudible or muted.

**Why it happens:**
- AUDIO_CONTENT_TYPE incorrect (SPEECH instead of MOVIE)
- Car head unit has custom audio routing rules
- USAGE attribute doesn't match vehicle's audio policy

**How to avoid:**
- Keep AUDIO_CONTENT_TYPE_MOVIE for video (correct for this app)
- Keep USAGE_MEDIA for standard playback
- Test on real car head unit or Android Automotive emulator
- Document: "Video plays through primary audio output as per system audio policy"

**Warning signs:**
- Audio inaudible despite focus granted
- Audio routes to unexpected speaker
- Car head unit shows "Muted" or "Not routed" for app

### Pitfall 5: Misunderstanding Transient Focus Loss

**What goes wrong:** Navigation pops up, user expects Media3 to duck (reduce volume) not pause. Or vice versa: Media3 ducks when user expects pause.

**Why it happens:** Confusing AUDIOFOCUS_LOSS_TRANSIENT_MAY_DUCK behavior. Media3 behavior depends on CONTENT_TYPE (MUSIC ducks, SPEECH pauses). 

**How to avoid:**
- Understand current setup uses AUDIO_CONTENT_TYPE_MOVIE, which ducks on transient loss (reduces volume, keeps playing)
- Document this behavior for QA
- If pause-on-interrupt is desired, would need CONTENT_TYPE_SPEECH (but that's for podcasts, not video)
- Current ducking behavior is correct for video playback

**Warning signs:**
- Unexpected volume reduction during nav instructions
- Playback pauses then resumes (indicates transient loss + automatic resume)

## Code Examples

### Correct Setup (Already in Place)

Source: MediaPlayerManager.kt lines 67-76 (existing code — verify and document)

```kotlin
val player: ExoPlayer = ExoPlayer.Builder(context)
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)  // Video content
            .setUsage(C.USAGE_MEDIA)                      // Standard media usage
            .build(),
        /* handleAudioFocus = */ true  // Media3 manages lifecycle
    )
    .setHandleAudioBecomingNoisy(true)  // Pause on BT disconnect
    .build()
```

**What this does:**
- CONTENT_TYPE_MOVIE: Informs system to route audio through video-appropriate speakers, duck (not pause) on transient interruptions
- USAGE_MEDIA: Standard foreground media playback
- handleAudioFocus=true: ExoPlayer requests focus on play(), releases on stop()
- handleAudioBecomingNoisy=true: Pause if Bluetooth headphones disconnect

**Verification:** No additional code needed. This is the standard pattern.

### Monitoring Suppression Reason (For Logging/Testing)

Source: Official Media3 documentation + Player.Listener pattern

```kotlin
// Add to existing Player.Listener in MediaPlayerManager if suppression tracking needed
player.addListener(object : Player.Listener {
    override fun onPlaybackSuppressionReasonChanged(
        @Player.PlaybackSuppressionReason reason: Int
    ) {
        Log.d(TAG, "Playback suppression reason changed: $reason")
        // For debugging: why did playback pause?
        // Reasons: NONE (normal), TRANSIENT_AUDIO_FOCUS_LOSS, MUTED, PLAYER_NOT_STARTED, etc.
    }

    // Already exists in codebase:
    override fun onIsPlayingChanged(playing: Boolean) {
        _isPlaying.value = playing
        // UI automatically reflects pause/play based on this
    }
})
```

**When to use:** Debugging suspected focus issues. If playback pauses unexpectedly, check suppression reason in logcat to understand why (focus loss vs. muting vs. other).

### Automotive Testing Pattern

Source: Android Automotive documentation + car context requirements

```kotlin
// Test pattern — verify focus works in car context
// Test on: Android Automotive emulator or real head unit

// 1. Start playback
player.playWhenReady = true

// 2. Verify focus requested (check logcat for AudioFocusManager logs)
// Expected: "AudioFocusManager: Requested audio focus"

// 3. Background the app (press home, switch to nav)
// Expected: Playback continues, focus held, foreground service persists

// 4. Bring another audio app to foreground (music, podcast)
// Expected: Video playback pauses (focus lost), other app plays

// 5. Bring video app back to foreground
// Expected: Other app pauses, video playback resumes (focus regained)

// In code: No new methods needed — test via UI/manual verification
// Just verify _isPlaying StateFlow reflects these transitions
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual OnAudioFocusChangeListener (pre-ExoPlayer 2.9) | Media3 automatic handleAudioFocus=true | ExoPlayer 2.9 (2019) | Simpler code, less error-prone. Manual listeners still supported but discouraged. |
| Separate audio focus + playback logic | Unified via AudioAttributes (handleAudioFocus) | Media3 1.0+ (2021) | Audio focus is now coordinated with ExoPlayer lifecycle transparently. |
| Focus requests in Activity lifecycle | Focus tied to foreground service lifecycle | Android 8+ (2017), enforced in car context | Apps must use foreground service for background playback, not manual lifecycle management. |

**Deprecated/Outdated:**
- requestAudioFocus(listener, streamType, hint): Pre-API 26 pattern. Replaced by AudioFocusRequest.Builder. Not used here (Media3 handles it).
- Direct AudioManager focus calls in media apps: Superseded by ExoPlayer automatic handling. Still possible but creates conflict with Media3's internal focus management.

## Open Questions

1. **Does the car head unit respect standard audio focus requests?**
   - What we know: Android Automotive head units implement focus per the AOSP spec. Standard requests (USAGE_MEDIA, AUDIOFOCUS_GAIN) should work.
   - What's unclear: Target head unit's specific firmware and audio policy.
   - Recommendation: Test on target head unit early (Phase 2). If focus doesn't work, issue is head unit firmware, not app.

2. **What happens if playback is paused for >10 minutes?**
   - What we know: Android foreground service docs state that if service has no playback activity for 10+ minutes, it auto-exits foreground state.
   - What's unclear: Whether this also releases audio focus.
   - Recommendation: ExoPlayer handles this. If focus is released, next play() call will request it again. Test the scenario (pause, wait, resume).

3. **Does AUDIO_CONTENT_TYPE_MOVIE work for all video types (HLS, MP4, etc.)?**
   - What we know: Content type is a hint to the system, not a codec restriction. Works for any video format ExoPlayer supports.
   - What's unclear: Whether car audio systems have special handling for MOVIE type.
   - Recommendation: Current choice (MOVIE) is correct. No changes needed.

4. **Transient focus loss behavior: Does ducking work in car context?**
   - What we know: AUDIO_CONTENT_TYPE_MOVIE + transient loss = volume ducking (not pause).
   - What's unclear: Whether car head unit audio routing supports ducking or just mutes.
   - Recommendation: Verify in car testing. If ducking doesn't work, document as car-specific limitation.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Media3/ExoPlayer | Audio focus + playback | ✓ | 1.3.1 | No fallback; core dependency |
| Android Foreground Service API | Focus retention while backgrounded | ✓ | API 26+ (project minSdk 29) | Focus not maintained if not used (critical) |
| Car App Library | Android Auto integration | ✓ | 1.7.0 | None (core to sideload approach) |
| AudioManager system service | Media3 uses internally | ✓ (system) | All API levels | Cannot be replaced; always available |
| Car head unit / Android Automotive emulator | Testing audio focus behavior | ? | Varies | Phone emulator won't test automotive routing; real device required |

**Missing dependencies with no fallback:**
- Real Android Automotive head unit or emulator: Required to verify focus behavior in car context. Phone emulator's audio focus behavior differs from car.

**Missing dependencies with fallback:**
- Car head unit unavailable: Can test focus on phone emulator initially (will work), but automotive-specific routing tests must happen on real car hardware.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | No test suite exists yet (reference: CLAUDE.md, Testing section) |
| Config file | None — see Wave 0 |
| Quick run command | Manual test on car head unit: Start playback → Pause another app → Verify pause → Resume |
| Full suite command | N/A — Phase requires real device testing |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| REQ-AUDIO-01 | AudioManager.requestAudioFocus() called when playback starts | Manual (requires car head unit) | N/A — ExoPlayer does this automatically | ✓ (Media3 built-in) |
| REQ-AUDIO-02 | System pauses other audio apps (if they respect focus) | Manual (requires competing app on car head unit) | Start playback, start music app, verify pause | N/A — car-dependent |
| REQ-AUDIO-03 | Release focus on pause/stop | Manual (logcat AudioFocusManager logs) | Pause playback, check logcat | ✓ (Media3 automatic) |
| REQ-AUDIO-04 | Foreground service maintains focus while backgrounded | Manual (background app while playing, verify continues) | Start playback → Press home → Verify continues playing | ✓ (service declared) |

### Sampling Rate

- **Per task commit:** Manual verification on car head unit: start playback, pause other apps, verify pause. (~5 minutes)
- **Per wave merge:** Full playback cycle test: play → background → pause other app → resume → stop. Verify focus held throughout. (~15 minutes)
- **Phase gate:** Playback doesn't pause unexpectedly; other apps pause when video plays (if they respect focus); focus released on stop (logcat verification).

### Wave 0 Gaps

- [ ] No gaps. Media3 1.3.1 already provides correct audio focus handling.
- [ ] Verification needed: Test on real car head unit (not phone emulator) to confirm car audio routing and focus behavior.
- [ ] Documentation: Document "Audio Focus Implementation" in codebase explaining automatic Media3 behavior (so future developers don't try to add manual requests).

*(If real car head unit available for testing: "Audio focus already implemented correctly. Test only requires manual verification on car hardware.")*

## Sources

### Primary (HIGH confidence)

- [Manage audio focus — Android Developers](https://developer.android.com/media/optimize/audio-focus) — Official documentation on audio focus best practices, Media3 automatic handling, and implementation patterns
- [Media3 ExoPlayer — Android Developers](https://developer.android.com/media/media3/exoplayer) — Official Media3/ExoPlayer documentation including audio attributes and focus configuration
- [Create a basic media player app using Media3 ExoPlayer — Android Developers](https://developer.android.com/media/implement/playback-app) — Standard implementation patterns, foreground service requirements, audio focus integration
- [Audio focus — Android Open Source Project](https://source.android.com/docs/automotive/audio/audio-focus) — Automotive-specific audio focus rules, multi-zone management, car routing

### Secondary (MEDIUM confidence)

- [Easy Audio Focus with ExoPlayer — Medium/Google Developers](https://medium.com/google-exoplayer/easy-audio-focus-with-exoplayer-a2dcbbe4640e) — Practical patterns, ducking vs. pause tradeoffs, limitations of automatic handling
- [Managing Audio Focus — Android Developers Blog](https://android-developers.googleblog.com/2013/08/respecting-audio-focus.html) — Historical context on audio focus design, why apps must respect it
- [Background playback with a MediaSessionService — Android Developers](https://developer.android.com/media/media3/session/background-playback) — Foreground service lifecycle, focus maintenance, notification requirements

## Metadata

**Confidence breakdown:**
- **Standard stack:** HIGH — Media3 1.3.1 audio focus is official and well-documented
- **Architecture:** HIGH — Official Android docs clearly explain automatic handling; existing codebase confirms correct setup
- **Pitfalls:** MEDIUM-HIGH — Documented pitfalls from official blogs and GitHub issues; automotive-specific behavior requires car hardware verification
- **Environment:** MEDIUM — Phone emulator can test focus; real car head unit behavior requires access

**Research date:** 2026-04-17  
**Valid until:** 2026-05-01 (Media3 updates frequently; recheck for new versions)

**Key assumption verified:** Audio focus in Media3 1.3.1 is automatic when handleAudioFocus=true and AudioAttributes configured correctly. No manual AudioManager calls needed.
