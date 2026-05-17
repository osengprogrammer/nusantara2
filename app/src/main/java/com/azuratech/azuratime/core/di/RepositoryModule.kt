package com.azuratech.azuratime.core.di

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
        impl: com.azuratech.azuratime.features.student.data.repo.StudentRepositoryImpl,
    ): com.azuratech.azuratime.features.student.domain.repository.StudentRepository

    @Binds
    @Singleton
    abstract fun provideAttendanceRepository(
        impl: com.azuratech.azuratime.features.attendance.data.repo.AttendanceRepositoryImpl,
    ): com.azuratech.azuratime.features.attendance.domain.repository.AttendanceRepository

    @Binds
    @Singleton
    abstract fun provideAccessRequestRepository(
        impl: com.azuratech.azuratime.features.account.data.repo.AccessRequestRepositoryImpl,
    ): com.azuratech.azuratime.features.account.domain.repository.AccessRequestRepository

    @Binds
    @Singleton
    abstract fun provideAccountRepository(
        impl: com.azuratech.azuratime.features.account.data.repo.AccountRepositoryImpl,
    ): com.azuratech.azuratime.features.account.domain.repository.AccountRepository

    @Binds
    @Singleton
    abstract fun provideMembershipRepository(
        impl: com.azuratech.azuratime.features.account.data.repo.MembershipRepositoryImpl,
    ): com.azuratech.azuratime.features.account.domain.repository.MembershipRepository

    @Binds
    @Singleton
    abstract fun provideSchoolWorkspaceRepository(
        impl: com.azuratech.azuratime.features.account.data.repo.SchoolWorkspaceRepositoryImpl,
    ): com.azuratech.azuratime.features.account.domain.repository.SchoolWorkspaceRepository

    @Binds
    @Singleton
    abstract fun provideStudentRegistrationRepository(
        impl: com.azuratech.azuratime.features.student.data.repo.StudentRegistrationRepositoryImpl,
    ): com.azuratech.azuratime.features.student.domain.repository.StudentRegistrationRepository

    @Binds
    @Singleton
    abstract fun provideAuthRepository(
        impl: com.azuratech.azuratime.features.auth.data.repo.AuthRepositoryImpl,
    ): com.azuratech.azuratime.features.auth.domain.repository.AuthRepository

    @Binds
    @Singleton
    abstract fun provideSchoolRepository(
        impl: com.azuratech.azuratime.features.school.data.repo.SchoolRepositoryImpl,
    ): com.azuratech.azuratime.features.school.domain.repository.SchoolRepository

    @Binds
    @Singleton
    abstract fun provideBiometricRepository(
        impl: com.azuratech.azuratime.features.biometric.data.repo.StudentBiometricRepositoryImpl,
    ): com.azuratech.azuratime.features.biometric.domain.repository.BiometricRepository

    @Binds
    @Singleton
    abstract fun provideFileStorage(
        impl: com.azuratech.azuratime.core.domain.media.PhotoStorageUtils,
    ): com.azuratech.azuratime.core.domain.media.FileStorage
}
