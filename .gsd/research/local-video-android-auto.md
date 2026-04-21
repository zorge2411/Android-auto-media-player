---
phase: General
level: 2
researched_at: 2026-04-21
---

# Research: Local Video Playback on Android Auto

## Questions Investigated
1. How does an Android Auto application request local storage permissions when the car screen cannot natively display Android OS dialogue prompts?
2. How can we query the local device storage for video files to present in the Car App UI?
3. Can ExoPlayer natively handle local `content://` URIs securely when initiated from an Android Auto service?

## Findings

### 1. Requesting Storage Permissions on Android Auto
The Android Auto interface is heavily restricted and cannot draw standard Android permission dialog boxes (like `READ_MEDIA_VIDEO` or `READ_EXTERNAL_STORAGE`).
- If an app attempts to read `MediaStore` without permissions, a `SecurityException` is thrown.
- **Solution:** The Car App Library provides `CarContext.requestPermissions()`. When invoked, if the car is parked, it will push a notification/dialog flow to the user's phone, prompting them to grant the permission on the handset.
- **Best Practice:** Display a `MessageTemplate` or `CarToast` on the car screen informing the user: *"Please check your phone to grant storage permissions to view local videos"*, along with an `Action` button hooked to `requestPermissions()`.

### 2. Querying Local Video Files
Once permissions are granted, accessing local videos is identical to standard Android development.
- **Querying:** Use `ContentResolver.query` against `MediaStore.Video.Media.EXTERNAL_CONTENT_URI`. 
- **Columns:** Retrieve `_ID`, `DISPLAY_NAME`, `DURATION`, `RESOLUTION`, and `MIME_TYPE`.
- **URIs:** Build continuous `content://media/external/video/media/{_ID}` URIs to pass to the media player.
- **Car App UI:** Map these queries to an `ItemList` and present them via a `ListTemplate` or `GridTemplate` in the vehicle.

### 3. ExoPlayer and `content://` URIs
ExoPlayer natively resolves MediaStore `content://` URIs utilizing the standard `DefaultDataSource.Factory`. 
- No custom `DataSource` or `ContentProvider` is necessary. 
- *Note:* The Android Auto service (`CarAppService`) runs within the host application process and shares the same permission context, meaning if the host app holds `READ_MEDIA_VIDEO`, ExoPlayer running inside the Auto session can arbitrarily read these URIs without crashing.

**Sources:**
- Official Google Car App Library Documentation regarding `requestPermissions`
- Android `MediaStore` Developer Guide

**Recommendation:** 
Build a dedicated `LocalVideoRepository` that wraps the MediaStore query. Create a "Local Media" entry in Android Auto `ListTemplate` that first checks `ContextCompat.checkSelfPermission(...)`. If unauthorized, push the phone permission flow. If authorized, display the local device videos inside the car screen.

## Decisions Made
| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Permission Handling** | `CarContext.requestPermissions()` | Only officially supported method to request Android runtime permissions from a vehicle screen. |
| **Data Source** | `MediaStore.Video.Media` | Safest, most standard way to scan the device for video content across all Android versions (Android 10+ scoped storage compliant). |

## Patterns to Follow
- **Parked Checks:** For complex UI setups (like granting permissions), enforce `ParkedOnlyOnClickListener` so the driver is not distracted by looking at their phone while moving.
- **Pagination:** MediaStore queries can return thousands of videos. Use `LIMIT` and `OFFSET` or windowing to avoid blowing up the `ItemList` bounds in the Car App Library (which is strictly capped at ~6-100 items depending on template).

## Anti-Patterns to Avoid
- **Direct File Paths (`java.io.File`)**: Do not query `DATA` columns or attempt to pass `file://` paths. Scoped Storage (Android 10+) strictly blocks this for external apps. Always use `content://` URIs.
- **Blocking the Main Thread**: `ContentResolver.query()` is a database operation. It must be launched in a Coroutine (e.g., `Dispatchers.IO`) so it does not freeze the Android Auto projection thread causing ANRs.

## Dependencies Identified
None. This relies purely on `android.provider.MediaStore` and `androidx.car.app.CarContext`, which are already integrated into the active project.

## Risks
- **Risk**: User attempts to play unsupported local codecs (e.g., specific HEVC profiles or MKV files that the car host hardware decoder fails to process).
- **Mitigation**: Filter queries by `MIME_TYPE` to only allow standard formats (e.g., `video/mp4`, `video/avc`) or ensure the ExoPlayer configuration includes software decoding fallbacks via `CCodec`.

## Ready for Planning
- [x] Questions answered
- [x] Approach selected
- [x] Dependencies identified
