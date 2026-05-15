# 🏗️ AzuraTime Architecture Guide

## 🎯 Philosophy & Principles
AzuraTime is built on a **Vertical Slice Architecture** (Feature-First), designed for maximum modularity, testability, and AI-assisted development.

*   **Feature-First Isolation**: Each feature contains its own UI, Domain, and Data layers. Cross-feature dependencies are strictly managed through `core` modules.
*   **Local-First SSOT**: The local Room database is the Single Source of Truth. Remote synchronization (Firebase) is treated as a side-effect.
*   **Explicit Over Implicit**: Package names and file suffixes explicitly state their purpose (e.g., `StudentRepositoryImpl`, `BiometricScreen`).
*   **AI-Readable by Design**: The structure is optimized for LLMs to navigate and understand with minimal context, using consistent patterns across all slices.

---

## 📁 Project Structure
The codebase is divided into two primary root packages under `com.azuratech.azuratime`:

```text
app/src/main/java/com/azuratech/azuratime/
├── core/               # 🏗️ Shared Infrastructure (The Foundation)
│   ├── boot/           # App startup and initialization logic
│   ├── data/           # Core data sources (AppDatabase, SchoolRepo)
│   ├── di/             # Hilt modules (AppModule, RepositoryModule)
│   ├── domain/         # Shared models (SyncStatus) and utils (Media, Sync)
│   ├── navigation/     # NavHost and global Screen routes
│   ├── security/       # Encryption and security vault
│   ├── session/        # User session management
│   ├── sync/           # WorkManager sync workers
│   └── ui/             # Root screens and shared UI utilities
├── features/           # 🧩 Business Capabilities (Vertical Slices)
│   ├── ai/             # Zohar AI Assistant
│   ├── attendance/     # Capture, Barcode, History, Manual
│   ├── auth/           # Login and Authentication state
│   ├── biometric/      # Face Enrollment and Matching
│   ├── dashboard/      # Teacher Landing Page
│   ├── reporting/      # Matrix, Audit Logs, Export, Integrity
│   ├── staff/          # Profile, Membership, School Admin
│   └── student/        # Roster, Forms, Class Management
└── navigation/         # 🗺️ Navigation Constants (NavigationRoutes)
```

---

## 🏷️ Naming Conventions

| Component | Suffix | Example | Location | Rule |
| :--- | :--- | :--- | :--- | :--- |
| **UI Screen** | `Screen` | `StudentRosterScreen.kt` | `features/*/ui/` | Composable function entry point. |
| **ViewModel** | `ViewModel` | `StudentFormViewModel.kt` | `features/*/ui/` | Handles UI state and repository calls. |
| **UI State** | `UiState` | `DashboardUiState.kt` | `features/*/ui/` | Data class representing the screen state. |
| **Entity** | `Entity` | `StudentEntity.kt` | `features/*/data/local/` | Room database table definition. |
| **DAO** | `Dao` | `StudentDao.kt` | `features/*/data/local/` | Room database access interface. |
| **Repository** | `Repository` | `StudentRepository.kt` | `features/*/domain/repository/` | Domain layer interface. |
| **Implementation**| `Impl` | `StudentRepositoryImpl.kt` | `features/*/data/repo/` | Data layer implementation. |

---

## 🧩 Vertical Slice Template
When creating a new feature (e.g., `library`), follow this structure:

```text
features/library/
├── data/
│   ├── local/          # LibraryEntity, LibraryDao
│   ├── remote/         # Optional: Firebase DataSources
│   └── repo/           # LibraryRepositoryImpl
├── domain/
│   ├── model/          # LibraryProfile (UI-friendly model)
│   └── repository/     # LibraryRepository (Interface)
└── ui/
    ├── components/     # Feature-specific widgets
    ├── LibraryScreen.kt
    ├── LibraryViewModel.kt
    └── LibraryUiState.kt
```

---

## 🛠️ Core Infrastructure

### 🗡️ Hilt Dependency Injection
DI is centralized in `core/di/` but scoped to feature repositories.

*   **`AppModule`**: Provides `AppDatabase` and DAOs.
*   **`RepositoryModule`**: Binds feature interfaces to implementations using `@Binds`.
*   **`DataSourceModule`**: Binds local/remote data sources.

**Example: Binding a new Repository**
```kotlin
// core/di/RepositoryModule.kt
@Binds
@Singleton
abstract fun bindLibraryRepository(
    impl: LibraryRepositoryImpl
): LibraryRepository
```

### 🗺️ Navigation
Navigation is organized into **Feature Graphs** in `ui/core/navigation/graphs/`.

1.  Define route in `navigation/NavigationRoutes.kt`.
2.  Add graph in `ui/core/navigation/graphs/`.
3.  Link graph to `NavHost` in `MainActivity.kt`.

### 🔐 Signup & Account Provisioning Flow (Firebase + Local)
To maintain security and proper auto-generation of database seeds, the user registration flow strictly follows a **Cloud Function Driven** model:
1. **App Side (Signup):** When a new user signs in via Google, `MembershipRepository` writes a temporary stub to local Room (`STATUS_PENDING`) and pushes a document to the **`memberships/{uid}`** collection in Firestore. It **does not** push to `whitelisted_users`.
2. **Admin Action:** The Super Admin manually changes the status in `memberships/{uid}` to `"ACTIVE"` via the Firebase Console.
3. **Cloud Function:** The `onregistrationapproved` Firebase Cloud Function (located in `functions/src/index.ts`) listens to `memberships/{uid}`. When changed to `ACTIVE`, the function:
   - Generates the `dbSeed` and `secureIsoKey` using the `hardwareId`.
   - Creates the default `School` document.
   - Creates the user in the **`whitelisted_users`** collection.
   - Deletes the temporary document in `memberships`.
4. **App Side (Resolution):** `MembershipViewModel` calls `syncUser()` which pulls the newly created `whitelisted_users` document, updates the local database to `ACTIVE`, and navigates the user to the Dashboard.

---

## 🚀 How to Add a New Feature
1.  **Define Routes**: Add a constant to `NavigationRoutes.kt`.
2.  **Create Package**: Create `features/newfeature/` and sub-packages.
3.  **Implement Data Layer**: Create Entity, Dao, and Repository implementation.
4.  **Register Dao**: Add to `AppDatabase.kt` and `AppModule.kt`.
5.  **Implement Domain Layer**: Create Repository interface and Domain models.
6.  **Bind Repository**: Add to `RepositoryModule.kt`.
7.  **Implement UI Layer**: Create UiState, ViewModel, and Screen.
8.  **Add NavGraph**: Create `NewFeatureGraph.kt` and link it.

---

## 🧪 Testing Guidelines

### ViewModel Unit Test
Test the UI state flow and interaction with repositories.
```kotlin
@Test
fun `when loading student roster, state updates to Success`() = runTest {
    coEvery { repository.getStudents() } returns flowOf(listOf(mockStudent))
    viewModel.loadRoster()
    assertEquals(UiState.Success(listOf(mockStudent)), viewModel.state.value)
}
```

### Repository Test
Verify Room transactions and mapping logic.
```kotlin
@Test
fun `saveProfile persists entity to database`() = runTest {
    repository.saveProfile(mockProfile)
    verify { dao.insert(any()) }
}
```

---

## 🔧 Troubleshooting

*   **KSP Errors**: If `AppDatabase_Impl` fails to generate, check if the new DAO is properly imported in `AppDatabase.kt`.
*   **@Binds Fixes**: Ensure the implementation class has `@Inject constructor(...)` and is a subclass of the interface.
*   **Package Sync**: If you move a file, run a global search for the old package name.
*   **Stale Imports**: If a `@Provides` method fails, verify that the return type matches the expected interface exactly.

---

## 🤖 AI-Assisted Development Tips
*   **Constraint**: "Always use `features/` or `core/` package prefixes. Never use legacy `ui/`, `data/`, or `domain/` roots."
*   **Prompt Template**: "Create a new vertical slice for [FeatureName]. Follow the AzuraTime ARCHITECTURE.md patterns: UI -> Domain Interface -> Data Impl -> Room SSOT."
*   **Avoid**: Do not let the AI create "Utility" classes for feature-specific logic. Put it in the feature's `domain/` or `data/` package.

---

## 📈 Quality Gates
1.  **Package Compliance**: Is the file in the correct feature/core slice?
2.  **Naming Consistency**: Does it follow the `*Screen`, `*ViewModel`, `*Repository` naming?
3.  **SSOT Check**: Does the UI observe data from a Repository?
4.  **DI Integrity**: Are all dependencies injected via Hilt? No `new Repository()` calls.

---

### Version History
| Version | Date | Description | Contributor |
| :--- | :--- | :--- | :--- |
| 1.0 | 2026-05-12 | Initial VSA Architecture Release | Gemini CLI |
