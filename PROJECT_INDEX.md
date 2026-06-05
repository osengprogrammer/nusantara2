# 🗂️ AzuraTime — PROJECT FILE INDEX (v3.2.1-ai-native)
⚡ *Status: 100% English-First & Effect-Driven Architecture Standardized.*

## 💾 Room Entities (Local SSOT)
| File | Package | Responsibility | SSOT Status |
|------|---------|---------------|-------------|
| `AccountEntity.kt` | `features.account.data.local` | Master account identity; includes global role | ✅ Migrated |
| `AccessRequestEntity.kt` | `features.account.data.local` | Tracks join/leave school requests | ✅ Migrated |
| `AttendanceRecordEntity.kt` | `features.attendance.data.local` | Individual attendance logs (scanned/manual) | ✅ Migrated |
| `AttendanceConflictEntity.kt` | `features.attendance.data.local` | Resolves offline/online sync collisions | ✅ Migrated |
| `StudentBiometricEntity.kt` | `features.biometric.data.local` | Native face embeddings and matching data | ✅ Migrated |
| `StudentClassAssignmentEntity.kt` | `features.biometric.data.local` | Link table between Student and Class (Composite PK) | ✅ Migrated |
| `ClassEntity.kt` | `features.school.data.local` | School class metadata (Grade, Name) | ✅ Migrated |
| `SchoolEntity.kt` | `features.school.data.local` | Multi-tenant school workspace identity | ✅ Migrated |
| `StudentEntity.kt` | `features.student.data.local` | Core student profile (Name, Photo URL) | ✅ Migrated |
| `AuditLogEntity.kt` | `features.reporting.data.local` | System traceability and action history | ✅ Migrated |

## 🧠 ViewModels (MVI Effect-Driven)
| File | Package | Responsibility |
|------|---------|---------------|
| `DashboardViewModel.kt` | `features.dashboard.ui` | Unified hub for Admin/Supervisor navigation |
| `AccountManagementViewModel.kt` | `features.account.ui.management` | User profile and school membership mgmt |
| `ClassViewModel.kt` | `features.school.ui.classes` | Management of school classes and assignments |
| `AttendanceViewModel.kt` | `features.attendance.ui` | Main scanning and manual entry logic |
| `StudentFormViewModel.kt` | `features.student.ui.form` | Single student enrollment/editing |
| `RegisterViewModel.kt` | `features.student.ui.bulk` | Bulk CSV student import engine |
| `StudentRosterViewModel.kt` | `features.student.ui.roster` | Roster browsing and filtering |
| `PendingSchoolsViewModel.kt` | `features.school.ui.admin` | Super Admin school verification flow |
| `AppUpdateViewModel.kt` | `features.update.ui` | Custom GitHub-based update engine state |

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

## 🏰 Repositories (Interface/Impl DRY standard)
| File | Package | Responsibility | DRY Status |
|------|---------|---------------|------------|
| `AccountRepository.kt` | `features.account.domain.repository` | Account lifecycle and Firebase sync | ✅ asLocalResult |
| `SchoolRepository.kt` | `features.school.domain.repository` | Multi-tenant workspace and class mgmt | ✅ asLocalResult |
| `AttendanceRepository.kt` | `features.attendance.domain.repository` | Attendance tracking SSOT | ✅ asLocalResult |
| `StudentRepository.kt` | `features.student.domain.repository` | Student profile management | ✅ asLocalResult |
| `BiometricRepository.kt` | `features.biometric.domain.repository` | Native face engine and assignment logic | ✅ asLocalResult |

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
