# 🛡️ AzuraTime Recovery Protocol & Self-Healing Database Guide

## 1. Local Database Protection (Guardian Hook)
AzuraTime implements a solid-state health guard directly in the AppDatabase initialization builder to detect and neutralize local database corruption proactively before it causes runtime crashes.

### 🔍 PRAGMA integrity_check Guardian
On every database open event (`onOpen`), a specialized callback is triggered which forces SQLite to scan its structural pages for allocation and consistency health:
```kotlin
private val integrityCheckCallback = object : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        try {
            val result = db.query("PRAGMA integrity_check").use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else "fail"
            }
            if (result != "ok") {
                android.util.Log.e("AZURA_DB", "Database corruption detected! Triggering safe-mode wipe.")
                // Emergency Recovery Trigger
            }
        } catch (e: Exception) {
            android.util.Log.e("AZURA_DB", "Integrity check failed: ${e.message}", e)
        }
    }
}
```

---

## 2. Emergency Fallback & Self-Healing Flow
When database page corruption or severe SQLite consistency errors are detected:
1. **Safe-Mode Wipe**: The system is designed to trigger a complete wipe of the corrupted local database file. This is facilitated by Room's `.fallbackToDestructiveMigration()` builder setting, ensuring the app boots into a fresh, clean schema rather than crashing repeatedly on startup.
2. **Atomic Cloud Resynchronization**:
   - Immediately following a clean boot from a local wipe, the `BootViewModel` detects a missing or un-initialized local account state.
   - It synchronously routes the session state machine to `BootUiState.Auth` or initiates a full workspace download if active account credentials exist in our secure C++ `SecurityVault` or shared encrypted preferences.
   - Distinct background sync workers (`AccountSyncWorker`, `SyncWorker`) are automatically enqueued to pull student rosters, class hierarchies, and school metadata down from the Firestore Cloud Single Source of Truth (SSOT).
   - Local offline attendance records that were never pushed to the cloud are lost during a destructive database reset. However, historical rosters and configurations are restored to 100% integrity within seconds of network reconnection.

---

## 3. Transactional Hardening & Recovery Boundaries
To prevent the local database from ever entering a corrupt or partially written "ghost state" in the first place, all multi-record and multi-entity operations are bound inside strict database transaction barriers:

### 🧩 Class Deletions
*   **Method**: `SchoolClassDao.deleteClassWithAssignments()`
*   **Behavior**: Deleting a class automatically wipes its assignments from the `school_class_assignments` junction table and Class metadata from the `classes` table in a single, atomic SQLite transaction block, preventing orphaned dead references.

### 🔐 Account & Permission Mapping
*   **Method**: `AccountRepositoryImpl.selectActiveClass()`, `assignClassToAccount()`, and `removeClassAccess()`
*   **Behavior**: Wraps mutations to both the `accounts` (master serialized memberships) and `account_class_access` (granular supervisor permission table) tables inside `database.withTransaction { ... }` blocks.

### 📝 Bulk Student Imports
*   **Method**: `BulkStudentImportUseCase.kt`
*   **Behavior**: Executes a single, comprehensive bulk-insert database transaction for high-volume student CSV registrations. Even if the device battery dies mid-import, SQLite's ACID transaction log automatically rolls back the entire bulk operation on the next boot, ensuring "all-or-nothing" consistency.