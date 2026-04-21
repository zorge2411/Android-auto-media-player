---
milestone: v1.0 Launch
version: 1.0.0-alpha
updated: 2026-04-21
---

# Roadmap

> **Current Phase:** 2 - Feature Closure
> **Status:** complete

## Must-Haves (from SPEC)

- [x] Accurate Video Surface Scaling (fix TODO)
- [x] Working "Play from Favorites" (fix TODO)
- [x] Stable Media3 Playback for Plex/Jellyfin
- [ ] Proper Android Auto Host Validation

---

## Phases

### Phase 1: Foundation & Scaling
**Status:** ✅ Complete
**Objective:** Complete the video surface rendering logic to ensure correct aspect ratios and fill on all car displays.
**Requirements:** Surface Scaling

**Plans:**
- [ ] Plan 1.1: Complete transformation logic in `VideoSurfaceRenderer`

---

### Phase 2: Feature Closure
**Status:** ✅ Complete
**Objective:** Implement the remaining core features that allow users to use the app effectively.
**Requirements:** Favorite Playback, Robust Persistence

**Plans:**
- [x] Plan 2.1: Implement Playback logic in `FavoritesScreen`
- [x] Plan 2.2: Refine remote source error handling

---

### Phase 3: Stability & Readiness
**Status:** ⬜ Not Started
**Objective:** Prepare the app for real-world usage and potential Play Store distribution.
**Requirements:** Security, Stability

**Plans:**
- [ ] Plan 3.1: Implement production-ready `HostValidator`
- [ ] Plan 3.2: Final performance and UI polish

---

## Progress Summary

| Phase | Status | Plans | Complete |
|-------|--------|-------|----------|
| 1 | ✅ | 1/1 | 2026-04-21 |
| 2 | ✅ | 2/2 | 2026-04-21 |
| 3 | ⬜ | 0/2 | — |

---

## Timeline

| Phase | Started | Completed | Duration |
|-------|---------|-----------|----------|
| 1 | — | 2026-04-21 | — |
| 2 | — | 2026-04-21 | — |
| 3 | — | — | — |
