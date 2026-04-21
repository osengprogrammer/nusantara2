# ============================================================================
# AZURA TIME — PROGUARD/R8 RULES (RELEASE BUILD)
# ============================================================================
# Purpose: Keep required classes while allowing R8 to shrink/obfuscate safely
# Stack: Android + Kotlin + Hilt + Firebase + Room + TFLite + Compose + ML Kit
# ============================================================================

# ----------------------------------------------------------------------------
# 0. ANDROID, KOTLIN & PLAY STORE DEFAULTS
# ----------------------------------------------------------------------------
# Preserve stack traces for Google Play Console crash reports
-keepattributes *Annotation*, SourceFile, LineNumberTable
-keep public class * extends java.lang.Exception
-renamesourcefileattribute SourceFile

# Keep Kotlin metadata for reflection (if used)
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# ----------------------------------------------------------------------------
# 1. TENSORFLOW LITE & BRAINFACE ENGINE
# ----------------------------------------------------------------------------
-keep class org.tensorflow.** { *; }
-keep class java.nio.** { *; }
-dontwarn org.tensorflow.**

# Keep native TFLite delegates (GPU, NNAPI)
-keep class org.tensorflow.lite.experimental.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }

# ----------------------------------------------------------------------------
# 2. GOOGLE ML KIT (FACE DETECTION, BARCODE SCANNING)
# ----------------------------------------------------------------------------
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep ML Kit model bindings
-keep class com.google.android.gms.mlkit.** { *; }

# ----------------------------------------------------------------------------
# 3. OPENCSV (REPORT EXPORT)
# ----------------------------------------------------------------------------
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**

# ----------------------------------------------------------------------------
# 4. CAMERAX (CAMERA PREVIEW & CAPTURE)
# ----------------------------------------------------------------------------
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Keep CameraX use-case bindings
-keep class * implements androidx.camera.core.UseCase { *; }

# ----------------------------------------------------------------------------
# 5. ROOM DATABASE (LOCAL STORAGE)
# ----------------------------------------------------------------------------
# Keep Room database class and all DAOs
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.Dao { *; }

# Keep Room entities with annotations
-keep @androidx.room.Entity class * {
    *;
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
    @androidx.room.Embedded *;
    @androidx.room.Relation *;
    @androidx.room.Index *;
}

# Keep TypeConverters
-keep class * implements androidx.room.TypeConverter { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

# Prevent warnings on optional Room paging module
-dontwarn androidx.room.paging.**

# ----------------------------------------------------------------------------
# 6. KOTLIN COROUTINES (ASYNC FLOWS)
# ----------------------------------------------------------------------------
# Keep coroutine dispatchers and exception handlers
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    <init>();
}

# Keep suspend function metadata
-keepattributes RuntimeVisibleAnnotations

# ----------------------------------------------------------------------------
# 7. NAVIGATION & JETPACK COMPOSE
# ----------------------------------------------------------------------------
# Keep Navigation components
-keepclassmembers class * extends androidx.navigation.Navigator {
    <init>(...);
}

# Keep Compose composables and modifiers
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.ui.Modifier *;
}

# Keep Compose stability annotations
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keepattributes *Annotation*

# ----------------------------------------------------------------------------
# 8. FIREBASE & GOOGLE PLAY SERVICES (CRITICAL)
# ----------------------------------------------------------------------------
# Keep all Firebase classes (Firestore, Auth, Storage, Messaging, Functions)
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.android.gms.tflite.** { *; }
-dontwarn com.google.firebase.**

# Preserve generic type signatures for Firestore deserialization
-keepattributes Signature, EnclosingMethod

# Keep Google Auth components
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }

# ----------------------------------------------------------------------------
# 9. WORKMANAGER (BACKGROUND SYNC — SyncWorker)
# ----------------------------------------------------------------------------
# Keep Worker subclasses and factory
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class com.azuratech.azuratime.worker.** { *; }

# Keep WorkRequest builders
-keep class androidx.work.** { *; }
-dontwarn androidx.work.impl.utils.futures.**

# ----------------------------------------------------------------------------
# 10. HILT & DAGGER (CRITICAL — PREVENTS RELEASE CRASHES)
# ----------------------------------------------------------------------------
# Keep Hilt-generated components, modules, factories
-keep class *Hilt* { *; }
-keep class *HiltComponents* { *; }
-keep class *HiltActivity* { *; }
-keep class *HiltFragment* { *; }
-keep class *HiltViewModel* { *; }
-keep class *HiltWorker* { *; }
-keep class *_Factory { *; }
-keep class *_Impl { *; }
-keep class *_Component { *; }
-keep class *_Module { *; }

# Keep Dagger internal runtime classes
-keep class dagger.internal.** { *; }
-keep class dagger.hilt.internal.** { *; }
-keep class dagger.hilt.android.internal.** { *; }
-keep class dagger.hilt.android.lifecycle.** { *; }
-keep class dagger.hilt.android.work.** { *; }

# Keep @Inject, @Provides, @Binds annotated members
-keepclassmembers class * {
    @javax.inject.Inject *;
    @dagger.Provides *;
    @dagger.Binds *;
    @dagger.multibindings.* *;
}

# Keep Hilt Worker factory integration
-keep class * extends androidx.hilt.work.HiltWorkerFactory { *; }
-keep class androidx.hilt.work.HiltWorkerFactory

# Keep ViewModel injection helpers
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ----------------------------------------------------------------------------
# 11. KOTLIN SERIALIZATION (JSON PARSING)
# ----------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class kotlinx.serialization.** { *; }
-keep class * implements kotlinx.serialization.KSerializer { *; }

# Keep your serializable model classes (adjust package if needed)
-keep class com.azuratech.azuratime.data.model.** { *; }
-keep class com.azuratech.azuratime.domain.model.** { *; }
-keepclassmembers class com.azuratech.azuratime.data.model.** {
    *** get*();
    void set*(***);
    *** is*();
}

# ----------------------------------------------------------------------------
# 12. DATA CLASSES & ENTITIES (FIRESTORE, ROOM, GSON)
# ----------------------------------------------------------------------------
# Keep local data layer classes (Room entities, DAOs, converters)
-keep class com.azuratech.azuratime.data.local.** { *; }
-keepclassmembers class com.azuratech.azuratime.data.local.** { *; }

# Keep remote data layer classes (Firestore models, mappers)
-keep class com.azuratech.azuratime.data.remote.** { *; }
-keepclassmembers class com.azuratech.azuratime.data.remote.** { *; }

# Keep repository implementations (if referenced via reflection)
-keep class com.azuratech.azuratime.data.repository.** { *; }

# Keep domain models used across layers
-keep class com.azuratech.azuratime.domain.model.** { *; }

# ----------------------------------------------------------------------------
# 13. NATIVE / JNI / C++ SECURITY GUARD (AZURA ENGINE)
# ----------------------------------------------------------------------------
# Keep all classes with native methods (called from C++)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep specific native-binding classes
-keep class com.azuratech.azuratime.ml.matcher.NativeSecurityVault {
    native <methods>;
}
-keep class com.azuratech.azuratime.utils.ModelGuard {
    native <methods>;
}
-keep class com.azuratech.azuratime.ml.** {
    native <methods>;
}

# If C++ calls Java methods, keep those Java methods with @Keep annotation:
# @androidx.annotation.Keep
# public void onNativeCallback(String result) { ... }

# ----------------------------------------------------------------------------
# 14. OPTIONAL OPTIMIZATIONS (REDUCE APK SIZE)
# ----------------------------------------------------------------------------
# Strip debug log calls from release build (safe if you use Log.d/v/i only for dev)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);  # Optional: keep warnings/errors
    public static *** e(...);  # Optional: keep errors for crash reporting
}

# Remove test-only dependencies from release
-dontwarn com.azuratech.azuratime.test.**
-dontwarn io.mockk.**
-dontwarn org.junit.**
-dontwarn androidx.test.**

# Remove unused Firebase modules (if not used in release)
# -dontwarn com.google.firebase.analytics.**
# -dontwarn com.google.firebase.crashlytics.**
# -dontwarn com.google.firebase.perf.**

# ----------------------------------------------------------------------------
# 15. FINAL SAFEGUARDS
# ----------------------------------------------------------------------------
# Keep your application class (if it has @HiltAndroidApp)
-keep class com.azuratech.azuratime.Hilt_Application { *; }
-keep class com.azuratech.azuratime.MainApp { *; }

# Keep MainActivity and other entry points
-keep class com.azuratech.azuratime.MainActivity { *; }
-keepclassmembers class com.azuratech.azuratime.MainActivity {
    <init>();
}

# Keep all classes in your core packages (adjust if too broad)
# -keep class com.azuratech.azuratime.core.** { *; }

# ============================================================================
# END OF PROGUARD RULES
# ============================================================================