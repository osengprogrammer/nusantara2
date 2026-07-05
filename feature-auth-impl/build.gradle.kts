plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.azuratech.azuratime.feature.auth.impl"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    // Public API that we extracted earlier
    implementation(project(":feature-auth-api"))

    // Core utilities (Result, etc.)
    api(project(":core"))

    // Dagger (for @Module, @Provides, @Singleton)
    implementation("com.google.dagger:dagger:2.44")

    // Firebase (optional – can be added later)
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Coroutines (for Flow, suspend, etc.)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
}
