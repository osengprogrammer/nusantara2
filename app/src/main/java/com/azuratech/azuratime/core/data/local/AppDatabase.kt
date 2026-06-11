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
    version = 18,
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

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add lookupKey and isActive to class_sessions
                database.execSQL("ALTER TABLE `class_sessions` ADD COLUMN `lookupKey` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `class_sessions` ADD COLUMN `isActive` INTEGER NOT NULL DEFAULT 1")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_class_sessions_lookupKey` ON `class_sessions` (`lookupKey`)")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Create subjects table
                database.execSQL(
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

                // 2. Create class_sessions table
                database.execSQL(
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

                // Add indices for class_sessions
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_class_sessions_schoolId` ON `class_sessions` (`schoolId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_class_sessions_classId` ON `class_sessions` (`classId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_class_sessions_subjectId` ON `class_sessions` (`subjectId`)")

                // 3. Update check_in_records (AttendanceRecordEntity)
                database.execSQL("ALTER TABLE `check_in_records` ADD COLUMN `sessionId` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val integrityCheckCallback = object : RoomDatabase.Callback() {
            override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
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
                    .addMigrations(MIGRATION_16_17, MIGRATION_17_18)
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
