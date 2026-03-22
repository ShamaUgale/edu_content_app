# =====================================================
# StockMarket Tutorials - ProGuard / R8 Rules
# =====================================================

# ---- General Android Rules ----
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# Keep the application class and entry points
-keep class com.stockmarket.tutorials.StockMarketApp { *; }
-keep class com.stockmarket.tutorials.MainActivity { *; }

# ---- Firebase ----
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Firestore model classes
-keep class com.stockmarket.tutorials.data.model.** { *; }
-keepclassmembers class com.stockmarket.tutorials.data.model.** {
    <fields>;
    <init>(...);
}

# ---- Hilt / Dagger ----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-dontwarn dagger.hilt.**

# ---- Kotlin Coroutines ----
-keepnames class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ---- Jetpack Compose ----
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ---- ExoPlayer / Media3 ----
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ---- Security-sensitive: Obfuscate security module heavily ----
# The security utility classes are intentionally NOT kept, 
# allowing R8 to fully obfuscate them
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ---- Remove debug logs in release ----
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ---- Prevent reverse engineering ----
-repackageclasses 'a'
-allowaccessmodification
-overloadaggressively
-flattenpackagehierarchy

# ---- Optimization ----
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
