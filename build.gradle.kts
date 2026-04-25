// 🔥 Jurus Pamungkas di level Root
buildscript {
    dependencies {
        classpath("com.squareup:javapoet:1.13.0")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    id("com.google.firebase.appdistribution") version "4.2.0" apply false
    
    // Opsional: Tambahkan ini jika di app level masih merah
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
}