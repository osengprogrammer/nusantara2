# 🏗️ AzuraTime Architecture Guide (v3.7.0-base)

## 🤖 AI Prime Directives
These directives are **absolute mandates** for all AI development within this codebase. They govern system predictability, state integrity, and codebase consistency.

### 1. Strict MVI Adherence & UI Purity
All ViewModels must strictly adopt a pure, unidirectional data flow (UDF) contract:
- **Single State Source**: A single, immutable `UiState` representing the entire view state, exposed as a single `StateFlow`. No multiple independent/uncoupled live data or state variables.
- **Flow Naming Policy**: Every variable of type `Flow`, `StateFlow`, or `SharedFlow` MUST end with the `Flow` suffix (e.g., `uiStateFlow`, `studentsFlow`).
- **Intent Expression**: All user actions or background triggers must map to a sealed class `UiEvent` and be dispatched solely via `onEvent(UiEvent)`.
- **No Ghost States (Effect-Driven MVI)**: Transient occurrences (Toasts, Snackbars, Alerts, Navigation actions) must never be mutated or stored in the persistent `UiState` to avoid UI state pollution. They must be broadcast using a dedicated `SharedFlow<UiEffect>` and collected on the UI layer as lifecycle-aware side effects.

### 2. Local-First SSOT (Single Source of Truth)
To guarantee robust, zero-downtime operations in offline or low-connectivity environments:
- **Local Room DB is Sovereign**: The local database is the absolute source of truth for all query and mutation flows.
- **Decoupled Cloud Operations**: Under no circumstances should UI layers subscribe directly to or update raw Firestore/cloud endpoints. All reads query Room; all writes commit to Room and flag data as `isSynced = false`.
- **Mediated Sync Workers**: Distinct, dedicated `WorkManager` and sync workers are solely responsible for bidirectional harmonization between the local SQLite DB and Firestore.

---

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
- **Session**: A time-bound attendance event. Tiers: **Global** (School-wide), **Class-Wide** (Homeroom), and **Academic** (Subject-specific).
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

### 🧩 Vertical Slice Architecture (VSA) & Feature Boundaries
AzuraTime is structured in cohesive **Vertical Slices** (Feature-First) to keep features self-contained. To prevent architectural decay, strict package boundaries are enforced:
1. **Zero Cross-Feature Dependencies**: Slices located in `features/` (e.g., `features/student/`, `features/attendance/`) MUST NOT import or directly depend on components, ViewModels, XML files, local DB entities, or business logic from other slices.
2. **Core as the Shared Bridge**: All inter-feature communications, common data models (e.g., `SyncStatus`, `AccountRole`), shared Room definitions, security operations, and utility libraries must reside in the `core/` module.
3. **Encapsulated Data & Domain layers**: Local DB tables, DAOs, custom use-cases, and domain repositories unique to a feature slice (e.g., reporting engine, face matching, custom update logic) must remain strictly scoped inside their respective package (e.g., `features/reporting/`, `features/biometric/`).

### ⚡ Explicit Error Handling (Result-Oriented Architecture)
All repository write operations, cloud updates, and complex business logic functions must never bubble up raw exceptions or return ambiguous nullable values.
1. **AppError Subclasses**: Use `Result.Failure(AppError)` explicitly with specialized errors:
   - `AppError.Network(message)`: For Firestore, HTTP client, or connectivity errors.
   - `AppError.LocalDB(message)`: For SQLite constraint exceptions, insertion failures, or DB corruption.
   - `AppError.BusinessRule(message)`: For validation issues, expired/invalid actions, or permission checks.
   - `AppError.Unknown(message)`: For catch-all standard exceptions.
2. **Stream Transformation**: Map cold Kotlin `Flow` streams of database entities or models to secure Result flows using `asResult()` or `asLocalResult()` operators.
3. **ViewModel Exhaustiveness**: ViewModels must handle both `Result.Success` and `Result.Failure` flows. Failures must be processed and explicitly routed to the user using the `UiEffect` channel (e.g., alerting via snackbar or modal) rather than failing silently.

### ☁️ Hybrid Sync Strategy (Multi-Class Persistence)
To ensure robust student-to-class assignments across offline/online cycles:
1. **Local SSOT**: Composite primary key `(studentId, classId)` in `StudentClassAssignmentEntity`.
2. **Cloud Source**: Primary source is `studentIds` array in Firestore **Class** documents.
3. **Cloud Fallback**: Secondary source is `classIds` array in Firestore **Student** documents.
4. **Push Rule**: When pushing profiles, the full list of assignments must be fetched from the DAO to prevent partial overwrites.

### 📊 Performance-Optimized Denormalization
To ensure high-performance reporting for tiered sessions:
1. **Denormalized sessionType**: The `sessionType` is stored directly in the `check_in_records` table.
2. **JOIN-Free Queries**: Reporting filters by tier use O(1) indexed lookups instead of expensive multi-table JOINs.

### 🔄 Sync Engine Protocols (Atomic Reconciliation)
To guarantee robust data reconciliation and offline-first durability in extremely low-connectivity environments:
1. **Sequential 1-2-3 Dependency Chain**: The core background sync worker (`SyncWorker`) executes its synchronization pipeline in a strict, sequential, transaction-style order. If any stage fails due to a network connection drop or network timeout, the worker immediately triggers WorkManager's exponential retry mechanism without executing subsequent steps:
   - **Stage 1 (Biometrics)**: `biometricRepository.syncBiometrics()` (Sync student face templates).
   - **Stage 2 (Assignments)**: `biometricRepository.syncAssignments()` (Sync student-class assignments).
   - **Stage 3 (Records)**: `attendanceRepository.syncRecords()` (Sync raw offline check-in logs).
2. **Biometric Overwrite Prevention Lock**: To protect offline supervisors from data loss during a down-sync, `StudentBiometricRepositoryImpl.syncBiometrics` executes a concurrency safety lock check on every incoming remote template. If a local biometric record exists, has not yet synced to the cloud (`!localRecord.isSynced`), and has a newer local modification timestamp than the incoming remote update (`localRecord.lastUpdated > remoteRecord.lastUpdated`), the sync engine blocks the incoming remote payload, preserving the supervisor's fresh offline biometric changes.

---

## 🚀 Stabilized Boot State Machine
The boot sequence acts as an ultra-stable, concurrency-safe gatekeeper during rapid startup, login, and logout events.

### 1. Boot States (`BootUiState`)
The state machine advances through a strict set of deterministic states:
- **`Loading`**: The application is verifying credentials and decrypting active session databases.
- **`Auth`**: No authenticated account exists; instantly transitions to authentication flows.
- **`NeedActivation`**: Account is authenticated, but lacks an active/approved school membership, or requires Supervisor-specific setup (e.g., class assignments).
- **`Ready`**: Session is verified, membership is fully active, and the main dashboard can safely render.
- **`Error`**: A critical initialization error occurred, displaying a clean recovery UI.

### 2. Concurrency Stabilization Strategies
To avoid asynchronous race conditions, redundant queries ("ghost coroutines"), and UI flickers:
- **Job Hygiene**: Active authentication check jobs are immediately cancelled (`authCheckJob?.cancel()`) prior to launching a fresh verification sequence.
- **Early Exit Flags**: Synchronously verify state exits. If a logout is currently active, background tasks and coroutines are aborted immediately.
- **Synchronous Validation**: If `getCurrentAccountId()` is null, the machine forces routing to `BootUiState.Auth` synchronously, avoiding spinning up async coroutines.
- **Double-Lock Guarding**: When kicking off asynchronous auth checking, after a delay (ensuring secure hardware cryptographic keys and room DB connections are stable), the state machine double-checks active logout and session states before invoking repository calls.

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
│   ├── session/        # 📅 Tiered Session & Subject Management
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
3.  **Result Wrapper**: All repository write operations, network calls, and business logic validations must return `Result<T>`.
4.  **Failure Handling**: Never return null or throw raw exceptions; wrap failures in specialized `Result.Failure(AppError)`.
5.  **Permission Check**: Use `PermissionUtils` instead of checking raw role strings.
6.  **MVI Purity**: Strict State + Event + Effect. Use `UiEffect` for transient visual elements (Toasts, Snackbars) and Navigation.
7.  **Feature Boundaries**: No cross-imports between `features/*`. Use `core/` for shared contracts.

---

### Version History
| Version | Date | Description | Contributor |
| :--- | :--- | :--- | :--- |
| 3.7.0 | 2026-06-12 | Session Tiering System, Resolution Hierarchy, denormalized sessionType | Gemini CLI |
| 3.2.2 | 2026-06-08 | AI Prime Directives, Explicit Error Handling, Feature Boundaries, Boot State Machine | Gemini CLI |
| 3.2.1 | 2026-06-05 | English-First Policy, Effect-Driven MVI, Hybrid Sync | Gemini CLI |
| 1.0 | 2026-05-12 | Initial VSA Architecture Release | Gemini CLI |
