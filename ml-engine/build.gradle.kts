plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = 35
    namespace = "com.azuratech.azuratime.mlengine"

    defaultConfig {
        minSdk = 24
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    // Placeholder for ML Kit / TensorFlow / security deps later
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // CameraX
    api(libs.camerax.core)
    api(libs.camerax.camera2)
    api(libs.camerax.lifecycle)
    api(libs.camerax.view)

    // ML Kit
    implementation("com.google.mlkit:face-detection:16.1.6")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu-api:2.14.0")

    // Other Dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    api("com.google.guava:guava:31.1-android")
    api("androidx.concurrent:concurrent-futures-ktx:1.1.0")
    api("androidx.security:security-crypto:1.1.0-alpha06")
    api("com.google.zxing:core:3.5.3")

    // Compose (for UI-related ML utilities)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
}
