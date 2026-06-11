📅 SUBJECT SESSION IMPLEMENTATION PLAN (v3.3.2-final)
Multi-Subject Attendance Architecture for AzuraFace
📍 Save as SUBJECT_SESSION_PLAN.md in project root

🎯 Vision & Scope
| Item | Description |
| :--- | :--- |
| **Goal** | Decouple attendance from physical class → attach to Class + Subject + Supervisor + Schedule via ClassSession |
| **Non-Goal** | No changes to AzuraEngine (face pipeline remains stateless & pure) |
| **Principles** | Local Room DB is Sovereign, Vertical Slice Architecture (VSA), Effect-Driven MVI, Supervisor-centric terminology |
| **Target Timeline** | 4–8 hours (AI-Native + CLI-assisted) |

🧱 Architecture & Data Model (Room + VSA Compliant)
🔹 Entity Relationships
- AttendanceRecord (check_in_records) → sessionId (primary reference) + classId (legacy fallback)
- ClassSession → subjectId + classId + supervisorEmail

🔹 Room Schema Draft (Flat VSA Structure)
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

🔹 DAO Draft
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

🔹 DB Registration
Path: `core/data/local/AppDatabase.kt`
- Add `SubjectEntity::class`, `ClassSessionEntity::class` to `@Database(entities = [...])`
- Export schema → update version 17
- Add `Migration(16, 17)` for new tables and column in `check_in_records`.

📋 Phase Breakdown (Vertical Slices)
| Phase | Scope | Deliverable | Path |
| :--- | :--- | :--- | :--- |
| **1** | **Schema & DAOs** | Entities, DAO, AppDatabase v17, Migration | `features/session/data/local/`, `core/data/local/` |
| **2** | **Repository & UseCases** | SessionRepository (`asLocalResult`), GetSessionsByDayUseCase | `features/session/` |
| **3** | **UI & ViewModel** | SessionPickerScreen, SessionPickerViewModel (Effect-Driven MVI) | `features/session/ui/` |
| **4** | **Testing & Polish** | Unit/Integration tests, docs, rollback plan | `src/test/`, `docs/` |

🤖 Phase 1 Prompt (Ready for Gemini CLI)
```text
"Act as an Android Expert. Create the 'session' feature slice.
1. Create SubjectEntity and ClassSessionEntity in features/session/data/local/ using the SUBJECT_SESSION_PLAN.md schema.
2. Create SessionDao.kt in the same folder with getSessionsByDayFlow.
3. Update AppDatabase.kt: add entities and implement a Migration to add these tables and the 'sessionId' column to 'check_in_records'.
4. Ensure 100% adherence to ARCHITECTURE.md (English-first, result-oriented, local-first)."
```

🛡️ Migration & Rollback Notes
- **Existing Data**: `sessionId` in `check_in_records` defaults to `""` → existing records remain queryable via `classId`.
- **SQL Migration**: `ALTER TABLE check_in_records ADD COLUMN sessionId TEXT NOT NULL DEFAULT ""`
- **Rollback**: git revert, delete local DB for clean state.
- **Feature Flag**: `BuildConfig.ENABLE_SUBJECT_SESSION` to toggle UI/flow safely.

✅ Definition of Done (Phase 1)
- [ ] `./gradlew :app:kspDebugKotlin` passes without errors.
- [ ] `AppDatabase.kt` version bumped with working `Migration`.
- [ ] Flat structure matches `features/attendance/` & `features/student/`.
- [ ] `check_in_records` table name used correctly in all SQL queries.
- [ ] `supervisorEmail` used instead of `supervisorId`.
```
