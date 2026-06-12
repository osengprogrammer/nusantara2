# 📋 Session Tiering System: 3-Level Hierarchy Plan
**Version:** v3.7.0-base (Hardened)
**Status:** Approved / Planning Phase

## 🎯 Vision & Scope
| Item | Description |
| :--- | :--- |
| **Goal** | Support 3 session types with clear resolution hierarchy, backward-compatible migration, and clean reporting. |
| **Non-Goal** | No changes to AzuraEngine, no breaking changes to existing attendance flow, no kiosk code reintroduced. |
| **Principles** | Room v23 sovereign, Flat VSA, Effect-Driven MVI, English-first, Supervisor-centric terminology. |

---

## 🧱 Data Model Refinements

### 🔹 SessionType Enum (Domain)
```kotlin
enum class SessionType {
    ACADEMIC,    // Class + Subject (Default)
    CLASS_WIDE,  // Class only (e.g., Homeroom, Class Ceremony)
    GLOBAL       // School-wide (e.g., Flag Ceremony, General Seminar)
}
```

### 🔹 Entity Update (`ClassSessionEntity`)
*   **Nullable Strategy**: `classId` and `subjectId` become nullable.
*   **Defaulting**: New column `sessionType` with default value `ACADEMIC`.

### 🔹 LookupKey Format (Collision Prevention)
| SessionType | Prefix | LookupKey Example |
| :--- | :--- | :--- |
| **ACADEMIC** | `ACADEMIC_` | `ACADEMIC_cls10A_subMath_1_0730` |
| **CLASS_WIDE** | `CLASS_` | `CLASS_cls10A_ALL_1_0700` |
| **GLOBAL** | `GLOBAL_` | `GLOBAL_SCH001_ALL_1_0630` |

---

## 🔄 Resolution Hierarchy (Business Logic)
When resolving the active session during scanning:
1.  **Check GLOBAL**: Is there a school-wide event? (If yes, use this).
2.  **Check CLASS_WIDE**: Is there a class-specific general session? (If no Global).
3.  **Check ACADEMIC**: Use the specific subject session.

**Tie-breaker**: If times overlap within the same tier, use `startTime ASC` -> `sessionId ASC`.

---

## 📋 Phase Breakdown

### Phase 1: Schema & Migration
*   Implement `SessionType` enum.
*   Update `ClassSessionEntity` with nullable fields and `sessionType`.
*   Implement `MIGRATION_22_23`.
*   Update `lookupKey` generation logic in `CreateSessionUseCase`.

### Phase 2: Domain Logic
*   Create `GetActiveTieredSessionUseCase` with resolution hierarchy.
*   Update `SessionRepository` and `SessionDao` (LEFT JOIN for subjects).

### Phase 3: UI & Management
*   Update `SessionManagementScreen` with type selector.
*   Conditional field rendering (derivedStateOf).
*   Add Tier Badges to session cards.

### Phase 4: Reporting & Polish
*   Filter support by session type in reports.
*   Unit tests for resolution hierarchy.
*   Documentation update.

---

## 🛡️ Critical Refinements
*   **Database**: Enforce `DEFAULT 'ACADEMIC'` in migration to handle legacy data safely.
*   **Queries**: Use `LEFT JOIN` for `subjects` and `classes` table to ensure GLOBAL/CLASS sessions are not dropped when subjectId/classId is null.
*   **UI**: Use `remember` + `derivedStateOf` to prevent layout jumps during conditional field switching.

---

## 🚀 Rollout Strategy
Implementation will be protected by an internal flag (if necessary) or implemented directly as a structural upgrade. Legacy data will be auto-migrated to the `ACADEMIC` type with the new prefixed `lookupKey` format to maintain integrity.
