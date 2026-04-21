# GSD Debug Knowledge Base

Resolved debug sessions. Used by `gsd-debugger` to surface known-pattern hypotheses at the start of new investigations.

---

## jellyfin-auth-malformed-json — Gson MalformedJsonException caused by Pangolin reverse-proxy cross-domain redirect
- **Date:** 2026-04-17
- **Error patterns:** MalformedJsonException, IllegalStateException, authentication failed, text/html, 302 redirect, cross-domain, Pangolin, wireguard, reverse-proxy, auth portal, line 1 column 1
- **Root cause:** Jellyfin base URL routes through a Pangolin reverse-proxy. Pangolin intercepts unauthenticated API calls with a 302 redirect to its own HTML auth portal on a different host. OkHttp follows the redirect automatically, so Gson receives an HTML body and throws MalformedJsonException, which is silently swallowed — producing a generic "authentication failed" error with no actionable guidance.
- **Fix:** (1) Change JellyfinApi.authenticate() to Response<JellyfinAuthResponse> to prevent Gson crash on non-JSON bodies. (2) Add lenient Gson + BODY-level OkHttp logging. (3) Add CrossDomainRedirectException (extends IOException) thrown by an OkHttp interceptor when response host differs from request host. (4) SettingsActivity catches CrossDomainRedirectException and shows a toast naming Pangolin and instructing the user to enable Extended compatibility in their Pangolin dashboard.
- **Files changed:** app/src/main/java/com/pscholer/autoplayer/data/remote/jellyfin/JellyfinRepository.kt, app/src/main/java/com/pscholer/autoplayer/SettingsActivity.kt
---

