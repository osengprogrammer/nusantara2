# 🛡️ Azura Time - Project Status

### Phase 12: Account Terminology Refactor
- **Project-Wide Naming Optimization**: Fully migrated all legacy "Staff", "Teacher", and "User" terminology to **"Account"** across the UI, ViewModels, and package structures.
- **Feature Package Consolidation**: Renamed the `features.staff` package to `features.account`, moving `AccountManagementViewModel`, `AccountProfileScreen`, and all related components.
- **Navigation Updates**: Refactored `UserGraph.kt` to `AccountGraph.kt` and updated `NavigationRoutes` to use `ACCOUNT_GRAPH` and `ACCOUNT_PROFILE`.
- **Compilation & SSOT Enforcement**: Resolved dependency mapping and import errors in `SchoolRepository` and `ClassViewModel` to strictly depend on `AccountDao` and `AccountEntity` as the single source of truth.

## 🚀 Recent Updates (May 16, 2026)
n### Phase 12B: Complete Legacy Terminology Eradication
- **Surgical Naming Refactor**: Conducted a final scan and successfully eradicated remaining instances of `user`, `USER`, `TEACHER`, and `Face ID` across the codebase.
- **UI & State Alignments**: Renamed variables in `DashboardUiState`, `DashboardViewModel`, `ClassViewModel`, and `MembershipViewModel` to use `account` instead of `user`. Replaced hardcoded "User Info" with "Account Info".
- **Role Standardization**: Updated fallback roles in `AccountEntity`, `Membership`, and auth states from `"USER"` / `"TEACHER"` to `"ACCOUNT"`. 
- **Export Data**: Changed CSV header from `Face ID` to `Student ID` in `ExportUtils.kt`.
- **Cleanup**: Purged stale `.broken` and `.nuclear-backup` files.
- **Build Status**: Verified Kotlin compilation (`compileDebugKotlin` ✅).

### Phase 11G: Reporting DI & NavGraph Realignment
- **Reporting Feature Consolidation**: Moved all reporting-related files (Entities, DAOs, Repositories, ViewModels, and Screens) to the `com.azuratech.azuratime.features.reporting` package.
- **Audit & Export Migration**: Consolidated `AuditLog` and `Export` functionality into the reporting feature module, ensuring a unified reporting architecture.
- **DI & Navigation Updates**: Updated `AppModule.kt` to provide reporting DAOs and re-routed `ReportingGraph.kt` to the new feature-scoped locations for `AttendanceMatrixScreen` and `DailyDetailScreen`.
- **Domain Package Alignment**: Fixed package declarations and added missing `SyncStatus` imports across reporting domain models and UI components.
- **Build Stabilization**: Successfully restored the build state after the massive reporting package reorganization (`compileDebugKotlin` ✅).

### Phase 11F: Biometric DI & NavGraph Realignment
- **DI Restoration**: Updated `AppModule.kt` and `DataSourceModule.kt` to reference the new `biometric` feature package. Corrected package declarations and DAO provider signatures.
- **NavGraph Alignment**: Updated `ManagementGraph.kt` to import `BiometricScreen` from its new feature-scoped location.
- **Project-Wide Entity Realignment**: Fixed unresolved references to `BiometricFaceEntity` and `FaceWithDetails` across 10+ files, including `ProfileMappers`, `FaceAssignmentDao`, and multiple ViewModels.
- **Build Stabilization**: Resolved KSP cache corruption through deep clean; verified build success (`compileDebugKotlin` ✅).

### Phase 11E: Staff Feature Move & KSP Restoration
- **KSP Failure Resolved**: Fixed `MissingType` errors in `Converters.kt` by adding missing imports for `Membership` and `FriendConnection`.
- **Database Refinement**: Cleaned up `AppDatabase.kt` by removing duplicate imports and qualifying `JournalMode` with `RoomDatabase.JournalMode`.
- **Project-Wide Import Alignment**: Performed a massive import update across 16+ files to reflect the move of `StaffAccountEntity`, `Membership`, `AccountManagementViewModel`, and associated UI components to the `staff` feature package.
- **Build Restoration**: Successfully restored the project to a compiling state (`compileDebugKotlin` ✅).

### Phase 7.19: Reporting Domain Refinement
- **Feature-Scoped Repositories**: Moved `AuditLogRepository` and `ExportRepository` to `features.reporting.data.repo` for better architectural alignment.
- **Package Standardization**: Updated package declarations and imports across the codebase to reflect the new feature-scoped locations.
- **Project Index Alignment**: Updated `PROJECT_INDEX.md` to track the new locations of reporting repositories.

### Phase 7.18: Student Domain Refinement
- **Standardized Domain Models**: Renamed `FaceEnrollmentProfile` to `BiometricEnrollmentProfile` to better reflect its purpose in the UI.
- **Design System Alignment**: Renamed `QuickEditFaceDialog` to `QuickEditStudentDialog` and `FaceAvatar` to `StudentAvatar`.
- **UI Integration**: Updated all call sites in `BiometricScreen`, `StudentRosterScreen`, `UserProfileScreen`, and `ClassDetailScreen` to use the new standardized components.
- **Project Index Cleanup**: Updated `PROJECT_INDEX.md` to reflect recent file renames and standardizations.

### Phase 7.17: Student Roster Naming Cascade
- **UI Naming Alignment**: Renamed `FaceListScreen` to `StudentRosterScreen` and `FaceListBarcodeScreen` to `StudentRosterBarcodeScreen`.
- **ViewModel Naming**: Renamed `FaceListViewModel` to `StudentRosterViewModel` and updated associated state flows for domain consistency.
- **Navigation SSOT**: Updated `NavigationRoutes` and `NavGraph` to use `STUDENT_ROSTER` instead of legacy `MANAGE_FACES` and `FACE_LIST`.
- **Bug Fixes**: Restored missing `StudentRosterBarcodeScreen` entry in the `ManagementGraph` and fixed unresolved preview references.
- **Warning Cleanup**: Resolved several Kotlin warnings (unused parameters, unchecked casts, missing navigation handlers).

### Phase 7.12: Reactive Audit Log
- **Audit Trail SSOT**: Implemented a reactive system audit trail using the `AuditLogProfile` domain model.
- **Data Layer**: Created `AuditLogEntity` and `AuditLogDao` to persist system actions locally.
- **Reactive UI**: Implemented `AuditLogScreen` and `AuditLogViewModel` for real-time observation of system events.

### Phase 7.11: Reactive Export Manager
- **Export Job SSOT**: Migrated the Export Manager to a reactive pipeline using the `ExportJobProfile` domain model.
- **Background Task Tracking**: Implemented `ExportJobEntity` and associated repository methods to track background CSV/Excel export jobs.
- **Reactive UI**: Created `ExportScreen` and `ExportViewModel` for real-time monitoring of export task progress and sync status.

### Phase 7.10: Reactive Report Dashboard
- **Report Summary SSOT**: Migrated the Report Dashboard to a reactive pipeline using the `ReportSummaryProfile` domain model.
- **Data Layer Enhancements**: Created `ReportEntity` and added `toProfile()` mappers to support local-first reporting.
- **Reactive UI**: Implemented `ReportScreen` and `ReportViewModel` observing data directly from Room via `ReportRepository`.
- **Bug Fix**: Fixed a syntax error in `AttendanceMatrixScreen.kt` where an invalid lambda expression was causing build failures.

### Phase 7.9: Reactive Biometric & Class Management
- **Biometric Enrollment SSOT**: Migrated Biometric Enrollment to a reactive Room-first pipeline using the `FaceEnrollmentProfile` domain model.
- **Biometric Management UI**: Implemented `BiometricScreen` and `BiometricViewModel` for real-time management of face enrollments.
- **Reactive Class Management**: Refactored `ClassViewModel` and `ClassManagementScreen` to observe `ClassModel` directly from Room via `SchoolRepository`.
- **Navigation Integration**: Integrated Biometric Management into the `ManagementGraph` and added an entry point in the `RegistrationMenuScreen`.

### Phase 7.8: Reactive Check-In History
- **Direct Repository Observation**: Migrated `CheckInViewModel` and `DailyDetailViewModel` to observe `CheckInRecordEntity` directly from Room via `CheckInRepository`.
- **Decoupled Actions**: Removed legacy Check-In UseCases; action handlers now call the repository directly, which enqueues background sync via `SyncManager`.
- **SSOT Integrity**: Ensured all check-in history views are reactive and strictly follow the local-first pattern.

### Phase 7.7: Reactive Conflict Resolution
- **Multi-Tenant Conflicts**: Updated `AttendanceConflictDao` and `DataIntegrityRepository` to support school-scoped conflict observation.
- **Health Management Hub**: Implemented `DataIntegrityScreen` as a centralized hub for monitoring system health and resolving data collisions.
- **Reactive Resolution**: Wired `DataIntegrityViewModel` to the repository for direct, reactive conflict resolution without intermediate UseCases.

### Phase 7.6: Membership & Sync Refactoring
- **Migrated ViewModels**: `StudentFormViewModel`, `FaceListViewModel`, and `FaceViewModel` have been migrated from deprecated UseCases to the unified `SaveStudentProfileUseCase`.
- **Enhanced SaveStudentProfileUseCase**: Updated to handle optional `photoBytes`. It now orchestrates local photo storage via `PhotoStorageUtils` and profile persistence via `StudentRepository`.
- **Domain Model Alignment**: Construction of `StudentProfile` now happens within the ViewModels, ensuring the domain model is the Single Source of Truth (SSOT) before persistence.
- **Removed Deprecated Usages**: All production usages of `CreateStudentUseCase`, `UpdateFaceUseCase`, `RegisterFaceUseCase`, and `UpdateFaceWithPhotoUseCase` have been eliminated.
- **Codebase Cleanup**: Deleted deprecated UseCase files and their associated tests. Fixed several Kotlin warnings (unused variables, deprecated icons, unnecessary safe calls).
- **Phase 3 Sync Integration**: Implemented `PushStudentsUseCase` and integrated `SyncManager` into `StudentRepositoryImpl`. Every local change now triggers an immediate background sync push.

### Phase 11H: Student Biometric Realignment
- **Terminology Refactor**: Successfully migrated all "Face" related terms to **"Student"** or **"Biometric"** across the entire project (Entities, DAOs, Repositories, ViewModels, and UI).
- **Canonical Models**: Established `StudentBiometricEntity` and `StudentBiometricDao` as the single sources of truth for biometric data.
- **Unified Identity**: Aligned `studentId` across `StudentEntity`, `StudentBiometricEntity`, and `AttendanceRecordEntity` to ensure a consistent person-centric data model.
- **Build Restoration**: Resolved all compilation errors and verified a successful build (`compileDebugKotlin` ✅).

## 🏗️ Architecture Compliance
- Follows the **Local-First** pattern. Remote sync is treated as a side-effect (managed by `SyncWorker` and `ProfileSyncWorker`).
- UI components now interact with the domain layer using `StudentProfile` and `AccessRequestProfile`.
- Repository handles atomic updates to `StudentEntity`, `StudentBiometricEntity`, `StudentClassAssignmentEntity`, `SchoolEntity`, and `AccessRequestEntity` within transactions.

## 🛠️ Build Status
- Kotlin compilation: ✅ **SUCCESSFUL**
- Java/Build artifacts: ⚠️ **PENDING** (Local Gradle cache issues detected in environment, but Kotlin source is valid).

## 📅 Pending Tasks
- [x] Phase 7.7: Reactive Conflict Resolution (Multi-tenant scoped).
- [x] Phase 7.8: Reactive Check-In History migration.
- [x] Phase 7.9: Reactive Biometric & Class Management.
- [x] Update/Clean up unit tests for migrated ViewModels.
- [x] Investigate and resolve primary Kotlin warnings across the codebase.

## 🏆 Nusantara v3.1 — SSOT MIGRATION COMPLETE
- ✅ 11 UI Features migrated (Phases 7.2-7.12)
- ✅ 4 Workers migrated (Phases 7.13-7.16)  
- ✅ 10 ViewModels migrated (Phase 8.1)
- ✅ 41 UseCases removed (2,355+ lines deleted)
- ✅ Build: compileDebugKotlin --quiet → ALWAYS SILENT
- ✅ Architecture: UI → Repository → Room → (internal) Cloud

### 🤖 AI-Native Guardrails

To ensure deterministic, hallucination-free code generation, all AI interactions MUST strictly follow these rules:

#### 1. Code Formatting (Spotless)
- You must write code that adheres to standard Kotlin style. 
- You do NOT need to perfectly format imports yourself. Instead, before finishing a major task or finalizing a commit, you MUST run:
  `./gradlew spotlessApply --quiet`
- *Note:* If spotless fails due to legacy files during a task, fix the immediate file you are working on, but do not derail the task to fix the entire codebase.

#### 2. Strict MVI Architecture (Model-View-Intent)
- ViewModels must NEVER have disparate variables or isolated state flows (e.g., `val isLoading`, `val error`).
- Every ViewModel MUST have exactly one `StateFlow<UiState>` and accept a single stream of `UiEvent`.
- Define a single `data class UiState` inside the ViewModel file.
- Define a single `sealed class UiEvent` inside the ViewModel file.
- Handle all inputs via a single `fun onEvent(event: UiEvent)` block.

#### 3. Strict Error Handling (Result<T>)
- Repositories must NEVER throw exceptions. They must wrap all returns in `kotlin.Result<T>`.
- The ViewModel is responsible for `.onSuccess { }` and `.onFailure { }`. This forces the compiler to ensure you have handled both the happy and sad paths during code generation.

#### 4. UI Previews Mandate
- No Compose screen or component is complete without a `@Preview`.
- Previews must utilize the dummy data defined in `core/ui/preview/PreviewMocks.kt` to allow instant visual verification of the structural integrity of your generated code.

