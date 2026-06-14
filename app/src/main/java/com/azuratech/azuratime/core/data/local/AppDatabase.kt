package com.azuratech.azuratime.core.data.local

import android.content.Context
import androidx.room.*
import com.azuratech.azuratime.features.school.data.local.*
import com.azuratech.azuratime.features.student.data.local.*
import com.azuratech.azuratime.features.attendance.data.local.*
import com.azuratech.azuratime.features.biometric.data.local.*
import com.azuratech.azuratime.features.account.data.local.*
import com.azuratech.azuratime.features.reporting.data.local.*
import com.azuratech.azuratime.features.aimusic.data.local.*
import com.azuratech.azuratime.features.session.data.local.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        SchoolEntity::class,
        ClassEntity::class,
        SchoolClassAssignment::class,
        StudentBiometricEntity::class,
        StudentClassAssignmentEntity::class,
        AttendanceRecordEntity::class,
        AccountEntity::class,
        AccountClassAccessEntity::class,
        StudentEntity::class,
        AccessRequestEntity::class,
        AttendanceConflictEntity::class,
        AuditLogEntity::class,
        ExportJobEntity::class,
        ReportEntity::class,
        AiMusicEntity::class,
        GpsGeofenceEntity::class,
        SubjectEntity::class,
        ClassSessionEntity::class,
    ],
    version = 24,
    exportSchema = false,
)
@TypeConverters(Converters::class)
@Suppress("RoomProcessor:RoomSchemaMerging")
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun biometricDao(): StudentBiometricDao
    abstract fun studentClassAssignmentDao(): StudentClassAssignmentDao
    abstract fun accountDao(): AccountDao
    abstract fun schoolDao(): SchoolDao
    abstract fun classDao(): ClassDao
    abstract fun schoolClassDao(): SchoolClassDao
    abstract fun accountClassAccessDao(): AccountClassAccessDao
    abstract fun accessRequestDao(): AccessRequestDao
    abstract fun attendanceConflictDao(): AttendanceConflictDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun exportJobDao(): ExportJobDao
    abstract fun reportDao(): ReportDao
    abstract fun aiMusicDao(): AiMusicDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `check_in_records` ADD COLUMN `sessionType` TEXT NOT NULL DEFAULT 'ACADEMIC'")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create new table with nullable classId/subjectId and sessionType
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `class_sessions_new` (
                        `sessionId` TEXT NOT NULL, 
                        `classId` TEXT, 
                        `subjectId` TEXT, 
                        `sessionType` TEXT NOT NULL DEFAULT 'ACADEMIC', 
                        `supervisorEmail` TEXT NOT NULL, 
                        `dayOfWeek` INTEGER NOT NULL, 
                        `startTime` TEXT NOT NULL, 
                        `endTime` TEXT NOT NULL, 
                        `schoolId` TEXT NOT NULL, 
                        `lookupKey` TEXT NOT NULL, 
                        `isActive` INTEGER NOT NULL DEFAULT 1, 
                        `isSynced` INTEGER NOT NULL DEFAULT 0, 
                        PRIMARY KEY(`sessionId`)
                    )
                    """.trimIndent(),
                )

                // 2. Copy data and Prefix lookupKey for legacy ACADEMIC sessions
                db.execSQL(
                    """
                    INSERT INTO `class_sessions_new` (
                        sessionId, classId, subjectId, sessionType, supervisorEmail, 
                        dayOfWeek, startTime, endTime, schoolId, lookupKey, isActive, isSynced
                    )
                    SELECT 
                        sessionId, classId, subjectId, 'ACADEMIC', supervisorEmail, 
                        dayOfWeek, startTime, endTime, schoolId, 'ACADEMIC_' || lookupKey, isActive, isSynced
                    FROM `class_sessions`
                    """.trimIndent(),
                )

                // 3. Swap tables
                db.execSQL("DROP TABLE `class_sessions`")
                db.execSQL("ALTER TABLE `class_sessions_new` RENAME TO `class_sessions`")

                // 4. Recreate Indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_class_sessions_schoolId` ON `class_sessions` (`schoolId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_class_sessions_classId` ON `class_sessions` (`classId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_class_sessions_subjectId` ON `class_sessions` (`subjectId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_class_sessions_lookupKey` ON `class_sessions` (`lookupKey`)")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No-op rollback: Keeping version bump to avoid crashes on existing installs
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `check_in_records` ADD COLUMN `processingType` TEXT NOT NULL DEFAULT 'SINGLE'")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `check_in_records` ADD COLUMN `authMethod` TEXT NOT NULL DEFAULT 'FACE'")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // LEGACY: Support for polymorphic sessions and directional tracking
                db.execSQL("ALTER TABLE `class_sessions` ADD COLUMN `sessionType` TEXT NOT NULL DEFAULT 'SCHOOL'")
                db.execSQL("ALTER TABLE `check_in_records` ADD COLUMN `direction` TEXT")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `class_sessions` ADD COLUMN `lookupKey` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `class_sessions` ADD COLUMN `isActive` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_class_sessions_lookupKey` ON `class_sessions` (`lookupKey`)")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subjects` (
                        `subjectId` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `description` TEXT, 
                        `schoolId` TEXT NOT NULL, 
                        `isSynced` INTEGER NOT NULL, 
                        PRIMARY KEY(`subjectId`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `class_sessions` (
                        `sessionId` TEXT NOT NULL, 
                        `classId` TEXT NOT NULL, 
                        `subjectId` TEXT NOT NULL, 
                        `supervisorEmail` TEXT NOT NULL, 
                        `dayOfWeek` INTEGER NOT NULL, 
                        `startTime` TEXT NOT NULL, 
                        `endTime` TEXT NOT NULL, 
                        `schoolId` TEXT NOT NULL, 
                        `isSynced` INTEGER NOT NULL, 
                        PRIMARY KEY(`sessionId`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_class_sessions_schoolId` ON `class_sessions` (`schoolId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_class_sessions_classId` ON `class_sessions` (`classId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_class_sessions_subjectId` ON `class_sessions` (`subjectId`)")
                db.execSQL("ALTER TABLE `check_in_records` ADD COLUMN `sessionId` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val integrityCheckCallback = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                try {
                    val result = db.query("PRAGMA integrity_check").use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else "fail"
                    }
                    if (result != "ok") {
                        android.util.Log.e("AZURA_DB", "Database corruption detected! Triggering safe-mode wipe.")
                    } else {
                        android.util.Log.d("AZURA_DB", "Database health check: OK")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AZURA_DB", "Integrity check failed: ${e.message}", e)
                }
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "local_db2.sqlite",
                )
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .addMigrations(MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24)
                    .fallbackToDestructiveMigration()
                    .addCallback(integrityCheckCallback)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
