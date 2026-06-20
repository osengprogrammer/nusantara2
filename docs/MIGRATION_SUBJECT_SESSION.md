# 📑 Migration & Implementation Guide: Subject-Based Attendance (v3.3.2)
Multi-Subject Attendance Architecture for AzuraFace

## 🎯 Vision & Scope
| Item | Description |
| :--- | :--- |
| **Goal** | Decouple attendance from physical class → attach to Class + Subject + Supervisor + Schedule via ClassSession |
| **Non-Goal** | No changes to AzuraEngine (face pipeline remains stateless & pure) |
| **Principles** | Local Room DB is Sovereign, Vertical Slice Architecture (VSA), Effect-Driven MVI, Supervisor-centric terminology |

---

## 🧱 Architecture & Data Model (Room + VSA Compliant)

### 🔹 Entity Relationships
- `AttendanceRecord` (`check_in_records`) → `sessionId` (primary reference) + `classId` (legacy fallback)
- `ClassSession` → `subjectId` + `classId` + `supervisorEmail`

### 🔹 Room Schema Draft (Flat VSA Structure)
Path: `features/session/data/local/`

```kotlin
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val subjectId: String,
    val name: String,
    val description: String? = null,
    val schoolId: String,
    val isSynced: Boolean = false
)

@Entity(
    tableName = "class_sessions",
    indices = [Index(value = ["schoolId"]), Index(value = ["classId"])]
)
data class ClassSessionEntity(
    @PrimaryKey val sessionId: String,
    val classId: String,
    val subjectId: String,
    val supervisorEmail: String, // 🔥 Unified Identity
    val dayOfWeek: Int,          // 1 (Mon) - 7 (Sun)
    val startTime: String,       // "08:00"
    val endTime: String,         // "09:30"
    val schoolId: String,
    val isSynced: Boolean = false
)
```

### 🔹 DAO Draft
Path: `features/session/data/local/SessionDao.kt`
```kotlin
@Dao
interface SessionDao {
    @Query("SELECT * FROM class_sessions WHERE schoolId = :schoolId AND dayOfWeek = :day")
    fun getSessionsByDayFlow(schoolId: String, day: Int): Flow<List<ClassSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ClassSessionEntity)
}
```

### 🔹 DB Registration
Path: `core/data/local/AppDatabase.kt`
- Add `SubjectEntity::class`, `ClassSessionEntity::class` to `@Database(entities = [...])`
- Export schema → update version 17
- Add `Migration(16, 17)` for new tables and column in `check_in_records`.

---

## 🗄️ Database Changes (Room v16 -> v17)
- **New Table: `subjects`**: Stores available school subjects.
- **New Table: `class_sessions`**: Links Class, Subject, and Supervisor (via email) with a specific schedule.
- **Modified Table: `check_in_records`**: Added `sessionId` (TEXT, NOT NULL, DEFAULT '') to link attendance logs to specific sessions.

### 🛠️ Migration SQL
```sql
CREATE TABLE IF NOT EXISTS `subjects` (`subjectId` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, `schoolId` TEXT NOT NULL, `isSynced` INTEGER NOT NULL, PRIMARY KEY(`subjectId`));

CREATE TABLE IF NOT EXISTS `class_sessions` (`sessionId` TEXT NOT NULL, `classId` TEXT NOT NULL, `subjectId` TEXT NOT NULL, `supervisorEmail` TEXT NOT NULL, `dayOfWeek` INTEGER NOT NULL, `startTime` TEXT NOT NULL, `endTime` TEXT NOT NULL, `schoolId` TEXT NOT NULL, `isSynced` INTEGER NOT NULL, PRIMARY KEY(`sessionId`));

CREATE INDEX IF NOT EXISTS `index_class_sessions_schoolId` ON `class_sessions` (`schoolId`);
CREATE INDEX IF NOT EXISTS `index_class_sessions_classId` ON `class_sessions` (`classId`);
CREATE INDEX IF NOT EXISTS `index_class_sessions_subjectId` ON `class_sessions` (`subjectId`);

ALTER TABLE `check_in_records` ADD COLUMN `sessionId` TEXT NOT NULL DEFAULT '';
```

---

## 👤 Identity & RBAC
- **Supervisor-Centric**: Access to sessions is validated against the active account's email (`supervisorEmail`).
- **Unified Identity**: The `accountEmail` from `AttendanceRecord` is preserved for historical audit.

---

## 🔙 Rollback & Migration Notes
1. Revert code to `v3.2.2-ai-native`.
2. The `sessionId` column in `check_in_records` will be ignored by the older code.
3. If a clean state is required, clear app data to recreate v16 schema.
4. **Existing Data**: `sessionId` in `check_in_records` defaults to `""` → existing records remain queryable via `classId`.

---

## 🚀 Rollout Strategy
1. **Internal**: Enable `ENABLE_SUBJECT_SESSION = true` in `build.gradle.kts`.
2. **Phase 1**: Manual creation of Subjects/Sessions for pilot supervisors.
3. **Phase 2**: Full sync engine integration for Subjects/Sessions.
