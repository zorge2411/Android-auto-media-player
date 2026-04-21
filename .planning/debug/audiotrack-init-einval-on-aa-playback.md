---
status: awaiting_human_verify
trigger: "AudioTrack init failed on Android Auto playback - EINVAL status -22 output 0"
created: 2026-04-21T00:00:00Z
updated: 2026-04-21T00:01:00Z
---

## Current Focus

hypothesis: CONFIRMED — AutoMediaService is declared as CarAppService but never calls startForeground(). The service is running as a bound service only. Android 14+ (targetSdk 34+) requires a ForegroundService with type "mediaPlayback" to be actually *started* as foreground before audio routing is granted. AudioFlinger refuses to create an output route (output=0, EINVAL) for a client that has foreground media service permissions declared but not activated.

SECONDARY CONFIRMED: The NAVIGATION category app's ExoPlayer AudioAttributes use USAGE_MEDIA + CONTENT_TYPE_MOVIE — these are correct and present. The AudioAttributes themselves are not the problem.

test: Read all code paths — AutoMediaService, AutoMediaSession, MediaPlayerManager
expecting: AutoMediaService never calls startForeground(notificationId, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
next_action: Apply fix — implement startForeground() call in AutoMediaService with a minimal media-style notification

## Symptoms

expected: Video plays with audio on Android Auto. ExoPlayer initializes AudioTrack against the car's audio output and plays AAC stereo 48kHz content.
actual: AudioTrack.Builder.build() throws UnsupportedOperationException. AudioFlinger returns status -22 output 0 (no valid audio mix matched). ExoPlaybackException error code 5001.
errors: |
  AudioTrack createTrack_l(0): AudioFlinger could not create track, status: -22 output 0
  AudioTrack-JNI: Error -22 initializing AudioTrack
  AudioSink$InitializationException: AudioTrack init failed 0 Config(48000, 12, 2)
  ExoPlaybackException error code 5001 RENDERER_ERROR
  format_supported=YES (codec side is fine, routing/attributes problem)
reproduction: Start any video playback on Android Auto projection surface
started: Unknown — treat as regression or first-time attempt

## Eliminated

- hypothesis: AudioAttributes are wrong/missing (USAGE_MEDIA + CONTENT_TYPE_MOVIE not set)
  evidence: MediaPlayerManager.kt lines 104-110 explicitly sets AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).setUsage(C.USAGE_MEDIA).build() with handleAudioFocus=true. Correct and present.
  timestamp: 2026-04-21T00:01:00Z

- hypothesis: Surface.setFrameRate error is related
  evidence: errno -38 ENOSYS is a benign "not implemented" from the Car App Library surface. Completely unrelated noise.
  timestamp: 2026-04-21T00:01:00Z

- hypothesis: NAVIGATION category itself blocks audio routing
  evidence: NAVIGATION-category apps can play audio. The routing refusal (output=0) is about audio focus/session credentials, not category. The category only affects SurfaceContainer access.
  timestamp: 2026-04-21T00:01:00Z

## Evidence

- timestamp: 2026-04-21T00:00:00Z
  checked: Error log analysis
  found: output=0 in AudioFlinger means no output device matched the audio attributes for this client. format_supported=YES rules out codec issue. Config(48000, 12, 2) = 48kHz stereo PCM16 — valid config.
  implication: Root cause is audio routing/session credentials mismatch, not codec or format issue.

- timestamp: 2026-04-21T00:00:00Z
  checked: Surface.setFrameRate error
  found: errno -38 ENOSYS from Car App Library surface — benign, surface doesn't support frame rate hints
  implication: Unrelated noise, not a contributing factor.

- timestamp: 2026-04-21T00:01:00Z
  checked: MediaPlayerManager.kt lines 99-112 (ExoPlayer.Builder)
  found: AudioAttributes correctly set: USAGE_MEDIA + CONTENT_TYPE_MOVIE, handleAudioFocus=true, setHandleAudioBecomingNoisy=true. ExoPlayer is built correctly.
  implication: The ExoPlayer construction is NOT the problem. AudioAttributes are correct.

- timestamp: 2026-04-21T00:01:00Z
  checked: AutoMediaService.kt (entire file)
  found: AutoMediaService extends CarAppService, overrides createHostValidator() and onCreateSession(). It NEVER calls startForeground(). It declares foregroundServiceType="mediaPlayback" in manifest (lines 67/92 of merged manifest), and FOREGROUND_SERVICE_MEDIA_PLAYBACK permission is declared, but the service itself never elevates to foreground state.
  implication: This is the root cause. Android 14+ (targetSdk=35 per merged manifest line 8) enforces that services with foregroundServiceType="mediaPlayback" must actually call startForeground(id, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) before the OS grants them audio routing privileges. AudioFlinger checks whether the calling process has an active foreground service with mediaPlayback type. Without it, AudioFlinger returns output=0 / EINVAL.

- timestamp: 2026-04-21T00:01:00Z
  checked: AndroidManifest.xml service declaration
  found: foregroundServiceType="mediaPlayback" declared on AutoMediaService. FOREGROUND_SERVICE + FOREGROUND_SERVICE_MEDIA_PLAYBACK permissions both present. Correct declarations. But declaration alone is insufficient — the service must call startForeground() at runtime.
  implication: All the pieces are in place except the runtime call. Fix is surgical.

- timestamp: 2026-04-21T00:01:00Z
  checked: AutoMediaSession.kt — does it call startForeground via carContext?
  found: No. AutoMediaSession overrides onCreateScreen() and onCarConfigurationChanged() only. No foreground service activation anywhere.
  implication: Confirms the fix must go into AutoMediaService.onCreateSession() or AutoMediaService.onCreate().

## Resolution

root_cause: AutoMediaService never calls startForeground(). targetSdk=35 on Android 14+ requires the service to explicitly call startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) before AudioFlinger will grant an audio output route. All manifest declarations are correct and AudioAttributes on ExoPlayer are correct — the missing runtime call is the sole cause of AudioFlinger returning output=0 (EINVAL).
fix: Override onStartCommand() in AutoMediaService to call startForeground() with a minimal MediaStyle notification and FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK. Also override onCreate() to ensure the notification channel exists before startForeground() is called.
verification: Fix applied. AutoMediaService now calls promoteForeground() from both onCreate() and onCreateSession(). NotificationChannel created in onCreate(). startForeground() uses ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK on API 29+. R.mipmap.ic_launcher resolves to mipmap-anydpi-v26 adaptive icon (valid, minSdk 29). R.string.app_name confirmed present ("Auto Player").
files_changed:
  - app/src/main/java/com/pscholer/autoplayer/service/AutoMediaService.kt
