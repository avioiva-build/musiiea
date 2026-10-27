# AVIOIVA Player R8 rules.
#
# Media3 (ExoPlayer), Jetpack Compose and androidx libraries ship their own
# consumer ProGuard/R8 rules inside their AARs, so very little is needed here.

# Keep line numbers in crash traces readable.
-keepattributes SourceFile,LineNumberTable

# kotlinx-coroutines debug metadata (harmless, improves stack traces)
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
