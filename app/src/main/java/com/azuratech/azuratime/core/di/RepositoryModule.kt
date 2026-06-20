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
    abstract fun provideBiometricScannerRepository(
        impl: com.azuratech.azuratime.features.attendance.data.repo.BiometricScannerRepositoryImpl,
    ): com.azuratech.azuratime.features.attendance.domain.repository.BiometricScannerRepository

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
    abstract fun provideBootRepository(
        impl: com.azuratech.azuratime.core.data.repo.BootRepositoryImpl,
    ): com.azuratech.azuratime.core.domain.repository.BootRepository

    @Binds
    @Singleton
    abstract fun provideMainRepository(
        impl: com.azuratech.azuratime.core.data.repo.MainRepositoryImpl,
    ): com.azuratech.azuratime.core.domain.repository.MainRepository

    @Binds
    @Singleton
    abstract fun provideSecurityRepository(
        impl: com.azuratech.azuratime.core.data.repo.SecurityRepositoryImpl,
    ): com.azuratech.azuratime.core.domain.repository.SecurityRepository

    @Binds
    @Singleton
    abstract fun provideSyncRepository(
        impl: com.azuratech.azuratime.core.data.repo.SyncRepositoryImpl,
    ): com.azuratech.azuratime.core.domain.repository.SyncRepository

    @Binds
    @Singleton
    abstract fun provideAuditLogRepository(
        impl: com.azuratech.azuratime.features.reporting.data.repo.AuditLogRepositoryImpl,
    ): com.azuratech.azuratime.features.reporting.domain.repository.AuditLogRepository

    @Binds
    @Singleton
    abstract fun provideExportRepository(
        impl: com.azuratech.azuratime.features.reporting.data.repo.ExportRepositoryImpl,
    ): com.azuratech.azuratime.features.reporting.domain.repository.ExportRepository

    @Binds
    @Singleton
    abstract fun provideDataIntegrityRepository(
        impl: com.azuratech.azuratime.features.reporting.data.repo.DataIntegrityRepositoryImpl,
    ): com.azuratech.azuratime.features.reporting.domain.repository.DataIntegrityRepository

    @Binds
    @Singleton
    abstract fun provideReportRepository(
        impl: com.azuratech.azuratime.features.reporting.data.repo.ReportRepositoryImpl,
    ): com.azuratech.azuratime.features.reporting.domain.repository.ReportRepository

    @Binds
    @Singleton
    abstract fun provideZoharRepository(
        impl: com.azuratech.azuratime.features.ai.data.repo.ZoharRepositoryImpl,
    ): com.azuratech.azuratime.features.ai.domain.repository.ZoharRepository

    @Binds
    @Singleton
    abstract fun provideAppUpdateRepository(
        impl: com.azuratech.azuratime.features.update.data.repo.AppUpdateRepositoryImpl,
    ): com.azuratech.azuratime.features.update.domain.repository.AppUpdateRepository

    @Binds
    @Singleton
    abstract fun provideAiMusicRepository(
        impl: com.azuratech.azuratime.features.aimusic.data.repo.AiMusicRepositoryImpl,
    ): com.azuratech.azuratime.features.aimusic.domain.repository.AiMusicRepository

    @Binds
    @Singleton
    abstract fun provideTemplateRepository(
        impl: com.azuratech.azuratime.features.template.data.repo.TemplateRepositoryImpl,
    ): com.azuratech.azuratime.features.template.domain.repository.TemplateRepository

    @Binds
    @Singleton
    abstract fun provideSessionRepository(
        impl: com.azuratech.azuratime.features.session.SessionRepositoryImpl,
    ): com.azuratech.azuratime.features.session.SessionRepository

    @Binds
    @Singleton
    abstract fun provideFileStorage(
        impl: com.azuratech.azuratime.core.domain.media.PhotoStorageUtils,
    ): com.azuratech.azuratime.core.domain.media.FileStorage
}
