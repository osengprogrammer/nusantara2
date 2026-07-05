pluginManagement {
    // -----------------------------------------------------------------
    // Central plugin versions – all sub‑projects can use the short id
    // -----------------------------------------------------------------
    plugins {
        // Android Gradle plugin (provides the com.android.application &
        // com.android.library plugins)
        id("com.android.application") version "7.4.2" apply false
        id("com.android.library") version "7.4.2" apply false

        // Kotlin Android plugin (adds kotlin("android") support)
        id("org.jetbrains.kotlin.android") version "1.9.20" apply false

        // Kotlin serialization plugin (used by many modules)
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20" apply false

        // Google Services plugin – needed for google-services.xml processing
        id("com.google.gms.google-services") version "4.4.2" apply false

        // Google DevTools KSP plugin
        id("com.google.devtools.ksp") version "1.9.20" apply false

        // Dagger Hilt plugin (must match the version used in the app module)
        id("com.google.dagger.hilt.android") version "2.48.1" apply false
    }

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "azuratime"
include(":feature-auth-api")
include(":core")
include(":feature-auth-impl")
