# 📑 Migration Guide: Subject-Based Attendance (v3.3.2)

## 🎯 Overview
Migrating attendance from a physical class-only model to a multi-subject session model.

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

## 👤 Identity & RBAC
- **Supervisor-Centric**: Access to sessions is validated against the active account's email (`supervisorEmail`).
- **Unified Identity**: The `accountEmail` from `AttendanceRecord` is preserved for historical audit.

## 🔙 Rollback Procedure
1. Revert code to `v3.2.2-ai-native`.
2. The `sessionId` column in `check_in_records` will be ignored by the older code.
3. If a clean state is required, clear app data to recreate v16 schema.

## 🚀 Rollout Strategy
1. **Internal**: Enable `ENABLE_SUBJECT_SESSION = true` in `build.gradle.kts`.
2. **Phase 1**: Manual creation of Subjects/Sessions for pilot supervisors.
3. **Phase 2**: Full sync engine integration for Subjects/Sessions.
