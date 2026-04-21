# Technology Stack

**Analysis Date:** 2026-04-17

## Languages

**Primary:**
- Kotlin 2.0.0 - Primary application language, configured in `build.gradle.kts`
- Java 17 - Compilation target via `jvmTarget` and `sourceCompatibility/targetCompatibility`

**Secondary:**
- XML - Android manifest and resource configuration

## Runtime & Android

**Environment:**
- Android SDK 35 (Pixel 9 / Android 15) - Compilation target
- Android SDK 29 (Android 10) - Minimum SDK for scoped storage and Android Auto improvements
- Android Auto (via Car App Library 1.7.0)

**Package Manager:**
- Gradle 8.7 (implied from AGP 8.7.3)
- Build tool: Android Gradle Plugin (AGP) 8.7.3
- Lockfile: `gradle-wrapper.properties` present

## Frameworks & Core Libraries

**UI & Android:**
- androidx.appcompat 1.7.0 - Backward compatibility
- androidx.activity 1.9.0 - Activity extensions
- androidx.fragment 1.8.1 - Fragment support
- Google Material Design 1.12.0 - Material UI components
- androidx.car.app 1.7.0 - Car App Library base
- androidx.car.app:app-projected 1.7.0 - Phone-side projection to car head unit (Android Auto display)

**Media Playback:**
- androidx.media3:media3-exoplayer 1.3.1 - Core ExoPlayer video/audio playback engine
- androidx.media3:media3-exoplayer-hls 1.3.1 - HLS stream support
- androidx.media3:media3-exoplayer-dash 1.3.1 - DASH stream support
- androidx.media3:media3-exoplayer-smoothstreaming 1.3.1 - Smooth Streaming support
- androidx.media3:media3-session 1.3.1 - Media session integration
- androidx.media3:media3-ui 1.3.1 - Media3 UI components
- androidx.media3:media3-common 1.3.1 - Common Media3 types
- androidx.media3:media3-datasource-okhttp 1.3.1 - OkHttp integration for stream downloads

**Lifecycle & State:**
- androidx.lifecycle:lifecycle-runtime-ktx 2.8.3 - Coroutine scope management
- androidx.lifecycle:lifecycle-viewmodel-ktx 2.8.3 - ViewModel coroutine support

**Data & Storage:**
- androidx.datastore:datastore-preferences 1.1.1 - Key-value preferences using Protocol Buffers
- androidx.documentfile:documentfile 1.0.1 - Storage Access Framework (SAF) file tree access
- androidx.core:core-ktx 1.13.1 - Kotlin extensions for core Android APIs

## Networking

**HTTP Client:**
- com.squareup.retrofit2:retrofit 2.11.0 - REST API client
- com.squareup.retrofit2:converter-gson 2.11.0 - JSON serialization via Gson
- com.squareup.okhttp3:okhttp 4.12.0 - HTTP client (underlying Retrofit)
- com.squareup.okhttp3:logging-interceptor 4.12.0 - HTTP request/response logging for debugging

## Dependency Injection

**DI Framework:**
- com.google.dagger:hilt-android 2.51.1 - Compile-time dependency injection
- com.google.dagger:hilt-android-compiler 2.51.1 - KAPT annotation processor
- Kotlin KAPT plugin - Annotation processing for Hilt

**Scope:**
- `@Singleton` bindings in `AppModule` for repositories and managers

## Asynchronous Programming

**Coroutines:**
- org.jetbrains.kotlinx:kotlinx-coroutines-android 1.8.1 - Android Main dispatcher
- org.jetbrains.kotlinx:kotlinx-coroutines-core 1.8.1 - Core suspend functions
- All network calls via `suspend fun` with Retrofit

## Image Loading

**Thumbnails & Posters:**
- io.coil-kt:coil 2.6.0 - Efficient image loading and caching library used for media thumbnails from Plex/Jellyfin

## Build Configuration

**Plugins:**
- `com.android.application` (8.7.3) - Android app build plugin
- `org.jetbrains.kotlin.android` (2.0.0) - Kotlin/Android compiler
- `com.google.dagger.hilt.android` (2.51.1) - Hilt dependency injection
- `kotlin-kapt` - Kotlin annotation processor for Hilt
- `kotlin-parcelize` - Parcel serialization for Parcelable data classes

**Build Types:**
- **Release:** Minification enabled via ProGuard (proguard-rules.pro)
- **Debug:** Debuggable, app ID suffix `.debug`

**Build Features:**
- View Binding enabled (`viewBinding = true`) for type-safe view references

## Platform Requirements

**Development:**
- Java 17 JDK (for `jvmTarget` compilation)
- Android SDK 35 installed
- Gradle 8.7+

**Production:**
- Android 10 (API 29) minimum for:
  - Scoped storage (MediaStore.VOLUME_EXTERNAL)
  - Android Auto improvements
- Android 15 (API 35) target for latest Android guidelines

## Configuration Files

**Build:**
- `build.gradle.kts` - Root build configuration (plugins)
- `app/build.gradle.kts` - App module configuration (dependencies, Android config)
- `gradle-wrapper.properties` - Gradle version management

**Android:**
- `app/src/main/AndroidManifest.xml` - App manifest, permissions, services
- `app/src/main/res/` - Resources (layouts, strings, styles)
- `app/proguard-rules.pro` - ProGuard obfuscation rules (release builds)

**Network Security:**
- `app/src/main/res/xml/network_security_config` - Certificate pinning and cleartext traffic policy (referenced in AndroidManifest.xml)

---

*Stack analysis: 2026-04-17*
