# 📅 Attendance Feature Rules
1.  **Strict SSOT**: All history screens must observe `AttendanceRecordEntity` directly from Room via `AttendanceRepository`. Never use intermediate UseCases for data reading.
2.  **Conflict Resolution**: Collisions are handled locally via `AttendanceConflictDao` and pushed via `SyncManager` as a background side-effect.
3.  **Terminology**: Always use `Attendance` (never `CheckIn`) and `Account` (never `Staff`/`Teacher`).

## 🔒 Type Safety Guidelines
Because we are restricted to `String` primitives for IDs due to Room/KSP constraints, **you MUST explicitly name variables with their ID type.**
- ✅ CORRECT: `fun checkIn(schoolId: String, studentId: String)`
When passing parameters, explicitly name them in the function call: `checkIn(schoolId = currentSchoolId, studentId = currentStudentId)` to prevent hallucinated swaps.
