# SPEC.md — Project Specification

> **Status**: `FINALIZED`
>
> ⚠️ **Planning Lock**: No code may be written until this spec is marked `FINALIZED`.

## Vision
A specialized media player for Android Auto that bridges the gap between high-quality video streaming (from personal servers like Plex and Jellyfin) and the car display. It provides a safe, responsive, and intuitive interface for browsing and playing back video and audio content while connected to a head unit.

## Goals
1. **Seamless Video Integration** — Provide smooth video playback on raw car displays using the Car App Library and Media3 ExoPlayer.
2. **Multi-Source Discovery** — Allow users to browse and play media from Local storage, Plex, and Jellyfin within a unified interface.
3. **Robust Playback Persistence** — Automatically save and resume playback positions across sessions to ensure a continuous experience.
4. **Optimized for Car UI** — Ensure all interactions meet automotive safety and usability standards (large targets, high contrast, minimal distractions).

## Non-Goals (Out of Scope)
- Direct video editing or metadata management.
- Support for DRM-protected commercial streaming services (Netflix, Disney+, etc.).
- Offline downloading for remote sources (streaming only for now).
- Background video playback on the phone UI (focus is exclusively on Android Auto).

## Constraints
- **Technical**: Must follow Android Auto's `CarAppService` restrictions and templates.
- **Safety**: Video playback must only be available when the car reports it is safe/parked (logic handled by AA host, rendering must respect it).
- **Network**: Must handle variable connectivity (switching between Wi-Fi and Cellular).

## Success Criteria
- [ ] Successful video playback on an Android Auto head unit from a Jellyfin/Plex source.
- [ ] Correct aspect ratio rendering on various car screen resolutions via `Presentation` effects.
- [ ] Playback resume works correctly for both local and remote files.
- [ ] Favorites can be added and successfully played from the Favorites screen.

## Technical Requirements

| Requirement | Priority | Notes |
|-------------|----------|-------|
| Surface Scaling | Must-have | `VideoSurfaceRenderer` must correctly scale video to fill available space while maintaining aspect ratio. |
| Favorite Playback | Must-have | Users must be able to start playback directly from the Favorites screen. |
| Media3 Integration | Must-have | Use Media3 Session and Player for all playback logic. |
| Security | Should-have | Implement proper host validation for Play Store readiness. |
| Theming | Nice-to-have | Support dynamic UI colors based on the car's day/night mode. |

---

*Last updated: 2026-04-21*
