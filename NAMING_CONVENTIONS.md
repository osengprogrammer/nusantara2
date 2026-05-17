# 📐 NUSANTARA v3.2 — AI-OPTIMIZED NAMING CONVENTIONS
⚡ *Mandatory for all AI prompts & code generation.*

## 🔹 TERMINOLOGY POLICY (v3.2.0)
- **Account**: The unified identity model. NO "User", "Staff", or "Teacher" variants allowed for app identity.
- **Student**: Refers to the person being recorded (e.g., student in school context).
- **Biometric**: Refers to face embeddings and enrollment data.

## 🔹 FILE & CLASS NAMING
- Files: `PascalCase.kt` → `FaceRepository.kt`, `CheckInViewModel.kt`
- Classes/Objects: `PascalCase` → `class AttendanceViewModel`
- Interfaces: `PascalCase` (No 'I' prefix) → `interface StudentRepository`
- Implementations: `PascalCaseImpl` → `class StudentRepositoryImpl`

## 🔹 FUNCTION NAMING
- Functions: `camelCase` (verb-first) → `fun calculateAttendance()`
- Composables: `PascalCase` → `@Composable fun StudentRow()`
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

## 🤖 AI PROMPT TEMPLATE
"Follow NAMING_CONVENTIONS.md. Use 'Account' for app identity, suffix Flows with 'Flow', use camelCase routes. Interface/Impl separation for repositories. Zero underscores in Kotlin code."
