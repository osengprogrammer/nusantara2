# Modularization Roadmap for Auth, Sync, and User Features

**Goal:** Extract the Auth, Sync, and User features into their own modules while preserving a clean dependency graph, avoiding circular dependencies, and keeping the app buildable at every step.

---

## Table of Contents
1. [Foundations (Shared Core)](#foundations-shared-core)  
2. [Phase 1 – API Extraction](#phase‑1--api-extraction)  
3. [Phase 2 – Implementation Extraction](#phase‑2--implementation-extraction)  
4. [Phase 3 – Dependency Inversion & Wiring](#phase‑3--dependency-inversion--wiring)  
5. [Safety Order – Absolute Minimum‑Risk Sequence](#safety-order--absolute-minimum‑risk-sequence)  
6. [Road‑Map Timeline (8 weeks)](#road‑map-timeline-8‑weeks)  
7. [Checklist Before Moving to the Next Phase](#checklist-before-moving-to-the-next-phase)  
8. [Quick Reference Summary](#quick-reference-summary)  

---

### Foundations (Shared Core)

| Module | Purpose | Key Artifacts |
|--------|---------|---------------|
| `:core` | Common utilities, constants, app‑wide extensions. | `Result<T>`, `Either`, `UiState`, `AppScope`, `SharedPreferencesHelper`, `TypeConverterFactory`. |
| `:core:di` | Hilt module definitions that expose **interfaces**, not concrete implementations. | `AuthApiModule`, `SyncApiModule`, `UserRepositoryModule`. |
| `:core:data` | Shared data‑layer abstractions (DTOs, serializers, TypeConverters). | `NetworkResponse<T>`, `PagingConfig`, `AppDatabase`. |
| `:app` | Entry point, navigation host, and **wiring** – only consumes abstractions from `:core`. | `NavHost`, UI composition, ViewModel bindings. |

> **Safety tip:** Keep `:app` → `:core` **only**. No other module may depend on `:app`. This guarantees no circular reference can appear at the Gradle‑level.

---

### Phase 1 – API Extraction

Create three **feature‑api** modules that expose *only* the public contracts needed by UI and domain layers. These modules contain **no implementation** – just interfaces, data classes, sealed‑class states, and request/response DTOs.

| Module | Content | Why it belongs here? |
|--------|---------|----------------------|
| `:feature-auth-api` | - `AuthService` (Retrofit) <br/> - `LoginRequest / LoginResponse` <br/> - `AuthStatus` sealed interface & enum <br/> - `AuthRepository` **interface** <br/> - `AuthState` data class | UI talks only to an **Auth API**; implementation can be swapped later. |
| `:feature-sync-api` | - `SyncService` (Retrofit) <br/> - `SyncRequest / SyncResponse` <br/> - `SyncEvent` sealed interface (e.g., `DataUpdated`, `Conflict`) <br/> - `SyncScheduler` interface (wrapper around WorkManager) | Encapsulates everything that synchronises data with the backend. |
| `:feature-user-api` | - `UserRepository` interface (CRUD on Room) <br/> - `User` data class / `UserProfile` sealed interface <br/> - `UpdateUserUseCase` (domain use‑case) | Keeps domain logic independent of any concrete Room or Firebase backend. |

**Gradle setup (example):**

```kotlin
// Root settings.gradle.kts
include(
    ":app",
    ":core",
    ":core:di",
    ":core:data",
    ":feature-auth-api",
    ":feature-sync-api",
    ":feature-user-api",
    ":feature-auth-impl",
    ":feature-sync-impl",
    ":feature-user-impl"
)
```

**Safety:** No binary dependency on other feature modules; only `:core` may depend on them (to access contracts). This guarantees that moving to the next phase cannot break the build.

---

### Phase 2 – Implementation Extraction

Now create concrete implementations that **realise** the API contracts. Place them under `:feature-auth-impl`, `:feature-sync-impl`, `:feature-user-impl`.

| Impl Module | Allowed External Dependencies |
|-------------|--------------------------------|
| `:feature-auth-impl` | Retrofit, Gson/Moshi, Kotlin Coroutines, Hilt, `:core:data`. |
| `:feature-sync-impl` | Retrofit, WorkManager, Hilt, `:core:data`. |
| `:feature-user-impl` | Room, Hilt, `:core:data`. |

#### Example: Auth Implementation

```kotlin
// :feature-auth-impl/src/main/java/com/azuratech/azuratime/domain/auth/AuthRepositoryImpl.kt
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthService,
    private val preferences: SharedPreferences
) : AuthRepository {
    override suspend fun login(...): Result<AuthStatus> { … }
}
```

**Hilt module in the impl:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AuthImplModule {
    @Provides @Singleton fun provideAuthService(): AuthService = Retrofit.Builder()
        .baseUrl(BuildConfig.AUTH_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(AuthService::class.java)

    @Provides fun provideAuthRepository(
        api: AuthService,
        prefs: SharedPreferences
    ): AuthRepository = AuthRepositoryImpl(api, prefs)
}
```

**Gradle linking:**  
`implementation project(":feature-auth-impl")` **only** in `:app` (or in `:core:di` if you prefer centralising the binding). All other modules must **not** depend on `:feature‑*‑impl`; they can only depend on the corresponding `:feature‑*‑api`.

**Safety check:** After adding each impl module, run `./gradlew assembleDebug`. If the build succeeds, you can move on to the next step.

---

### Phase 3 – Dependency Inversion & Wiring in `:app`

Now the **:app** module becomes a thin “composition root”. It only knows about **interfaces** from the API modules and **provides** concrete implementations from the impl modules via Hilt.

#### 3.1 Update Hilt Modules

1. **Remove concrete classes** from `:core:di`. They should now expose only **@Provides** methods that pull implementations from the feature‑impl modules (or let the impl modules install their own Hilt modules automatically).

2. **Create a “FeatureWiring” module** in `:app` (or keep it in `:core:di`) that aggregates all provider methods:

```kotlin
// :app/src/main/java/com/azuratech/azuratime/di/FeatureWiringModule.kt
@Module
@InstallIn(SingletonComponent::class)
object FeatureWiringModule {

    @Provides @Singleton fun provideAuthRepository(
        impl: com.azuratech.azuratime.feature.auth.presentation.AuthRepositoryImpl
    ) = impl

    @Provides @Singleton fun provideSyncScheduler(
        workerFactory: WorkerFactory
    ): SyncScheduler = SyncSchedulerImpl(workerFactory)

    // … similarly for UserRepository, etc.
}
```

3. **Make the impl modules auto‑install** their Hilt components:

```kotlin
// In :feature-auth-impl/src/main/java/com/azuratech/azuratime/feature/auth/di/AuthImplModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AuthImplModule {
    @Provides @Singleton fun provideAuthService(...): AuthService = …
    @Provides @Singleton fun provideAuthRepository(
        api: AuthService,
        prefs: SharedPreferences
    ): AuthRepository = AuthRepositoryImpl(api, prefs)
}
```

**Result:** `:app` only declares a single `@Module` that pulls everything together. **No feature module depends on another feature module**, eliminating circular‑dependency risk.

#### 3.2 Navigation & ViewModel Refactoring

- **LoginScreen** now receives its `AuthRepository` via constructor injection (or via a `SavedStateHandle`‑backed ViewModel).  
- The navigation lambda (`onNavigateToDashboard`) stays unchanged because it only calls `navController.navigate`.  
- Remove the hard‑coded `webClientId` usage (already fixed). If still needed, supply it through DI.

#### 3.3 Testing

- Write **unit tests** for each repository interface using **mock** implementations of the API contracts.  
- Mock implementations can be provided by a `TestAndroidModule` that lives in `:core:di` and supplies **in‑memory** or **fake** versions of the impl modules.  
- Run `./gradlew testDebugUnitTest` after each incremental change to guarantee no regression.

---

## Safety Order – Absolute Minimum‑Risk Sequence

| Step | Action | Reason |
|------|--------|--------|
| **0** | Ensure `:core` compiles and passes all tests (including lint). | Baseline stability. |
| **1** | Create the three **API** modules (`:feature‑auth‑api`, `:feature‑sync‑api`, `:feature‑user‑api`). Add them as dependents of `:core` only. | No code moved yet – just contracts. |
| **2** | Add **empty** implementations in `:feature‑auth‑impl`, `:feature‑sync‑impl`, `:feature‑user‑impl` (just `@Inject constructor() {}`) and publish them. Run a full build. | Verifies that the Gradle graph has no circular reference. |
| **3** | Move **one** concrete implementation at a time (e.g., start with `AuthRepositoryImpl`). Update its Hilt module to provide the implementation. Re‑build and run unit tests. | First feature to go live; minimal impact. |
| **4** | Wire the newly‑available implementation in `:app` DI. Run UI‑test sanity check (`./gradlew connectedDebugAndroidTest`). | Confirms that the app can still start. |
| **5** | Repeat steps 3‑4 for **Sync** and **User** implementations, one after the other. | Each step isolates a single feature change. |
| **6** | Once all three impl modules are wired, **delete** the old monolithic code paths (e.g., the large `LoginScreen` block that referenced `webClientId`). Keep the CI green after each deletion. | Guarantees you never have a half‑migrated feature. |
| **7** | Refactor navigation and ViewModels to use only the abstract APIs. Run full instrumentation suite. | Final sanity check before enabling any new feature. |

> **Key safety net:** *Never* delete or rename a file before an incremental build succeeds with the new code in place. Use Git feature branches for each feature‑module migration; merge only when the CI build passes.

---

## Road‑Map Timeline (8 weeks)

| Week | Milestone |
|------|-----------|
| **1** | Scaffold `:feature‑auth‑api`, `:feature‑sync‑api`, `:feature‑user‑api`. Add dummy interfaces. Verify build. |
| **2** | Implement **Auth** API contracts, add Hilt providers in `:feature‑auth‑impl`. Wire in `:app`. Run UI sanity test. |
| **3** | Implement **Sync** API & impl, expose via Hilt. Merge a **feature‑branch** where only Sync works. |
| **4** | Implement **User** API & impl, expose via Hilt. Merge a branch where all three features compile. |
| **5** | Delete the old monolithic Auth/Sync/User code (e.g., remove `webClientId` block). Refactor navigation to use abstract repositories. |
| **6** | Add **unit‑test** coverage for each repository using fake implementations. |
| **7** | Run full CI pipeline (lint, unit, instrumented) on `main`. Fix any failing builds. |
| **8** | Tag the release `modularisation‑v1` and merge to `main`. Prepare for future feature additions (e.g., `:feature‑offline‑sync`). |

---

## Checklist Before Moving to the Next Phase

- [ ] Build succeeds (`./gradlew assembleDebug`).  
- [ ] All **unit** tests pass (`./gradlew testDebugUnitTest`).  
- [ ] No **circular** Gradle dependencies (`./gradlew :app:dependencies --configuration compileClasspath`).  
- [ ] Hilt **graph** validates (`./gradlew :app:dependencies --configuration kapt`).  
- [ ] Lint reports **0 errors** (`./gradlew lintDebug`).  
- [ ] Run a quick **smoke‑test** on a device/emulator (`./gradlew installDebug`).  

---

## Quick Reference Summary

| Module | Phase 1 (API) | Phase 2 (Impl) | Phase 3 (DI) |
|--------|----------------|----------------|--------------|
| `Auth` | `AuthApi`, `AuthRepository` interface | `AuthRepositoryImpl` (Retrofit + Room) | `@Provides AuthRepository` in `:app` DI |
| `Sync` | `SyncService`, `SyncScheduler` interface | `SyncWorker` + `SyncRepositoryImpl` | `@Provides SyncScheduler` in `:app` DI |
| `User` | `UserRepository`, `User` data class | `UserDao`, `UserRepositoryImpl` (Room) | `@Provides UserRepository` in `:app` DI |

**Safety order:** API modules → empty impl modules → one concrete impl → wire → delete old code → repeat.

---

### 🎯 Bottom line
By **extracting contracts first**, then **isolating implementations**, and finally **centralising wiring in the `:app` DI layer**, you guarantee:

1. **No circular dependencies** at compile‑time.  
2. **Build‑always‑green** checkpoints after each incremental change.  
3. **Clear ownership** of each feature’s dependencies (Firebase, Room, WorkManager) inside its own impl module.  
4. **Testable, interchangeable** backends (swap Retrofit for MockWebServer or Room for H2 without touching UI).

Follow the phased roadmap above, and you’ll be able to migrate `Auth`, `Sync`, and `User` into modular, reusable libraries while keeping the project buildable at every step. Happy refactoring! 🚀