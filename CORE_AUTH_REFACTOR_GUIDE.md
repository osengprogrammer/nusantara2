# Core Auth Module Refactoring Guide

## 📁 New Module Structure

```
:core-auth-api          [Pure Kotlin - Zero framework dependencies]
├── build.gradle.kts
└── src/main/kotlin/
    └── com/azuratech/azuratime/core/auth/api/
        ├── repository/
        │   └── AuthRepository.kt      ← Interface definition
        └── model/
            └── AuthState.kt           ← Sealed class for state

:core-auth-impl         [Android module - Firebase implementation]
├── build.gradle.kts
└── src/main/java/
    └── com/azuratech/azuratime/core/auth/impl/
        ├── di/
        │   └── AuthModule.kt          ← Hilt binding
        └── repo/
            └── AuthRepositoryImpl.kt  ← Firebase implementation

:feature-auth           [UI layer - Uses the API]
└── build.gradle.kts    ← Depends on :core-auth-api and :core-auth-impl
```

## 📦 Module Responsibilities

### :core-auth-api (Pure Kotlin)
**Purpose**: Define contracts and domain models
- ❌ No Android imports
- ❌ No Firebase imports  
- ❌ No Hilt annotations
- ✅ AuthRepository interface
- ✅ AuthState sealed class (StateFlow-based)
- ✅ Zero external framework dependencies

### :core-auth-impl (Android Library)
**Purpose**: Provide concrete implementation
- ✅ Firebase Auth integration
- ✅ Google Sign-In integration
- ✅ Room Database operations
- ✅ Hilt dependency binding
- ✅ Implements AuthRepository from :core-auth-api

### :feature-auth (Android Library)
**Purpose**: UI/Compose layer
- ✅ Compose screens (LoginScreen, WelcomeScreen, etc.)
- ✅ ViewModel using StateFlow
- ✅ Depends on BOTH :core-auth-api AND :core-auth-impl
- ✅ Uses @HiltViewModel to inject AuthRepository

## 🔧 Build Configuration Highlights

### :core-auth-api
```kotlin
plugins {
    `java-library`
    kotlin("jvm")
}
// Pure Kotlin, JAR output, no Android framework
```

### :core-auth-impl
```kotlin
plugins {
    id("com.android.library")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    api(project(":core-auth-api"))  // Expose API to consumers
    implementation("com.google.firebase:firebase-auth-ktx")
    // ... Firebase, Room, Hilt dependencies
}
```

## 🎯 Dependency Injection Strategy

### Where to place the Hilt binding? (Answered in AuthModule.kt)

**RECOMMENDED**: Place in `:core-auth-impl` (what we did)

**Why?**
1. This is a Firebase-specific implementation
2. Keeps auth module self-contained and reusable
3. :app module doesn't need to know implementation details
4. Follows Hilt conventions for feature modules

**When would you use :app instead?**
- Multiple apps with different auth providers
- A/B testing different implementations
- Mock implementation for testing without Firebase

### Binding Code
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}
```

## 🔄 Data Flow

```
User Interaction
    ↓
LoginScreen (Compose UI)
    ↓
AuthViewModel (@HiltViewModel)
    ↓
AuthRepository (injected via Hilt)
    ↓
AuthRepositoryImpl (Firebase implementation)
    ↓
Firebase Auth / Google Sign-In
    ↓
StateFlow<AuthState> updates
    ↓
UI recomposes with new state
```

## 🚀 Migration Steps

1. ✅ **Created modules**: :core-auth-api and :core-auth-impl
2. ✅ **Moved contracts**: AuthRepository interface and AuthState to API module
3. ✅ **Moved implementation**: Firebase logic to impl module
4. ✅ **Added Hilt binding**: In :core-auth-impl/di/AuthModule.kt
5. ✅ **Updated dependencies**: feature-auth now uses core-auth-* modules

### Next Steps for Your Codebase

1. **Update existing ViewModel** in :feature-auth to use new StateFlow pattern
2. **Move ViewModels** to :feature-auth if not already there
3. **Update imports** in consumer code:
   ```kotlin
   // OLD
   import com.azuratech.azuratime.features.auth.domain.repository.AuthRepository
   
   // NEW  
   import com.azuratech.azuratime.core.auth.api.repository.AuthRepository
   ```

4. **Remove old files** from :feature-auth:
   - `feature-auth/src/main/java/.../auth/domain/repository/AuthRepository.kt`
   - `feature-auth/src/main/java/.../auth/data/repo/AuthRepositoryImpl.kt`
   - `feature-auth/src/main/java/.../auth/ui/AuthState.kt`

5. **Configure Firebase** in your app:
   - Add `google-services.json` to :app module
   - Apply `com.google.gms.google-services` plugin
   - Add Firebase BOM to dependencies

## 📝 Key Differences from Old Structure

| Aspect | Old (Monolithic) | New (Modular) |
|--------|-----------------|---------------|
| Firebase imports | in :feature-auth | only in :core-auth-impl |
| AuthRepository | in :feature-auth | interface in :core-auth-api |
| AuthState | in :feature-auth | in :core-auth-api |
| Hilt binding | ? | in :core-auth-impl |
| Testability | Harder | Easier (mock interface) |
| Reusability | Low | High (core-auth-api can be shared) |

## 🧪 Testing Strategy

```kotlin
// Unit tests can mock the interface
@Mock
lateinit var authRepository: AuthRepository

@Test
fun `login success updates state`() = runTest {
    coEvery { authRepository.signInWithGoogle(any()) } returns Result.Success(false)
    
    val viewModel = AuthViewModel(authRepository)
    
    viewModel.loginWithGoogle("test-token")
    
    assertEquals(AuthState.Success("test@email.com", "uid", "USER"), viewModel.state.value)
}
```

## 📚 Additional Resources

- See `core-auth-impl/src/main/java/.../di/AuthModule.kt` for detailed binding explanation
- Check `core-auth-api/src/main/kotlin/.../model/AuthState.kt` for state management patterns
- Refer to official Hilt docs for more DI patterns