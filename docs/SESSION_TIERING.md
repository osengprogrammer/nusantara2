# 🏫 Session Tiering System (v3.7.0)
**Architectural Specification & Rollout Guide**

## 🎯 Overview
The Session Tiering System decouples attendance from specific academic classes, allowing schools to track attendance for three distinct levels of events:
1.  **GLOBAL**: School-wide events (e.g., Flag Ceremony, Seminars).
2.  **CLASS_WIDE**: General class sessions (e.g., Homeroom).
3.  **ACADEMIC**: Specific subject-based classes (Legacy behavior).

---

## 🏗️ Data Architecture

### 1. Database Schema (Room v24)
*   **`class_sessions`**: 
    *   `sessionType`: [ACADEMIC, CLASS_WIDE, GLOBAL]
    *   `lookupKey`: Prefixed with tier (e.g., `GLOBAL_SCH001_ALL_1_0630`)
*   **`check_in_records`**:
    *   `sessionType`: Denormalized column for O(1) reporting.

### 2. Resolution Hierarchy
When a student scans their barcode/face, the system resolves the active session using the following deterministic priority:
**GLOBAL > CLASS_WIDE > ACADEMIC**

---

## 🔄 Migration & Compatibility
*   **Legacy Data**: Auto-migrated to `ACADEMIC` tier during `MIGRATION_22_23`.
*   **Prefixing**: All existing `lookupKey` values were prefixed with `ACADEMIC_` to maintain consistency.
*   **Denormalization**: `MIGRATION_23_24` added the `sessionType` column to attendance records for performance.

---

## 🛠️ Implementation Details

### MVI UI Strategy
*   `SessionManagementScreen` uses dynamic forms.
*   `AddSessionDialog` adapts based on `SessionType`:
    *   Academic: Shows Subject & Class.
    *   Class-Wide: Shows Class only.
    *   Global: Hides both (inherits school context).

### Reporting
*   New DAO methods: `getRecordsByTier` and `getTierSummaryCount`.
*   Supports filtering reports by session tier without expensive JOIN operations.

---

## 🛡️ Stability & Rollout
*   **Feature Flag**: Integrated via `SessionManager`.
*   **Tests**: Exhaustive coverage in `GetActiveTieredSessionUseCaseTest`.
*   **Rollout**: v3.7.0 release candidate verified with 100% test pass rate.
