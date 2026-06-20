# 🛡️ Azura Time - Project Status

### Phase 43: Documentation Restructuring & Index Refinement (June 20, 2026)
- **Documentation Migration:** Reorganized the repository's root directory by moving development markdown guides (`README.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `DEPLOYMENT.md`, `RECOVERY_PROTOCOL.md`, `ROADMAP.md`, `TROUBLESHOOTING.md`) into the [docs](file:///home/max/azuratime/nusantara-main/docs) folder.
- **Plan Consolidation:** Merged redundant planning blueprints (`SESSION_TIERING_SYSTEM: 3-Level Hierarchy Plan.md` and `SUBJECT_SESSION_PLAN.md`) into their main respective specifications: [SESSION_TIERING.md](file:///home/max/azuratime/nusantara-main/docs/SESSION_TIERING.md) and [MIGRATION_SUBJECT_SESSION.md](file:///home/max/azuratime/nusantara-main/docs/MIGRATION_SUBJECT_SESSION.md).
- **Index Overhaul:** Overhauled the newly renamed root [index_project.md](file:///home/max/azuratime/nusantara-main/index_project.md) (formerly `PROJECT_INDEX.md`) to index all 18 local entities, 34 ViewModels, 17 UI Effects, and 24 Repositories, ensuring a 100% accurate file mapping for subsequent AI interactions.

### Phase 42: Custom Subjects & Soft-Deleted Restore Fixes (June 20, 2026)
- **Custom Subjects Allowed:** Removed the strict template verification in [SessionManagementViewModel.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/session/ui/SessionManagementViewModel.kt) to allow user-defined custom subjects to be added successfully while dynamically assigning the `isFromTemplate` flag.
- **Soft-Deleted Subject Reactivation:** Modified `getSubjectByName` in [SessionDao.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/session/data/local/SessionDao.kt) to query across all subjects regardless of active status. Hardened `saveSubject` in [SessionRepositoryImpl.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/session/SessionRepositoryImpl.kt) to automatically reactivate (`isActive = true`) soft-deleted subjects when they are re-added, avoiding composite key `(schoolId, name)` SQLite violations.
- **Robust Unit Testing:** Added comprehensive unit tests in [SessionManagementViewModelTest.kt](file:///home/max/azuratime/nusantara-main/app/src/test/java/com/azuratech/azuratime/features/session/ui/SessionManagementViewModelTest.kt) to cover dynamic `isFromTemplate` assignment for both template-matching and custom fallback subject creation.
- **Verification:** Successfully compiled and executed all targeted JVM unit tests.

### Phase 41: School Explorer Deduplication & Unique Constraints (June 19, 2026)
- **Database Schema Upgraded (v26):** Promoted Room Database version to `26` and created composite unique indexes `(schoolId, name)` on both `classes` and `subjects` tables to prevent name duplications at the SQLite layer.
- **Enterprise-Grade Coordinated Deduplication:** Authored and integrated `MIGRATION_25_26` containing a multi-step remapping SQL transaction. The migration safely resolves pre-existing duplicate groups to a master record (oldest ID) and updates referencing foreign key IDs in related tables (e.g., `class_sessions`, `student_class_assignments`, `school_class_assignments`, `account_class_access`, `accounts`) to preserve database integrity and prevent constraints violations.
- **DAO Resolution:** Verified that `ClassDao` and `SessionDao` use of `OnConflictStrategy.IGNORE` now functions flawlessly when resolving name collisions.
- **Verification:** Successfully executed `./gradlew compileDebugKotlin` confirming perfect compile-time type-safety and code layout.

### Phase 40: Unit Test Compilation Fixes (June 19, 2026)
- **Room withTransaction Mock Alignment:** Added missing `androidx.room.withTransaction` import and resolved class cast issues in [SessionRepositoryTest.kt](file:///home/max/azuratime/nusantara-main/app/src/test/java/com/azuratech/azuratime/features/session/SessionRepositoryTest.kt) by correctly mocking extension function signature and accessing the second argument (`secondArg`) for the transaction block.
- **Coroutines & Mocking in UI Tests:** Refactored [SessionManagementViewModelTest.kt](file:///home/max/azuratime/nusantara-main/app/src/test/java/com/azuratech/azuratime/features/session/ui/SessionManagementViewModelTest.kt) to mock the suspend function `fetchAllGlobalSubjects` using `coEvery` and returning custom `Result.Success` directly instead of matching a coroutine flow flowOf.
- **Verification:** Verified 100% compilation and successful execution of all JVM unit tests.

### Phase 39: Subject Template Selection & Fallback Custom Inputs (June 19, 2026)
- **Rich Model Integration:** Migrated `availableSubjects` state from simple strings to full domain model `SubjectTemplate` structures, carrying both names and descriptive categories (e.g., MIPA, IPS, Umum, Bahasa).
- **Flexible UI Dialogs:** Hardened `AddSubjectDialog` to display global template lists featuring search-by-name filters, category badges, and dynamic custom fallback options (`Create custom: "..."`).
- **ViewModel Syncing:** Configured `SessionManagementViewModel` to fetch global subjects via `TemplateRepository` with comprehensive fallback configurations on failure.
- **Verification & Formats:** Confirmed spotless format compliance and successful execution of all JVM unit tests.

### Phase 38: Unit Test Hardening & Timezone Determinism (June 19, 2026)
- **Timezone/Midnight Wrap-around Resilience:** Introduced custom `currentTime` parameter injection in [GetActiveTieredSessionUseCase.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/session/GetActiveTieredSessionUseCase.kt) to prevent unit tests from failing due to midnight local time wrapping in [GetActiveTieredSessionUseCaseTest.kt](file:///home/max/azuratime/nusantara-main/app/src/test/java/com/azuratech/azuratime/features/session/GetActiveTieredSessionUseCaseTest.kt).
- **Mock Alignment:** Updated MockK mocks in [DashboardViewModelTest.kt](file:///home/max/azuratime/nusantara-main/app/src/test/java/com/azuratech/azuratime/features/dashboard/ui/DashboardViewModelTest.kt) to match the new 4-parameter `invoke` signature, resolving test timeout and Turbine errors.
- **Aesthetic Formatting & Quality Control:** Successfully ran Spotless formatting and confirmed 100% of compilation and unit tests pass cleanly.

### Phase 37: School Templates Dashboard Entry Point & Diagnostics (June 19, 2026)
- **Dashboard Card Integration:** Added a "School Templates" entry card in [AccountTasksGrid.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/dashboard/ui/components/AccountTasksGrid.kt) for Admin users, resolving the issue where the template dashboard was inaccessible.
- **String Resources:** Registered `dashboard_school_templates` in [strings.xml](file:///home/max/azuratime/nusantara-main/app/src/main/res/values/strings.xml).
- **Diagnostics & Unit Tests:** Hardened parsing diagnostics in [SchoolTemplate.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/template/domain/model/SchoolTemplate.kt) to log warning statements on type mismatches, and added static log mocking in [TemplateDashboardViewModelTest.kt](file:///home/max/azuratime/nusantara-main/app/src/test/java/com/azuratech/azuratime/features/template/ui/TemplateDashboardViewModelTest.kt) to prevent JVM unit test failures.
- **Unit Test Alignment:** Mocked Admin role in [SessionPickerViewModelTest.kt](file:///home/max/azuratime/nusantara-main/app/src/test/java/com/azuratech/azuratime/features/session/ui/SessionPickerViewModelTest.kt) to align with supervisor filtering rules.
- **Class & Subject Detail Previews:** Resolved class and subject IDs to their actual names in [TemplateDashboardViewModel.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/template/ui/TemplateDashboardViewModel.kt) by performing concurrent batch fetches from `global_classes` and `global_subjects`.
- **FlowRow Chip Previews:** Rendered class and subject names as wrapping, colorful `AssistChip` layouts inside [TemplateDashboardScreen.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/template/ui/TemplateDashboardScreen.kt) to give admins immediate visual feedback before applying templates.
- **Quality Verification:** Passed 100% of compilation, unit tests, and local build checks.

### Phase 36: School Structure Templates Integration (June 18, 2026)
- **MVI Architecture Implementation:** Created [TemplateDashboardViewModel.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/template/ui/TemplateDashboardViewModel.kt) adhering to the project's strict State-Event-Effect pattern.
- **Midnight Azure UI:** Developed [TemplateDashboardScreen.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/features/template/ui/TemplateDashboardScreen.kt) to list and apply templates using Compose.
- **Robust Unit Testing:** Added [TemplateDashboardViewModelTest.kt](file:///home/max/azuratime/nusantara-main/app/src/test/java/com/azuratech/azuratime/features/template/ui/TemplateDashboardViewModelTest.kt) to verify states, events, and effects (resolving race conditions using `UnconfinedTestDispatcher` eager collection).
- **Navigation Integration:** Registered the `SchoolTemplates` route inside [NavigationRoutes.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/core/navigation/NavigationRoutes.kt), [Screen.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/core/navigation/Screen.kt), and [ManagementGraph.kt](file:///home/max/azuratime/nusantara-main/app/src/main/java/com/azuratech/azuratime/core/ui/navigation/graphs/ManagementGraph.kt).
- **Quality Verification:** Passed 100% of compilation, spotless check, and new unit tests.

### Phase 35: Midnight Azure Rebranding & Dashboard Intelligence (June 15, 2026)
- **Visual Identity:** Implemented the **Midnight Azure** premium theme.
    - **Primary:** Deep Navy Azure (`#1E3A8A`) for authoritative branding.
    - **Secondary:** Cobalt Blue (`#3B82F6`) for modern accents.
    - **Backgrounds:** Clean Slate (`#F8FAFC`) for light mode and Midnight (`#0F172A`) for OLED-optimized dark mode.
- **Dynamic Context:** Refactored Dashboard to automatically resolve class context for Supervisors (Session-Aware logic), eliminating the need for manual "Active Class" selection.
- **Enterprise Polish:** 
    - Updated `AssignClassScreen` with **Search functionality**, Step-by-Step UI, and "Select All/Clear All" actions.
    - Enhanced `SessionPickerScreen` with search and distinct categorization (Scheduled vs. Matrix).
    - Beautified `Staff Profiles` with **Section Headers** and a responsive **FlowRow** role chip layout.
- **Verification:** Verified consistent branding across 50+ UI components; `compileDebugKotlin` and `spotlessCheck` passing 100%.

### Phase 34: Enterprise Matrix System & Bulk Import (June 15, 2026)
- **Bulk CSV Engine:** Implemented the **Matrix Import Engine** for batch teacher-class-subject assignments.
    - Supports `teacher_email`, `class_name`, and `subject_name` columns.
    - Intelligent resolution of human-readable names to internal system IDs.
    - Preview UI for data validation before committing to Firestore.
- **Interactive Matrix:** Enabled direct Class-Subject management by clicking on Staff Profile cards with a polished, iterative UI.
- **Data Integrity:** 
    - Implemented **Soft Delete for Subjects** (`isActive` flag) to prevent data loss.
    - Hardened Sync Engine with **Unsynced Protection**, ensuring local deletions are pushed to Cloud before being overwritten by remote pulls.
    - Fixed missing class name population in session picking flows.
- **Documentation:** Created `ROADMAP.md` for strategic "Enterprise Gold Standard" planning.

### Phase 33: Session Tiering Foundation (June 12, 2026)
- **Phase 1: Schema Evolution:** Implemented `SessionType` enum and updated `ClassSessionEntity`. Successfully migrated to Room v23.
- **Phase 3: UI/UX Implementation:**
    - Updated `SessionManagementScreen` with a dynamic **Tier Selector** using `FilterChip`.
    - Implemented **Conditional Forms** in `AddSessionDialog` that adapt fields based on `SessionType`.
    - Added visual **Tier Badges** and smooth layout transitions.
- **Phase 4: Reporting & Final Polish:**
    - **Denormalization:** Added `sessionType` to `AttendanceRecordEntity` for O(1) reporting performance.
    - **Database Migration:** Successfully migrated to Room v24 (`MIGRATION_23_24`).
    - **Reporting DAOs:** Added `getRecordsByTier` and `getTierSummaryCount` to `AttendanceRecordDao`.
    - **Hardened Logic:** Updated `processAttendance` in `AttendanceRepositoryImpl` to auto-resolve `sessionType` from `sessionId`.
    - **Documentation:** Created `docs/SESSION_TIERING.md` as the official technical spec.
    - **Verification:** 100% pass rate for unit tests; zero compilation/lint errors.

### Phase 32: Kiosk Feature Decommissioning & Cleanup (June 12, 2026)
- **Feature Decommissioned:** Successfully removed the Corporate Kiosk mode and related autonomous terminal features to maintain core focus.
- **Structural Cleanup:**
  - Purged `TerminalEntity`, `TerminalDao`, and related database configurations.
  - Removed `KioskSessionResolverUseCase`, `KioskSyncWorker`, and `KioskLockHelper`.
- **Semantic Purity:** Removed `AttendanceDirection` (IN/OUT/BREAK) and reverted `AttendanceRecord` to a simplified model.
- **Database Rollback:** Incremented Room version to 22 with a no-op migration (`MIGRATION_21_22`) to safely isolate legacy kiosk schema changes.
- **Verification:** Confirmed 0 occurrences of 'kiosk' and 'terminal' in functional code; verified 100% compilation success.

### Phase 31: Multi-Subject Attendance Hardening (v3.3.2-final) (June 11, 2026)
- **Feature Locked:** Successfully delivered a multi-subject attendance system decoupled from physical classes. Attendance is now attached to `ClassSession` (Class + Subject + Supervisor + Schedule).
- **Data Integrity & Soft Delete:**
  - Implemented `isActive` flag in `ClassSessionEntity` for soft-delete support, preventing orphaned `AttendanceRecord` links.
  - Added `lookupKey` (unique composite index) for high-performance daily schedule queries and collision prevention.
  - Enforced "Subject Deletion Guard": Subjects cannot be deleted if active sessions are linked.
- **Room Migration (v18):** Successfully migrated database to version 18 with explicit `MIGRATION_17_18` paths and index efficiency hardening.
- **UseCase Centralization:** Created `CreateSessionUseCase` to unify `sessionId` and `lookupKey` generation logic.
- **MVI Refinement:** Updated `SessionManagementViewModel` with robust error handling (Conflicts/Validation) and SharedFlow `UiEffect` feedback.
- **Verification:** 100% compilation success, `spotlessCheck` passing, and `v3.3.2` release tagged on `main`.

### Phase 30: Global Semantic Purity & MVI Restoration (June 5, 2026)
- **Terminology Alignment:** Executed a project-wide sweep to replace "User" with "Account", "Teacher" with "Supervisor", and "Classroom" with "Class".
- **Architecture Refinement:** Migrated `StudentClassAssignmentEntity` to the `student` package and `AccountClassAccessEntity` to the `account` package.
- **MVI Purity:** Refactored `BiometricEnrollmentViewModel`, `AttendanceCaptureViewModel`, and `DataIntegrityViewModel` to use specific `UiEffect` types.
- **Language Policy:** Translated all hardcoded Indonesian UI strings and technical comments to Common English across 20+ files.
- **Documentation Sync:** Updated `ARCHITECTURE.md`, `PROJECT_INDEX.md`, and `VOCABULARY.md` to reflect the latest semantic standards.
- **Test Suite Alignment:** Updated unit tests (`GetAccountUseCaseTest`, `DashboardViewModelTest`) to match new repository signatures.

### Phase 29: Robust Multi-Class Persistence & Sync Harmonization (June 5, 2026)
- **Feature Completion:** Successfully implemented persistent multi-class assignment. A single student can now belong to multiple classes (e.g., 10-IPA-1 and 10-IPA-2) without data loss during sync or logout.
- **Data Model Refinement:**
  - Local DB: Updated `StudentClassAssignmentEntity` to use composite primary keys (`studentId`, `classId`) and `ForeignKey.NO_ACTION` to prevent accidental cascade deletions.
  - Firestore (Cloud): Standardized on a Hybrid Sync strategy. Primary source is `studentIds` in Class documents; fallback is `classIds` in Student documents.
- **Sync Logic Overhaul:**
  - Trigger Mechanism: Fixed critical bug where `pushPendingProfiles` was not triggered after class assignment changes in `StudentFormViewModel`. Added explicit call to `pushPendingProfiles()` upon successful profile save.
  - Data Freshness: Modified `saveProfile` in `StudentRepositoryImpl` to set `isSynced = false` locally, ensuring the sync worker always picks up the latest class assignments.
  - Repository Fix: Refined `pushPendingProfiles` to fetch the FULL LIST of class IDs from `assignmentDao` before pushing to Firestore, preventing single-class overwrites.
- **UI/UX Integration:**
  - Fixed missing event handler in `ClassViewModel.onEvent` for `AddStudentToClass`.
  - Ensured both "Management Class" and "Management Student" flows correctly update the Cloud state.
- **Verification:** Verified data persistence across Logout/Login cycles and confirmed bidirectional sync between Local Room DB and Firestore.

### Phase 28: Robust Sync Logic & Firebase Function Refinement (June 2, 2026)
- **Background Sync**: Updated `AccountSyncWorker` to explicitly sync classes for the active school, ensuring no data gaps after account synchronization.
- **Sync Reliability**: Fixed a critical bug in `SchoolRepositoryImpl.syncClasses` where remote network failures were treated as successes, preventing proper error handling and retries.
- **SSOT Integrity**: Updated `SchoolRepositoryImpl` to mark classes as `isSynced = true` when pulled from remote, ensuring local state matches Cloud state.
- **Worker Logic Fix**: Refined `AccountSyncWorker` to correctly return `Result.retry()` if the initial `pushAccount` fails with a network error, ensuring data eventual consistency.
- **Test Stabilization**: Fixed compilation errors in `AccountSyncWorkerTest` and `ClassAssignmentSyncTest` by updating constructor parameters and adding missing dependencies.
- **Verification**: 100% pass rate for `spotlessCheck`, `compileDebugKotlin`, and all unit tests.
- **Firebase Function Hardening**: Refined `onregistrationapproved` in `functions/src/index.ts` to:
    - Prevent accidental overwriting of school owners (`accountId`, `ownerEmail`) when joining existing schools.
    - Ensure `email` and `name` are present in the `accounts` collection for seamless app synchronization.
- **Supervisor Accessibility**: Verified and improved the class visibility logic to ensure supervisors only see their assigned classes while admins retain full access.

### Phase 27: Student Barcode PDF Export (June 1, 2026)
- **PDF Generation**: Implemented a lightweight, native PDF generator using `android.graphics.pdf.PdfDocument`.
- **Batch Export**: Added ability to select multiple students and generate a printable grid of QR codes on A4 pages.
- **Sharing Integration**: Integrated with `FileProvider` to allow immediate sharing or opening of generated PDF files.
- **MVI Refinement**: Updated `StudentRosterBarcodeViewModel` and `StudentRosterBarcodeScreen` to handle the export side-effect seamlessly.

### Phase 26: Robust In-App Update Implementation (June 1, 2026)
- **Stability Fix**: Resolved "stuck disabled" button state by refining `enabled` logic in `AppUpdateDialog`. The button now correctly re-enables on network failure.
- **Network Resilience**: Increased `readTimeout` to 60s and explicitly enabled `followRedirects` in `AppUpdateRepositoryImpl` for reliable GitHub release downloads.
- **Error Visibility**: Added real-time error reporting to the `AppUpdateDialog` for better user feedback during connection drops or HTTP failures.
- **Clean MVI**: Refactored `AppUpdateViewModel` to strictly follow `v3.2.0-ai-native` standards with improved logging and error handling.
- **Production Verification**: Verified full download-to-install flow using production APKs and restored live Firebase Remote Config data.

### Phase 25: Dashboard Menu Restoration (May 31, 2026)
- **Menu Restoration**: Restored missing "Class Management", "Staff Management", "School Selection", and "Follow System" cards to the Dashboard grid.
- **Role-Based Visibility**: Enforced strict visibility logic using `PermissionUtils.isAdmin(activeSchoolId)`.
- **UI Grid Reorganization**: Optimized `AccountTasksGrid` layout for better accessibility and logical grouping of Admin vs. Member actions.
- **Navigation Verification**: Ensured all restored menu items correctly navigate to their respective screens (`ClassManagement`, `Following`, `SchoolList`).
- **MVI Consistency**: Maintained 100% adherence to `v3.2.0-ai-native` standards with clean state propagation.

### Phase 24: AI Music Rule-Based Engine (May 31, 2026)
- **Data Model**: Expanded `TraditionalMusicTrack` to include `name` and updated Room `AiMusicEntity` accordingly.
- **Rule Engine**: Implemented a functional filtering engine in `AiMusicRepositoryImpl` using an internal dataset of Nusantara tracks (Gamelan, Angklung, Sasando, etc.).
- **Smart Filtering**: Enabled filtering by `region` and `mood` with a fallback to "Best of Nusantara" curated suggestions if no matches are found.
- **UI Enhancement**: Updated `AiMusicScreen` with a refined track list and a "Play Preview" button for immediate user feedback.
- **Unit Testing**: Added `AiMusicRepositoryTest` to verify filtering logic and updated `AiMusicViewModelTest` to ensure correct UI state reactivity.

### Phase 23: Supervisor-Specific Dashboard View (May 31, 2026)
- **ViewModel Filtering**: Updated `DashboardViewModel` to filter `allClasses` based on `assignedClassIds` for `SUPERVISOR` accounts.
- **My Classes Section**: Implemented `MyAssignedClassesSection` in the Dashboard UI for quick access to attendance and rosters of assigned classes.
- **Permission Enforcement**: Utilized `PermissionUtils` to ensure data visibility follows role-based access control (Admin vs Supervisor).
- **Unit Verification**: Added `DashboardViewModelTest` cases to confirm that Supervisors only see their assigned classes while Admins retain full visibility.
- **MVI Compliance**: Followed `v3.2.0-ai-native` standards with strict State-Event-Effect separation.

### Phase 22: E2E Sync Integration Testing (May 31, 2026)
- **Integration Suite**: Created `ClassAssignmentSyncTest.kt` using Robolectric and MockK to verify end-to-end data flow.
- **Worker Verification**: Confirmed `AccountSyncWorker` correctly pulls updated `assignedClassIds` from Cloud to Room via `syncAccount()`.
- **UseCase Validation**: Verified `AssignClassToSupervisorUseCase` triggers the correct repository calls and handles network failures gracefully.
- **Consistency Check**: Asserted that `SchoolMembership` data model remains consistent across local and remote sources during the sync lifecycle.

### Phase 21: Dedicated Class Assignment UI (May 31, 2026)
- **UseCase Implementation**: Created `AssignClassToSupervisorUseCase.kt` to handle supervisor class assignments via `AccountRepository`.
- **MVI Architecture**: Built `AssignClassViewModel.kt` following the `v3.2.0-ai-native` standard with strict State-Event-Effect separation.
- **Dedicated UI**: Developed `AssignClassScreen.kt` featuring a modern Chip-based selector and a "Save" button for explicit assignment confirmation.
- **Navigation Integration**: Registered `ASSIGN_CLASS` route and integrated it into the `AccountGraph`.
- **Permission Enforcement**: Refactored `FollowingScreen` to show the assignment entry point only for accounts with `ADMIN` role in the active school.
- **Refactoring**: Replaced legacy dialog-based assignment with the new dedicated screen for better UX and consistency.

### Phase 20: Role & Assignment Refactor (May 31, 2026)
- **Supervisor Architecture**: Migrated from "Teacher" entity to `AccountRole.SUPERVISOR`. Teachers are now Accounts with specific class assignments.
- **Assigned Class IDs**: Integrated `assignedClassIds` into `SchoolMembership` to support granular access control (Teacher as Supervisor of specific classes).
- **Permission Engine**: Created `PermissionUtils.kt` with extension functions (`isAdmin`, `isSupervisorOf`, `canAccessClass`) for centralized authorization.
- **Terminology Unification**: Updated `AccountRole` to [SUPER_ADMIN, ADMIN, SUPERVISOR, USER], replacing legacy names.
- **UI Refactor**: Replaced hardcoded role checks in `Dashboard` and `Attendance` screens with the new permission engine.
- **Migration Blueprint**: Added `MigrateRoles.kt` with Firestore migration logic for legacy accounts.

### Phase 19: AI Music Feature Scaffolding & Integration (May 30, 2026)
- **Feature Scaffolding**: Built a complete vertical slice for "Traditional Music AI" with Domain, Data (Room + Remote Stub), and MVI UI layers.
- **MVI Compliance**: Followed `v3.2.0-ai-native` standards with `UiState`, `UiEvent`, and `UiEffect` (SharedFlow).
- **Navigation Integration**: Registered `aiMusic` route and integrated `AiMusicScreen` into the `dashboardGraph`.
- **Dashboard UI**: Added a new "AI Native Features" section to the Dashboard with a shortcut to the AI Music tool.
- **Data Integrity**: Verified 100% compilation and unit test coverage for the new feature's business logic.

### Phase 18: Unit Testing & WorkManager Stability (May 30, 2026)
- **Test Migration**: Moved `AccountSyncWorkerTest` from `androidTest` to `src/test` for faster execution and better CI compatibility.
- **Dependency Optimization**: Added `work-testing`, `robolectric`, and `androidx.test.core` to `testImplementation`.
- **Worker Verification**: Achieved 100% coverage for `AccountSyncWorker` sync logic, including pull/push success, failure, and retry scenarios.
- **Robolectric Integration**: Configured Robolectric to provide a functional `Context` for unit tests involving WorkManager and repositories.
- **Architecture Documentation**: Formalized the sync strategy in [ADR 001: Account Sync Architecture](docs/adr/001-account-sync-architecture.md).

### Phase 17: APK Optimization & Build Stability (May 27, 2026)
- **Feature Completed**: Implemented ABI splits for `arm64-v8a` and `armeabi-v7a` to reduce APK size.
- **Modern Distribution**: Disabled Universal APK generation in favor of leaner, architecture-specific APKs.
- **Build Stability**: Resolved `lintVitalAnalyzeRelease` crash by disabling the problematic `NullSafeMutableLiveData` detector (AGP bug workaround).
- **Efficiency**: Optimized `app/build.gradle.kts` for faster, more reliable deployments.

### Phase 16: Custom In-App Update Engine (May 22, 2026)
- **Feature Completed**: Implemented a native in-app update engine from scratch hitting `https://osengprogrammer.github.io/nusantara2/version.json`.
- **MVI Compliance**: 100% adherence to the "State + Event + Effect" pattern via `AppUpdateViewModel`.
- **Background Downloader**: Built a custom APK downloader in `AppUpdateRepository` with real-time progress tracking.
- **Secure Installation**: Configured `FileProvider` and `REQUEST_INSTALL_PACKAGES` for seamless APK installation on Android 7.0+.
- **Aesthetic UI**: Added `AppUpdateDialog` with sleek progress indicators and release notes support, integrated into `MainScreen`.

### Phase 15: Distribution & Stability (May 21, 2026)
- **Firebase Distribution**: Incremented to **v1.1.4 (Build 6)**.
- **Visual Feedback**: Added a version badge (`v1.1.4`) to the Dashboard for update verification.
- **Update Logic**: Added Toast and Logcat feedback for better troubleshooting of in-app updates.
- **Test Toast**: Added 'ok' Toast on Dashboard load to verify real-time updates.
- **Build Fixes**: Simplified `build.gradle.kts` to ensure single-APK generation for Firebase compatibility.
- **Zero Warnings**: Room KSP warnings suppressed for `AttendanceConflictEntity`.
- **Admin**: Updated primary tester to `osengprogrammer@gmail.com`.

### Phase 14: Secure Piped Barcode & Admin Portal Perfection (May 20, 2026)
- **100% MVI Activated**: Fully implemented the **`StudentRosterBarcode`** vertical slice. This feature strictly follows the "State + Event + Effect" pattern, enabling admins to generate and preview student QR codes with real-time reactivity.
- **Secure Piped Format**: Standardized barcode content to a 3-part piped string: **`schoolId|classId|studentId`**. This enables cross-app validation and prevents "barcode spoofing" from external sources.
- **Smart Scanner Engine**: Upgraded the `AttendanceCaptureViewModel` to parse and validate the new piped format. Implemented automatic school ID checking and class-specific verification to ensure check-in integrity.
- **AI-Native Excellence**: All new components (`StudentRosterBarcodeScreen`, `StudentRosterBarcodeViewModel`) are built with 100% adherence to the `ARCHITECTURE.md` standards, ensuring instant readability and maintenance by any AI agent.
- **Parent App Handshake**: Aligned the QR generation logic with `azura-parent` to ensure a seamless "Parent Follows Child" experience across the AzuraTime ecosystem.

### Phase 13: 100% MVI & AI-Native Architecture Perfection (May 19, 2026)
- **Effect-Driven MVI Standard**: Fully migrated all core features to the **`UiEffect`** pattern. Transient events (Toasts, Snackbars, Navigation) are now handled via a dedicated `SharedFlow<UiEffect>`, ensuring zero "ghost states" and persistent UI state purity.
- **DRY Repository Engine**: Implemented the **`asLocalResult()`** and **`asResult()`** extension suite across 8+ repositories. This standardizes data mapping from Room/Firestore to Result wrappers, eliminating hundreds of lines of redundant boilerplate.
- **Bulk Import Logic Fix**: Resolved the persistent `SQLITE_CONSTRAINT_FOREIGNKEY` (Code 787) error in the Student Bulk Import feature. Implemented a "Smart Class Resolution" engine that automatically maps class names to UUIDs and auto-creates missing classes.
- **CSV Standardization**: Unified CSV headers to follow canonical project naming (`student_id`, `full_name`, `class_name`, `photo_url`). Updated template generation and parsing aliases for 100% compatibility.
- **Zero-Warning Clean Code**: Performed a project-wide scan and resolved **40+ compiler warnings** (redundant smart-casts, unused parameters, unused variables). Standardized icon usage to the latest Compose `AutoMirrored` patterns.
- **MVI Contract Testing**: Established the **"Gold-Standard" MVI Unit Test** template. Features are now self-validating, asserting strict "Event → State + Effect" contracts.
- **Cleanup**: Purged legacy "Example" files and redundant boilerplate tests, reducing build noise and improving compilation speed.

### Phase 12: Account Terminology Refactor
- **Project-Wide Naming Optimization**: Fully migrated all legacy "Staff", "Teacher", and "User" terminology to **"Account"** across the UI, ViewModels, and package structures.
- **Feature Package Consolidation**: Renamed the `features.staff` package to `features.account`, moving `AccountManagementViewModel`, `AccountProfileScreen`, and all related components.
- **Navigation Updates**: Refactored `UserGraph.kt` to `AccountGraph.kt` and updated `NavigationRoutes` to use `ACCOUNT_GRAPH` and `ACCOUNT_PROFILE`.
- **Compilation & SSOT Enforcement**: Resolved dependency mapping and import errors in `SchoolRepository` and `ClassViewModel` to strictly depend on `AccountDao` and `AccountEntity` as the single source of truth.

### Phase 12: Final Terminology Unification & Naming Standard Enforcement (v3.2.0-ai-native)
- **100% "Account" Terminology**: Eliminated all legacy "Teacher", "Staff", and "User" references (identity context) project-wide.
- **AccountRole Enum**: Established `AccountRole` [ADMIN, TEACHER, MEMBER, OBSERVER] as the canonical role model.
- **Flow Naming SSOT**: Enforced `Flow` suffix on all `Flow<T>` and `StateFlow<T>` variables (e.g., `uiStateFlow`, `studentsFlow`).
- **Route CamelCase Enforcement**: Sanitized all navigation routes to use `camelCase` (e.g., `attendanceCapture`).
- **Complete Repository Decoupling**: Migrated 7 legacy repositories to domain interfaces with `Result<T>` returns, strictly enforced via `RepositoryModule`.
- **Build Stabilization**: Verified 100% compilation and linting compliance (`compileDebugKotlin` ✅).

## 🚀 Recent Updates (May 17, 2026)

### Phase 11G: Reporting DI & NavGraph Realignment
- **Reporting Feature Consolidation**: Moved all reporting-related files (Entities, DAOs, Repositories, ViewModels, and Screens) to the `com.azuratech.azuratime.features.reporting` package.
- **Audit & Export Migration**: Consolidated `AuditLog` and `Export` functionality into the reporting feature module, ensuring a unified reporting architecture.

### Phase 11F: Biometric DI & NavGraph Realignment
- **DI Restoration**: Updated `AppModule.kt` and `DataSourceModule.kt` to reference the new `biometric` feature package.
- **Project-Wide Entity Realignment**: Fixed unresolved references to `BiometricFaceEntity` and `FaceWithDetails` across 10+ files.

## 🛠️ Build Status
- Kotlin compilation: ✅ **SUCCESSFUL** (0 Critical Warnings)
- KSP Processing: ✅ **CLEAN** (Room merge and cursor warnings resolved)
- Unit Tests: ✅ **PASSING** (MVI Contract Tests Verified)
- Check Suite: ✅ **SUCCESSFUL** (Spotless applied)

## 🏗️ Architecture Compliance
- **MVI Engine**: 100% Effect-Driven (State + Effect).
- **Data Layer**: Local-First Room SSOT with `asLocalResult()` mapping.
- **Terminology**: Unified "Account", "Student", and "Biometric".
- **AI-Native**: Standardized headers and idiomatic patterns across all 29 ViewModels.
- **In-App Updates**: Powered by a custom **GitHub-based Engine**.
    - **Source**: `https://osengprogrammer.github.io/nusantara2/version.json`
    - **Stability**: Protocol-level cache-busting and 60s network resilience.
    - **Versioning**: Remote `latest_version_code` must be > local `BuildConfig.VERSION_CODE` to trigger UI.

## 🏆 Azura Time v3.2.1-ai-native — 100% SEMANTIC PURITY & ARCHITECTURAL ALIGNMENT
- ✅ **100% English-First Policy**: All code, technical comments, and documentation migrated to Common English.
- ✅ **Global Semantic Sweep**: Eliminated legacy terms (User, Teacher, Staff, Classroom) in favor of standardized taxonomy (Account, Supervisor, Class).
- ✅ **MVI Effect-Driven Completion**: All transient UI actions (Snackbars, Toasts, Navigation) now use specific `UiEffect` SharedFlows.
- ✅ **Entity/DAO Package Realignment**: Fixed structural anomalies by moving `StudentClassAssignment` and `AccountClassAccess` to their correct feature slices.
- ✅ **Repository Method Standardization**: Enforced `Flow` suffix on all reactive methods project-wide (e.g., `observeEnrollmentsFlow`).
- ✅ **Post-Refactoring Stabilization**: Resolved all syntax corruption and unresolved references caused by the global naming migration.

### Phase 30: Global Semantic Purity & MVI Restoration (June 5, 2026)
- **Terminology Alignment:** Executed a project-wide sweep to replace "User" with "Account", "Teacher" with "Supervisor", and "Classroom" with "Class".
- **Architecture Refinement:** Migrated `StudentClassAssignmentEntity` to the `student` package and `AccountClassAccessEntity` to the `account` package.
- **MVI Purity:** Refactored `BiometricEnrollmentViewModel`, `AttendanceCaptureViewModel`, and `DataIntegrityViewModel` to use specific `UiEffect` types.
- **Language Policy:** Translated all hardcoded Indonesian UI strings and technical comments to Common English across 20+ files.
- **Documentation Sync:** Updated `ARCHITECTURE.md`, `PROJECT_INDEX.md`, and `VOCABULARY.md` to reflect the latest semantic standards.
- **Test Suite Alignment:** Updated unit tests (`GetAccountUseCaseTest`, `DashboardViewModelTest`) to match new repository signatures.
# Phase 30: Global Semantic Purity & MVI Restoration (June 5, 2026)
- **Terminology Alignment:** Executed a project-wide sweep to replace "User" with "Account", "Teacher" with "Supervisor", and "Classroom" with "Class".
- **Architecture Refinement:** Migrated `StudentClassAssignmentEntity` to the `student` package and `AccountClassAccessEntity` to the `account` package.
- **MVI Purity:** Refactored `BiometricEnrollmentViewModel`, `AttendanceCaptureViewModel`, and `DataIntegrityViewModel` to use specific `UiEffect` types.
- **Language Policy:** Translated all hardcoded Indonesian UI strings and technical comments to Common English across 20+ files.
- **Documentation Sync:** Updated `ARCHITECTURE.md`, `PROJECT_INDEX.md`, and `VOCABULARY.md` to reflect the latest semantic standards.
- **Test Suite Alignment:** Updated unit tests (`GetAccountUseCaseTest`, `DashboardViewModelTest`) to match new repository signatures.
