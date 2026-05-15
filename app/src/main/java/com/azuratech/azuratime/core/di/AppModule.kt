package com.azuratech.azuratime.core.di

import android.content.Context
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.student.data.local.StudentDao
import com.azuratech.azuratime.features.attendance.data.local.AttendanceRecordDao
import com.azuratech.azuratime.features.attendance.data.local.AttendanceConflictDao
import com.azuratech.azuratime.features.biometric.data.local.BiometricFaceDao
import com.azuratech.azuratime.features.biometric.data.local.FaceAssignmentDao
import com.azuratech.azuratime.features.staff.data.local.StaffAccountDao
import com.azuratech.azuratime.features.staff.data.local.AccessRequestDao
import com.azuratech.azuratime.features.reporting.data.local.AuditLogDao
import com.azuratech.azuratime.features.reporting.data.local.ExportJobDao
import com.azuratech.azuratime.features.reporting.data.local.ReportDao
import com.azuratech.azuratime.features.school.data.local.ClassDao
import com.azuratech.azuratime.features.school.data.local.SchoolDao
import com.azuratech.azuratime.core.data.local.UserClassAccessDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // =====================================================
    // 🗄️ LOCAL DATABASE PROVIDER (Room)
    // =====================================================
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    // =====================================================
    // 🔑 SESSION MANAGER PROVIDER (DataStore/SharedPreferences)
    // =====================================================
    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager.getInstance(context)
    }

    // =====================================================
    // 🗡️ DAO PROVIDERS
    // =====================================================
    @Provides
    fun provideStudentDao(db: AppDatabase): StudentDao = db.studentDao()

    @Provides
    fun provideAttendanceRecordDao(db: AppDatabase): AttendanceRecordDao = db.attendanceRecordDao()

    @Provides
    fun provideBiometricFaceDao(db: AppDatabase): BiometricFaceDao = db.faceDao()

    @Provides
    fun provideBiometricAssignmentDao(db: AppDatabase): FaceAssignmentDao = db.faceAssignmentDao()

    @Provides
    fun provideUserDao(db: AppDatabase): StaffAccountDao = db.userDao()

    @Provides
    fun provideClassDao(db: AppDatabase): ClassDao = db.classDao()

    @Provides
    fun provideSchoolDao(db: AppDatabase): SchoolDao = db.schoolDao()

    @Provides
    fun provideUserClassAccessDao(db: AppDatabase): UserClassAccessDao = db.userClassAccessDao()

    @Provides
    fun provideAccessRequestDao(db: AppDatabase): AccessRequestDao = db.accessRequestDao()

    @Provides
    fun provideAttendanceConflictDao(db: AppDatabase): AttendanceConflictDao = db.attendanceConflictDao()

    @Provides
    fun provideAuditLogDao(db: AppDatabase): AuditLogDao = db.auditLogDao()

    @Provides
    fun provideExportJobDao(db: AppDatabase): ExportJobDao = db.exportJobDao()

    @Provides
    fun provideReportDao(db: AppDatabase): ReportDao = db.reportDao()

    // =====================================================
    // ☁️ FIREBASE CLOUD PROVIDERS
    // =====================================================

    @Provides
    @Singleton
    fun provideFirebaseStorage(): com.google.firebase.storage.FirebaseStorage {
        return com.google.firebase.storage.FirebaseStorage.getInstance()
    }
}
