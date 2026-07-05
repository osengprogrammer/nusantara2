import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val keyProperties = Properties()
val keyPropertiesFile = rootProject.file("key.properties")
if (keyPropertiesFile.exists()) {
    keyProperties.load(FileInputStream(keyPropertiesFile))
}

val geminiApiKey: String = keyProperties.getProperty("GEMINI_API_KEY") ?: localProperties.getProperty("GEMINI_API_KEY") ?: ""
val mapsApiKey: String = keyProperties.getProperty("MAPS_API_KEY") ?: localProperties.getProperty("MAPS_API_KEY") ?: ""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    id("com.google.dagger.hilt.android") version "2.48.1"
    id("com.google.gms.google-services")
    kotlin("plugin.serialization")
    id("com.google.firebase.appdistribution")
}

android {
    namespace = "com.azuratech.azuratime"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.azuratech.azuratime"
        minSdk = 24
        targetSdk = 35
        versionCode = 3735
        versionName = "3.7.13"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        buildConfigField("boolean", "ENABLE_SUBJECT_SESSION", "true")
        buildConfigField("boolean", "ENABLE_BIOMETRIC_FALLBACK", "false")
        manifestPlaceholders["MAPS_API_KEY"] = project.findProperty("MAPS_API_KEY") ?: keyProperties.getProperty("MAPS_API_KEY") ?: localProperties.getProperty("MAPS_API_KEY") ?: ""

        externalNativeBuild {
            cmake { cppFlags += "-std=c++17" }
        }
    }

    flavorDimensions += "appType"
    productFlavors {
        create("schoolAttendance") {
            dimension = "appType"
            applicationId = "com.azuratech.azuratime.school"
            versionNameSuffix = "-school"
            manifestPlaceholders["appName"] = "AzuraTime School"
        }
        create("officeAttendance") {
            dimension = "appType"
            applicationId = "com.azuratech.azuratime.office"
            versionNameSuffix = "-office"
            manifestPlaceholders["appName"] = "AzuraTime Office"
        }
    }

    signingConfigs {
        create("release") {
            val storePath = keyProperties.getProperty("storeFile")
            if (!storePath.isNullOrEmpty()) {
                storeFile = file(storePath)
            } else {
                storeFile = file("azura-key.jks")
            }
            storePassword = keyProperties.getProperty("storePassword") ?: System.getenv("RELEASE_STORE_PASSWORD") ?: localProperties.getProperty("RELEASE_STORE_PASSWORD") ?: ""
            keyAlias = keyProperties.getProperty("keyAlias") ?: System.getenv("RELEASE_KEY_ALIAS") ?: localProperties.getProperty("RELEASE_KEY_ALIAS") ?: ""
            keyPassword = keyProperties.getProperty("keyPassword") ?: System.getenv("RELEASE_KEY_PASSWORD") ?: localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            firebaseAppDistribution {
                releaseNotes = "Azura Time - Latest Stable Build"
                testers = "osengprogrammer@gmail.com"
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    buildFeatures {
        compose = true
        mlModelBinding = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions { jvmTarget = "17" }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs { useLegacyPackaging = true }
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

    lint {
        disable += "NullSafeMutableLiveData"
        checkReleaseBuilds = false
        abortOnError = false
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        freeCompilerArgs += "-opt-in=kotlinx.coroutines.FlowPreview"
    }
}

dependencies {
    implementation(project(":azura-engine-kmp"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.material.icons.extended)
    implementation(libs.accompanist.permissions)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("io.coil-kt:coil-compose:2.5.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-appdistribution:16.0.0-beta19")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.firebase:firebase-messaging-ktx:23.4.0")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-common-ktx")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    
    implementation(project(":ml-engine"))
    implementation(project(":feature-attendance-core"))
    implementation(project(":feature-navigation"))
    implementation(project(":core-designsystem"))


    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.google.dagger:hilt-android:2.48.1")
    ksp("com.google.dagger:hilt-android-compiler:2.48.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")
    implementation("com.google.firebase:firebase-functions-ktx:20.4.0")
    testImplementation(libs.junit)
    testImplementation("com.tngtech.archunit:archunit-junit4:1.3.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.work:work-testing:2.9.0")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    kspTest("com.google.dagger:hilt-android-compiler:2.48.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.register<JavaExec>("encryptModel") {
    group = "application"
    description = "Encrypts the standard .tflite model using ModelEncryptor"
    
    // Ensure the library is compiled before the task runs
    dependsOn(":ml-engine:compileDebugKotlin")

    // Build the classpath from the library output + runtime classpath
    val compileLib = project(":ml-engine")
        .tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileDebugKotlin")
    
    mainClass.set("com.azuratech.azuratime.core.security.ModelEncryptorKt")
    workingDir = project.rootDir
    
    classpath = files(
        compileLib.map { it.destinationDirectory },
        project.configurations.detachedConfiguration(
            project.dependencies.create("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
        )
    )
}

