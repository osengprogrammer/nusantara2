# 🗂️ AzuraTime — PROJECT FILE INDEX (v3.7.0-base)
⚡ *Status: 100% English-First & Session Tiering Hierarchy Integrated.*

## 💾 Room Entities (Local SSOT)
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `AccountEntity.kt` | `features.account.data.local` | Master account identity; includes global role | ✅ Migrated |
| `AccessRequestEntity.kt` | `features.account.data.local` | Tracks join/leave school requests | ✅ Migrated |
| `AttendanceRecordEntity.kt` | `features.attendance.data.local` | Individual attendance logs (scanned/manual) | ✅ Migrated |
| `AttendanceConflictEntity.kt` | `features.attendance.data.local` | Resolves offline/online sync collisions | ✅ Migrated |
| `StudentBiometricEntity.kt` | `features.biometric.data.local` | Native face embeddings and matching data | ✅ Migrated |
| `StudentClassAssignmentEntity.kt` | `features.student.data.local` | Link table between Student and Class (Composite PK) | ✅ Migrated |
| `ClassEntity.kt` | `features.school.data.local` | School class metadata (Grade, Name) | ✅ Migrated |
| `SchoolEntity.kt` | `features.school.data.local` | Multi-tenant school workspace identity | ✅ Migrated |
| `StudentEntity.kt` | `features.student.data.local` | Core student profile (Name, Photo URL) | ✅ Migrated |
| `AuditLogEntity.kt` | `features.reporting.data.local` | System traceability and action history | ✅ Migrated |
| `SubjectEntity.kt` | `features.session.data.local` | Academic subjects for attendance tracking | ✅ New (v3.3) |
| `ClassSessionEntity.kt` | `features.session.data.local` | Tiered sessions (Academic/Class/Global) | ✅ Tiered (v3.7) |

## 🧠 ViewModels (MVI Effect-Driven)
| File | Package | Responsibility |
|------|---------|---------------|
| `DashboardViewModel.kt` | `features.dashboard.ui` | Unified hub for Admin/Supervisor navigation |
| `AccountManagementViewModel.kt` | `features.account.ui.management` | User profile and school membership mgmt |
| `ClassViewModel.kt` | `features.school.ui.classes` | Management of school classes and assignments |
| `AttendanceViewModel.kt` | `features.attendance.ui` | Main scanning and manual entry logic |
| `StudentFormViewModel.kt` | `features.student.ui.form` | Single student profile management |
| `RegisterViewModel.kt` | `features.student.ui.bulk` | Bulk CSV student import engine |
| `StudentRosterViewModel.kt` | `features.student.ui.roster` | Roster browsing and filtering |
| `PendingSchoolsViewModel.kt` | `features.school.ui.admin` | Super Admin school verification flow |
| `AppUpdateViewModel.kt` | `features.update.ui` | Custom GitHub-based update engine state |
| `SessionManagementViewModel.kt` | `features.session.ui` | Tiered session and subject management |
| `SessionPickerViewModel.kt` | `features.session.ui` | Manual session selection engine |


## ⚡ UI Effects (Transient Event Stream)
| File | Package | Responsibility |
|------|---------|---------------|
| `DashboardUiEffect.kt` | `features.dashboard.ui` | Snackbars, Logout, and Navigation effects |
| `AccountUiEffect.kt` | `features.account.ui.management` | Toast/Snackbar feedback for account updates |
| `ClassUiEffect.kt` | `features.school.ui.classes` | Feedback for class creation/deletion |
| `SchoolUiEffect.kt` | `features.school.ui.list` | Workspace switching notifications |
| `AttendanceUiEffect.kt` | `features.attendance.ui` | Verification success/failure transient UI |
| `RegisterUiEffect.kt` | `features.student.ui.bulk` | CSV parsing status and result summary |
| `PendingSchoolsUiEffect.kt` | `features.school.ui.admin` | Approval/Rejection success messages |
| `SessionManagementUiEffect.kt` | `features.session.ui` | Session/Subject creation feedback |

## 🏰 Repositories (Interface/Impl DRY standard)
| File | Package | Responsibility | DRY Status |
|------|---------|---------------|------------|
| `AccountRepository.kt` | `features.account.domain.repository` | Account lifecycle and Firebase sync | ✅ asLocalResult |
| `SchoolRepository.kt` | `features.school.domain.repository` | Multi-tenant workspace and class mgmt | ✅ asLocalResult |
| `AttendanceRepository.kt` | `features.attendance.domain.repository` | Attendance tracking SSOT | ✅ asLocalResult |
| `StudentRepository.kt` | `features.student.domain.repository` | Student profile management | ✅ asLocalResult |
| `BiometricRepository.kt` | `features.biometric.domain.repository` | Native face engine and assignment logic | ✅ asLocalResult |
| `SessionRepository.kt` | `features.session.domain.repository` | Tiered session SSOT & management | ✅ asLocalResult |


---

## 🔗 Critical Data Flows (AI Navigation Map)

*   **Hybrid Multi-Class Sync:**
    1. `StudentFormViewModel` saves local `StudentClassAssignmentEntity`.
    2. `StudentRepositoryImpl.pushPendingProfiles` fetches ALL assigned `classIds` for a student from `assignmentDao`.
    3. Firestore **Class** document is updated with `studentIds` (Primary).
    4. Firestore **Student** document is updated with `classIds` (Fallback).
*   **Supervisor Onboarding:**
    1. `DashboardViewModel` detects `SUPERVISOR` role with empty `assignedClasses`.
    2. Displays `SupervisorOnboardingCard` → Navigates to `AssignClassScreen`.
    3. Supervisor selects classes; `AccountRepository` updates `memberships` map in Firestore.
    4. `AccountSyncWorker` pulls changes; Dashboard reactively updates session filters.
*   **Automatic Identity Healing:**
    1. `AccessSyncWorker` detects new biometrics without local Student profiles.
    2. Calls `StudentRepository.autoHealStudentIdentities` to fetch missing data from Firestore.
*   **Session Tiering Resolution:**
    1. Student scans barcode/face.
    2. `GetActiveTieredSessionUseCase` fetches all sessions for the current day.
    3. Hierarchy enforced: **GLOBAL > CLASS_WIDE > ACADEMIC**.
    4. Deterministic tie-breaker: `startTime ASC` -> `sessionId ASC`.
*   **Performance-Optimized Reporting:**
    1. Attendance records are saved with denormalized `sessionType`.
    2. `AttendanceRecordDao.getRecordsByTier` queries O(1) without JOINs.

## 🚨 Architectural Constraints (AI Safety)

*   **Flow Suffix Mandatory:** All reactive variables MUST end with `Flow` (e.g., `uiStateFlow`, `classesFlow`).
*   **English-First Policy:** Technical comments and code must be 100% English. Indonesian is reserved for user-facing string keys.
*   **NO Cascade Delete:** `StudentClassAssignmentEntity` MUST use `ForeignKey.NO_ACTION` to preserve data during class metadata syncs.
*   **Composite Primary Keys:** `StudentClassAssignmentEntity` uses `(studentId, classId)` to support many-to-many relationships.

## 📍 Quick-Reference Hotspots

| Task | Target Files |
| :--- | :--- |
| **Modify Navigation** | `core.navigation.NavigationRoutes` & `core.ui.navigation.graphs.*` |
| **Fix Database Schema** | `features.*.data.local.*Entity` |
| **Update Cloud Logic** | `features.*.data.repo.*RepositoryImpl` |
| **Adjust UI Layout** | `features.*.ui.*Screen` |
| **Change Permissions** | `core.util.PermissionUtils` |

## 📁 Project Documentation
- [GEMINI.md](file:///home/max/azuratime/nusantara-main/GEMINI.md) — Current project status, phase history, and AI instructions (Root)
- [index_project.md](file:///home/max/azuratime/nusantara-main/index_project.md) — This project file index (Root)
- [README.md](file:///home/max/azuratime/nusantara-main/docs/README.md) — Core overview, terminology, features, and setup instructions
- [CHANGELOG.md](file:///home/max/azuratime/nusantara-main/docs/CHANGELOG.md) — Version release logs
- [CONTRIBUTING.md](file:///home/max/azuratime/nusantara-main/docs/CONTRIBUTING.md) — AI-native contribution guidelines and standards
- [DEPLOYMENT.md](file:///home/max/azuratime/nusantara-main/docs/DEPLOYMENT.md) — Tag-based CI pipeline and manual deployment guide
- [RECOVERY_PROTOCOL.md](file:///home/max/azuratime/nusantara-main/docs/RECOVERY_PROTOCOL.md) — Database self-healing and emergency recovery guide
- [ROADMAP.md](file:///home/max/azuratime/nusantara-main/docs/ROADMAP.md) — Strategic future planning and phases
- [TROUBLESHOOTING.md](file:///home/max/azuratime/nusantara-main/docs/TROUBLESHOOTING.md) — Quick fixes for common development issues
- [SESSION_TIERING.md](file:///home/max/azuratime/nusantara-main/docs/SESSION_TIERING.md) — 3-Level hierarchy specification and session selection
- [MIGRATION_SUBJECT_SESSION.md](file:///home/max/azuratime/nusantara-main/docs/MIGRATION_SUBJECT_SESSION.md) — Database v16 to v17 migration and subject session plans
- [ARCHITECTURE.md](file:///home/max/azuratime/nusantara-main/docs/ARCHITECTURE.md) — Vertical Slice Architecture and data layer design
- [AI_NATIVE_TEMPLATE.md](file:///home/max/azuratime/nusantara-main/docs/AI_NATIVE_TEMPLATE.md) — Feature blueprint and Kotlin ViewModel/UI boilerplate
- [NAMING_CONVENTIONS.md](file:///home/max/azuratime/nusantara-main/docs/NAMING_CONVENTIONS.md) — Naming policies for semantic purity
- [VOCABULARY.md](file:///home/max/azuratime/nusantara-main/docs/VOCABULARY.md) — Terminology dictionary

