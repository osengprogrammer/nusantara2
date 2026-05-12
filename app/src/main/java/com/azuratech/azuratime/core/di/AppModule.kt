package com.azuratech.azuratime.core.di

import android.content.Context
import com.azuratech.azuratime.core.session.SessionManager
import com.azuratech.azuratime.data.local.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.azuratech.azuratime.features.student.data.local.StudentDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): com.azuratech.azuratime.data.local.AppDatabase {
        return com.azuratech.azuratime.data.local.AppDatabase.getInstance(context)
    }

    // =====================================================
    // 🔑 SESSION MANAGER PROVIDER (DataStore/SharedPreferences)
    // =====================================================
    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): com.azuratech.azuratime.core.session.SessionManager {
        return com.azuratech.azuratime.core.session.SessionManager.getInstance(context)
    }

    // =====================================================
    // 🗡️ DAO PROVIDERS
    // =====================================================
    @Provides
    fun provideStudentDao(db: AppDatabase): StudentDao = db.studentDao()

    @Provides
    fun provideBiometricFaceDao(db: AppDatabase): com.azuratech.azuratime.data.local.BiometricFaceDao = db.faceDao()

    @Provides
    fun provideBiometricAssignmentDao(db: AppDatabase): com.azuratech.azuratime.data.local.FaceAssignmentDao = db.faceAssignmentDao()

    @Provides
    fun provideUserDao(db: AppDatabase): com.azuratech.azuratime.data.local.StaffAccountDao = db.userDao()

    @Provides
    fun provideClassDao(db: AppDatabase): com.azuratech.azuratime.data.local.ClassDao = db.classDao()

    @Provides
    fun provideSchoolDao(db: AppDatabase): com.azuratech.azuratime.data.local.SchoolDao = db.schoolDao()

    @Provides
    fun provideUserClassAccessDao(db: AppDatabase): com.azuratech.azuratime.data.local.UserClassAccessDao = db.userClassAccessDao()

    @Provides
    fun provideAccessRequestDao(db: AppDatabase): com.azuratech.azuratime.data.local.AccessRequestDao = db.accessRequestDao()

    @Provides
    fun provideAttendanceConflictDao(db: AppDatabase): com.azuratech.azuratime.data.local.AttendanceConflictDao = db.attendanceConflictDao()

    // =====================================================
    // ☁️ FIREBASE CLOUD PROVIDERS
    // =====================================================

    // ❌ FUNGSI provideFirebaseFirestore() TELAH DIHAPUS DARI SINI ❌
    // Sudah ditangani secara eksklusif oleh FirebaseModule.

    @Provides
    @Singleton
    fun provideFirebaseStorage(): com.google.firebase.storage.FirebaseStorage {
        return com.google.firebase.storage.FirebaseStorage.getInstance()
    }
}