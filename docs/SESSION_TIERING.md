# 🏫 Session Tiering System (v3.7.0)
**Architectural Specification, 3-Level Hierarchy Plan & Rollout Guide**

## 🎯 Overview & Scope
The Session Tiering System decouples attendance from specific academic classes, allowing schools to track attendance for three distinct levels of events:
1.  **GLOBAL**: School-wide events (e.g., Flag Ceremony, Seminars).
2.  **CLASS_WIDE**: General class sessions (e.g., Homeroom).
3.  **ACADEMIC**: Specific subject-based classes (Legacy behavior).

| Item | Description |
| :--- | :--- |
| **Goal** | Support 3 session types with clear resolution hierarchy, backward-compatible migration, and clean reporting. |
| **Non-Goal** | No changes to AzuraEngine, no breaking changes to existing attendance flow, no kiosk code reintroduced. |
| **Principles** | Room v23/v24 sovereign, Flat VSA, Effect-Driven MVI, English-first, Supervisor-centric terminology. |

---

## 🏗️ Data Architecture

### 1. Database Schema & Refinements (Room v24)
*   **`class_sessions`**:
    *   `sessionType` column: Contains `ACADEMIC`, `CLASS_WIDE`, or `GLOBAL`. Defaults to `ACADEMIC` to handle legacy data safely.
    *   `lookupKey` column: Prefixed with tier (e.g., `GLOBAL_SCH001_ALL_1_0630`).
    *   `classId` and `subjectId` become nullable for `GLOBAL` or `CLASS_WIDE` tiers.
*   **`check_in_records`**:
    *   `sessionType`: Denormalized column for O(1) reporting.

#### 🔹 SessionType Enum (Domain)
```kotlin
enum class SessionType {
    ACADEMIC,    // Class + Subject (Default)
    CLASS_WIDE,  // Class only (e.g., Homeroom, Class Ceremony)
    GLOBAL       // School-wide (e.g., Flag Ceremony, General Seminar)
}
```

#### 🔹 LookupKey Format (Collision Prevention)
| SessionType | Prefix | LookupKey Example |
| :--- | :--- | :--- |
| **ACADEMIC** | `ACADEMIC_` | `ACADEMIC_cls10A_subMath_1_0730` |
| **CLASS_WIDE** | `CLASS_` | `CLASS_cls10A_ALL_1_0700` |
| **GLOBAL** | `GLOBAL_` | `GLOBAL_SCH001_ALL_1_0630` |

---

## 🔄 Resolution Hierarchy (Business Logic)
When a student scans their barcode/face, the system resolves the active session using the following deterministic priority:
1.  **Check GLOBAL**: Is there a school-wide event? (If yes, use this).
2.  **Check CLASS_WIDE**: Is there a class-specific general session? (If no Global).
3.  **Check ACADEMIC**: Use the specific subject session.

**Tie-breaker**: If times overlap within the same tier, use `startTime ASC` -> `sessionId ASC`.

---

## 🔄 Migration & Compatibility
*   **Legacy Data**: Auto-migrated to `ACADEMIC` tier during `MIGRATION_22_23`.
*   **Prefixing**: All existing `lookupKey` values were prefixed with `ACADEMIC_` to maintain consistency.
*   **Denormalization**: `MIGRATION_23_24` added the `sessionType` column to attendance records for performance.

---

## 🛠️ Implementation Details

### Session Selection (Picker)
*   **Today's Resolution**: Dashboard automatically resolves the active session for the current day/time.
*   **Manual Selection**: The `SessionPickerScreen` provides a full view of **ALL** scheduled sessions for the school, sorted by day and time. This ensures supervisors can pick sessions for makeup classes or early starts regardless of the current day.
*   **Visual Cues**: Each session in the picker displays its **Day Name**, **Time Range**, and **Tier Badge** for unambiguous selection.

### Reporting
*   New DAO methods: `getRecordsByTier` and `getTierSummaryCount`.
*   Supports filtering reports by session tier without expensive JOIN operations (queries run in O(1) without JOINs).

---

## 🛡️ Stability & Rollout
*   **Feature Flag**: Integrated via `SessionManager`.
*   **Tests**: Exhaustive coverage in `GetActiveTieredSessionUseCaseTest`.
*   **Rollout**: v3.7.0 release candidate verified with 100% test pass rate.
*   **Database Queries**: Use `LEFT JOIN` for `subjects` and `classes` table to ensure GLOBAL/CLASS sessions are not dropped when subjectId/classId is null.
