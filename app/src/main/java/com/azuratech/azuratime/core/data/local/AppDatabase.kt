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
    ],
    version = 16,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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
