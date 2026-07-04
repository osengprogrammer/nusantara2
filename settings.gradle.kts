pluginManagement {
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
include(":app")
include(":azura-engine-kmp")
include(":ml-engine")
include(":core-api")
include(":feature-attendance-core")
include(":feature-navigation")
