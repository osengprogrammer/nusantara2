# 🏗️ AzuraTime Architecture Guide (v3.2.1-ai-native)

## 📐 Terminology & Language Policy
To ensure codebase consistency and maximize AI comprehension speed, the following policies are strictly enforced:

### 🔹 Language Policy (English-First)
- **Codebase:** 100% Common English for Variables, Functions, Classes, and Comments.
- **Documentation:** 100% Common English for all Markdown files.
- **Reasoning:** Aligns with global Kotlin/Android standards and ensures AI agents never "hallucinate" translations.

### 🔹 Standardized Vocabulary
- **Account**: The unified identity model. Replaces legacy "User" and "Staff" variant terminology.
- **Supervisor**: An Account with management access to specific classes (formerly "Teacher").
- **Admin**: An Account with full management rights for a School.
- **Student**: Refers to the person being recorded/tracked.
- **Biometric**: Refers to face embeddings and enrollment data.
- **Assignment**: The link between a Student and a Class.
- **Membership**: The link between an Account and a School.

### 🔑 Authorization & Role Management
Role checks MUST use the `AccountRole` enum via `PermissionUtils` extension functions:
- `isAdmin(schoolId)`: Returns true for ADMIN or SUPER_ADMIN in the school.
- `isSupervisorOf(schoolId, classId)`: Returns true if assigned to that specific class.
- `canAccessClass(schoolId, classId)`: The primary check for attendance operations.

---

## 🎯 Core Design Patterns

### 🧩 Vertical Slice Architecture (VSA)
AzuraTime is built on **Vertical Slices** (Feature-First), ensuring that each feature contains its own UI, Domain, and Data layers. Cross-feature dependencies are strictly managed through `core` modules.

### ⚡ Effect-Driven MVI Standard
All ViewModels must strictly follow the state-event-effect pattern:
- **`UiState`**: A single `StateFlow` representing the persistent view state.
- **`UiEvent`**: A sealed class representing user intentions.
- **`UiEffect`**: A `SharedFlow` for transient events (Snackbars, Navigation, Toasts).
- **Naming Rule**: All Flow variables MUST end with the `Flow` suffix (e.g., `uiStateFlow`, `studentsFlow`).

### ☁️ Hybrid Sync Strategy (Multi-Class Persistence)
To ensure robust student-to-class assignments across offline/online cycles:
1. **Local SSOT**: Composite primary key `(studentId, classId)` in `StudentClassAssignmentEntity`.
2. **Cloud Source**: Primary source is `studentIds` array in Firestore **Class** documents.
3. **Cloud Fallback**: Secondary source is `classIds` array in Firestore **Student** documents.
4. **Push Rule**: When pushing profiles, the full list of assignments must be fetched from the DAO to prevent partial overwrites.

---

## 📁 Project Structure

```text
app/src/main/java/com/azuratech/azuratime/
├── core/               # 🏗️ Shared Infrastructure
│   ├── boot/           # Startup and activation logic
│   ├── data/           # Shared DAOs and global AppDatabase
│   ├── di/             # Hilt dependency injection modules
│   ├── domain/         # Shared models (SyncStatus, AccountRole)
│   ├── navigation/     # Global routes and NavHost
│   ├── security/       # C++ Secure Engine and Security Vault
│   ├── session/        # Encrypted Session Manager
│   ├── sync/           # WorkManager background workers
│   └── util/           # Common utils (PermissionUtils, DateUtils)
├── features/           # 🧩 Business Vertical Slices
│   ├── account/        # Profile, Memberships, Connections
│   ├── ai/             # Zohar AI & Rule-based Engines
│   ├── attendance/     # Capture (Face/Barcode), History, Matrix
│   ├── auth/           # Firebase Auth & Session state
│   ├── biometric/      # Enrollment, Matching, Face Local DB
│   ├── dashboard/      # Unified Admin/Supervisor Landing
│   ├── reporting/      # Audit Logs, CSV/PDF Export
│   └── student/        # Roster, Forms, Assignments
```

---

## 🚀 Onboarding & Provisioning Flow

### 1. Account Activation (Cloud Function Driven)
Registration follows a secure, centralized flow:
1. **Signup**: User signs in via Google; App pushes a PENDING stub to `memberships/{uid}`.
2. **Approval**: Super Admin sets status to `ACTIVE` in Firestore.
3. **Cloud Function**: `onregistrationapproved` (Node.js) generates secure keys, seeds the default school, and migrates the account to `whitelisted_accounts`.
4. **App Sync**: `MembershipViewModel` pulls the final state and unlocks the Dashboard.

### 2. Supervisor Onboarding (Class Selection)
If an Account has the `SUPERVISOR` role but no classes assigned:
1. **Dashboard Alert**: An "Action Required" card appears prompting for class selection.
2. **Assignment UI**: The Supervisor selects classes; the app updates `assignedClassIds` in the local Membership and pushes to Cloud.
3. **Reactive Refresh**: The Dashboard automatically filters attendance sessions based on the new assignments.

---

## 🏷️ Naming Conventions

| Component | Suffix | Example | Location |
| :--- | :--- | :--- | :--- |
| **ViewModel** | `ViewModel` | `StudentFormViewModel` | `features/*/ui/` |
| **State** | `UiState` | `DashboardUiState` | `features/*/ui/` |
| **Effect** | `UiEffect` | `AccountUiEffect` | `features/*/ui/` |
| **Entity** | `Entity` | `ClassEntity` | `features/*/data/local/` |
| **Repository** | `Repository` | `AccountRepository` | `features/*/domain/repository/` |
| **Reactive** | `Flow` | `studentsFlow` | Anywhere |

---

## 📈 Quality Gates
1.  **Strict English**: No Indonesian in code or technical comments.
2.  **Flow Suffix**: Every `Flow<T>` or `StateFlow<T>` must end in `Flow`.
3.  **Result Wrapper**: All repository write operations must return `Result<T>`.
4.  **Permission Check**: Use `PermissionUtils` instead of checking raw role strings.
5.  **MVI Purity**: Use `UiEffect` for Snackbars and Navigation.

---

### Version History
| Version | Date | Description | Contributor |
| :--- | :--- | :--- | :--- |
| 3.2.1 | 2026-06-05 | English-First Policy, Effect-Driven MVI, Hybrid Sync | Gemini CLI |
| 1.0 | 2026-05-12 | Initial VSA Architecture Release | Gemini CLI |
