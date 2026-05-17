package com.azuratech.azuratime.core.di

import com.azuratech.azuratime.features.student.domain.repository.StudentRepository
import com.azuratech.azuratime.features.student.data.repo.StudentRepositoryImpl
import com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository
import com.azuratech.azuratime.features.attendance.data.repo.AttendanceRepositoryImpl
import com.azuratech.azuratime.features.account.domain.repository.AccessRequestRepository
import com.azuratech.azuratime.features.account.data.repo.AccessRequestRepositoryImpl
import com.azuratech.azuratime.core.domain.media.FileStorage
import com.azuratech.azuratime.core.domain.media.PhotoStorageUtils
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun provideStudentRepository(
        impl: StudentRepositoryImpl,
    ): StudentRepository

    @Binds
    @Singleton
    abstract fun provideAttendanceRepository(
        impl: AttendanceRepositoryImpl,
    ): AttendanceRepository

    @Binds
    @Singleton
    abstract fun provideAccessRequestRepository(
        impl: AccessRequestRepositoryImpl,
    ): AccessRequestRepository

    @Binds
    @Singleton
    abstract fun provideBiometricRepository(
        impl: com.azuratech.azuratime.features.biometric.domain.repository.StudentBiometricRepository,
    ): com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository

    @Binds
    @Singleton
    abstract fun provideFileStorage(
        impl: PhotoStorageUtils,
    ): FileStorage
}
