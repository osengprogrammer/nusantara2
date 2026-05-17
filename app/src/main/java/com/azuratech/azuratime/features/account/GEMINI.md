# 👤 Account Feature Rules
1.  **Entity**: `AccountEntity` is the absolute SSOT for user data, role, and workspace selection.
2.  **Roles**: Roles are determined dynamically per school workspace via `memberships[schoolId]?.role`. The fallback role is `"ACCOUNT"`.
3.  **Terminology**: Never use `Staff`, `Teacher`, or `User` in UI components or variable names. Always use `Account` or `AccountEntity`.

## 🔒 Type Safety Guidelines
Because we are restricted to `String` primitives for IDs due to Room/KSP constraints, **you MUST explicitly name variables with their ID type.**
- ✅ CORRECT: `fun enroll(schoolId: String, studentId: String)`
- ❌ INCORRECT: `fun enroll(school: String, student: String)`
- ❌ INCORRECT: `fun enroll(id1: String, id2: String)`
When passing parameters, explicitly name them in the function call: `enroll(schoolId = currentSchoolId, studentId = currentStudentId)` to prevent hallucinated swaps.
