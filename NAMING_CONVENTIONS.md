# 📐 NUSANTARA v3.2.2 — AI-OPTIMIZED NAMING CONVENTIONS
⚡ *Mandatory for all AI prompts & code generation.*

## 🔹 LANGUAGE POLICY (STRICT)
- **Codebase:** 100% Common English (Variables, Functions, Classes, Comments).
- **Documentation:** 100% Common English (README, ARCHITECTURE, GEMINI.md).
- **UI Strings:** Localized (Indonesian for end-users), but Keys must be in English.
- **Reasoning:** Maximizes AI comprehension speed and aligns with global Kotlin/Android standards.

## 🔹 TERMINOLOGY POLICY (v3.2.2 - PHASE 30)
- **Account**: The unified identity model. NO "User", "Staff", or "Teacher" variants allowed for app identity.
- **Student**: Refers to the person being recorded/tracked.
- **Supervisor**: An Account with limited access to specific classes (formerly "Teacher").
- **Admin**: An Account with full management rights for a School.
- **Biometric**: Refers to face embeddings and enrollment data.
- **Assignment**: The link between a Student and a Class (`StudentClassAssignmentEntity`).
- **Membership**: The link between an Account and a School.

## 🔹 FILE & CLASS NAMING
- Files: `PascalCase.kt` → `FaceRepository.kt`, `CheckInViewModel.kt`
- Classes/Objects: `PascalCase` → `class AttendanceViewModel`
- Interfaces: `PascalCase` (No 'I' prefix) → `interface StudentRepository`
- Implementations: `PascalCaseImpl` → `class StudentRepositoryImpl`
- MVI Contracts: MUST use `UiState`, `UiEvent`, and `UiEffect` suffixes → `LoginUiState`, `LoginUiEvent`, `LoginUiEffect`

## 🔹 FUNCTION NAMING
- Functions: `camelCase` (verb-first) → `fun calculateAttendance()`
- Composables (UI Components): `PascalCase` → `@Composable fun StudentRow()`
- Composables (Top-Level Screens): MUST end in `Screen` suffix → `@Composable fun DashboardScreen()`, `@Composable fun AttendanceCaptureScreen()`
- Mappers: `toDomain()`, `toEntity()`, `toProfile()`

## 🔹 VARIABLE & FLOW NAMING
- Variables: `camelCase` → `val studentName`
- Flow/StateFlow: MUST end with `Flow` suffix → `val uiStateFlow`, `val studentsFlow`
- Constants: `UPPER_SNAKE_CASE` → `const val MAX_RETRIES = 3`
- Routes: `camelCase` string values → `const val ATTENDANCE_CAPTURE = "attendanceCapture"`

## 🔹 REPOSITORY PATTERN (SSOT)
- Interfaces live in `domain.repository`
- Implementations live in `data.repo`
- All write operations MUST return `Result<T>`
- Read operations MUST return `Flow<Result<T>>` or `Flow<T>` if reactive SSOT

## 🚨 ARCHITECTURAL CONSTRAINTS (AI SAFETY)
1. **NO CASCADE DELETE:** `StudentClassAssignmentEntity` uses `ForeignKey.NO_ACTION`.
2. **COMPOSITE KEYS:** `StudentClassAssignmentEntity` uses `(studentId, classId)` as primary key.
3. **FULL LIST SYNC:** `pushPendingProfiles` MUST fetch `classIds` from `assignmentDao`, not local Student entity.
4. **ROLE CHECKS:** Use `PermissionUtils` (e.g., `isAdmin()`, `isSupervisorOf()`) instead of raw string checks.

## 🤖 AI PROMPT TEMPLATE
"Follow NAMING_CONVENTIONS.md. Use 100% Common English for code. Use 'Account' for identity, 'Supervisor' for teachers. Suffix Flows with 'Flow'. Interface/Impl separation for repositories. Zero underscores in Kotlin code."
