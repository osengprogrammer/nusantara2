# 🚀 Azura Engine KMP (`:azura-engine-kmp`)

Pure Kotlin Multiplatform module containing platform-agnostic business models, error handling, and core abstractions for the AzuraTime ecosystem.

## 📦 Module Structure
src/
├── commonMain/kotlin/com/azuratech/azuraengine/
│   ├── core/          # Platform-agnostic interfaces (ImageProcessor, StorageProvider)
│   ├── face/          # Face registration & enrollment result models
│   ├── media/         # Photo processing & storage models
│   ├── model/         # Generic execution & processing results
│   ├── result/        # Shared Result<T> wrapper & AppError sealed class
│   └── sync/          # CSV parsing & data synchronization models
└── androidMain/kotlin/...  # (Future) Android-specific implementations via actual

## 🧠 What Belongs Here?
| Package | Purpose | Platform Dependencies |
|---------|---------|---------------------|
| result/ | Unified error handling & async results | None |
| model/  | Generic data transfer & process outcomes | None |
| core/   | Abstract contracts for image & storage ops | None (uses expect/actual) |
| face/, media/, sync/ | Domain models for specific features | None |

## 📥 How to Use
1. Add Dependency
In your app/build.gradle.kts:
dependencies {
    implementation(project(":azura-engine-kmp"))
}

2. Import Correctly
✅ Correct: import com.azuratech.azuraengine.result.AppError
❌ Deprecated: import com.azuratech.azuratime.domain.result.AppError

## 🚧 Architectural Rules
1. commonMain must be 100% platform-agnostic. No android.*, Context, Bitmap, or Room imports allowed.
2. Use expect/actual for platform-specific behavior (e.g., file I/O, image decoding).
3. Use Cases stay in :app for now. Phase 2 will migrate pure logic use cases here.
4. Always import from com.azuratech.azuraengine.* when referencing shared types.

## 🛠️ Build & Verify
# Compile for Android
./gradlew :azura-engine-kmp:assembleDebug

# Run common tests (JVM)
./gradlew :azura-engine-kmp:jvmTest

# Quick leak check: ensure no Android APIs slipped into commonMain
find src/commonMain -name "*.kt" -exec grep -l "android\|Context\|Bitmap" {} \;

## 🗺️ Roadmap
- [ ] Add iosMain target for iOS/macOS sharing
- [ ] Migrate pure UseCases (RegisterFaceUseCase, SyncClassesUseCase, etc.) to commonMain
- [ ] Add shared unit tests for Result & AppError
- [ ] Publish to internal Maven repository for cross-project reuse

## 📌 Phase 1 Scope (Current)
- ✅ Migrated: Models, Errors, Abstract Interfaces (`core/`)
- 📍 Kept in `:app`: UseCases, DataSources, SessionManager, DI Modules
- 🔄 Why: UseCases orchestrate Android-specific data (Room, DataStore). 
   Phase 2 will abstract `LocalDataSource` interfaces to `commonMain`.
