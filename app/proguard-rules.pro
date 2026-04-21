# Keep Car App Library
-keep class androidx.car.app.** { *; }

# Keep Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-keep class com.google.android.exoplayer2.** { *; }

# Keep Retrofit model classes (Gson serialization)
-keepclassmembers class com.pscholer.autoplayer.data.remote.** { *; }
-keep class com.pscholer.autoplayer.data.remote.** { *; }

# Keep Gson annotations
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }
