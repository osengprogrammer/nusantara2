package com.azuratech.azuratime.core.di

import com.azuratech.azuratime.core.data.AndroidImageProcessor
import com.azuratech.azuratime.core.data.AndroidStorageProvider
import com.azuratech.azuratime.features.attendance.data.local.AttendanceLocalDataSource
import com.azuratech.azuratime.features.attendance.data.local.AttendanceLocalDataSourceImpl
import com.azuratech.azuratime.features.biometric.data.local.BiometricLocalDataSource
import com.azuratech.azuratime.features.biometric.data.local.BiometricLocalDataSourceImpl
import com.azuratech.azuratime.features.attendance.data.remote.AttendanceRemoteDataSource
import com.azuratech.azuratime.features.attendance.data.remote.AttendanceRemoteDataSourceImpl
import com.azuratech.azuratime.features.biometric.data.remote.BiometricRemoteDataSource
import com.azuratech.azuratime.features.biometric.data.remote.BiometricRemoteDataSourceImpl
import com.azuratech.azuraengine.core.ImageProcessor
import com.azuratech.azuraengine.core.StorageProvider
import com.azuratech.azuratime.features.school.data.remote.SchoolRemoteDataSource
import com.azuratech.azuratime.features.school.data.remote.SchoolRemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindSchoolRemoteDataSource(
        impl: SchoolRemoteDataSourceImpl
    ): SchoolRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindImageProcessor(
        impl: AndroidImageProcessor
    ): ImageProcessor

    @Binds
    @Singleton
    abstract fun bindStorageProvider(
        impl: AndroidStorageProvider
    ): StorageProvider

    @Binds
    @Singleton
    abstract fun bindBiometricLocalDataSource(
        impl: BiometricLocalDataSourceImpl
    ): BiometricLocalDataSource

    @Binds
    @Singleton
    abstract fun bindBiometricRemoteDataSource(
        impl: BiometricRemoteDataSourceImpl
    ): BiometricRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAttendanceLocalDataSource(
        impl: AttendanceLocalDataSourceImpl
    ): AttendanceLocalDataSource

    @Binds
    @Singleton
    abstract fun bindAttendanceRemoteDataSource(
        impl: AttendanceRemoteDataSourceImpl
    ): AttendanceRemoteDataSource
}
