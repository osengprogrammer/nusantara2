# ----------------------------------------------------------------------------
# Android, Kotlin, & Play Store Crashlytics Defaults
# ----------------------------------------------------------------------------
# Wajib agar stack trace di Google Play Console bisa dibaca (tidak acak-acakan)
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-renamesourcefileattribute SourceFile

# ----------------------------------------------------------------------------
# 1. TensorFlow Lite & Brain
# ----------------------------------------------------------------------------
-keep class org.tensorflow.** { *; }
-keep class java.nio.** { *; }
-dontwarn org.tensorflow.**

# ----------------------------------------------------------------------------
# 2. Google ML Kit (Face Detection)
# ----------------------------------------------------------------------------
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ----------------------------------------------------------------------------
# 3. OpenCSV (Reports)
# ----------------------------------------------------------------------------
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**

# ----------------------------------------------------------------------------
# 4. CameraX
# ----------------------------------------------------------------------------
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ----------------------------------------------------------------------------
# 5. Room Database
# ----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ----------------------------------------------------------------------------
# 6. Coroutines (Async Tasks)
# ----------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    <init>();
}

# ----------------------------------------------------------------------------
# 7. Navigation & Compose
# ----------------------------------------------------------------------------
-keepclassmembers class * extends androidx.navigation.Navigator {
    <init>(...);
}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ----------------------------------------------------------------------------
# 8. GMS & Firebase (KRUSIAL UNTUK AZURA TIME!)
# ----------------------------------------------------------------------------
-keep class com.google.android.gms.tflite.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ----------------------------------------------------------------------------
# 9. WorkManager (Untuk SyncWorker)
# ----------------------------------------------------------------------------
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class com.azuratech.azuratime.worker.** { *; }

# ----------------------------------------------------------------------------
# ----------------------------------------------------------------------------
# 10. Data Classes & Entities (Firestore, Room, & GSON)
# ----------------------------------------------------------------------------
# 🔥 FIX: Sesuaikan dengan package 'db' milikmu
-keep class com.azuratech.azuratime.data.local.** { *; }
-keepclassmembers class com.azuratech.azuratime.data.local.** { *; }

# Menjaga Room Entities agar tidak diotak-atik
-keep @androidx.room.Entity class * { *; }

# Jika kamu punya model khusus untuk response API / Firestore selain di folder db
-keep class com.azuratech.azuratime.models.** { *; }
-keepclassmembers class com.azuratech.azuratime.models.** { *; }

# ----------------------------------------------------------------------------
# 11. JNI / Native (C++ Security Guard)
# ----------------------------------------------------------------------------
# Menjaga semua class yang memiliki fungsi native
-keepclasseswithmembernames class * {
    native <methods>;
}

# Spesifik menjaga NativeSecurityVault dan ModelGuard agar tidak dihapus
-keep class com.azuratech.azuratime.ml.matcher.NativeSecurityVault {
    native <methods>;
}
-keep class com.azuratech.azuratime.utils.ModelGuard {
    native <methods>;
}

# JIKA C++ memanggil fungsi Java, fungsi Java tersebut harus dijaga:
# Gunakan anotasi @androidx.annotation.Keep di atas fungsi Java kamu.