package com.azuratech.azuratime.di

import com.azuratech.azuratime.data.core.AndroidImageProcessor
import com.azuratech.azuratime.data.core.AndroidStorageProvider
import com.azuratech.azuratime.features.attendance.data.local.CheckInLocalDataSource
import com.azuratech.azuratime.features.attendance.data.local.CheckInLocalDataSourceImpl
import com.azuratech.azuratime.data.local.FaceLocalDataSource
import com.azuratech.azuratime.data.local.FaceLocalDataSourceImpl
import com.azuratech.azuratime.features.attendance.data.remote.CheckInRemoteDataSource
import com.azuratech.azuratime.features.attendance.data.remote.CheckInRemoteDataSourceImpl
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuratime.data.remote.FaceRemoteDataSourceImpl
import com.azuratech.azuraengine.core.ImageProcessor
import com.azuratech.azuraengine.core.StorageProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.azuratech.azuratime.data.remote.SchoolRemoteDataSource
import com.azuratech.azuratime.data.remote.SchoolRemoteDataSourceImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindSchoolRemoteDataSource(
        impl: SchoolRemoteDataSourceImpl
    ): com.azuratech.azuratime.data.remote.SchoolRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindImageProcessor(
        impl: AndroidImageProcessor
    ): com.azuratech.azuraengine.core.ImageProcessor

    @Binds
    @Singleton
    abstract fun bindStorageProvider(
        impl: AndroidStorageProvider
    ): com.azuratech.azuraengine.core.StorageProvider

    @Binds
    @Singleton
    abstract fun bindFaceLocalDataSource(
        impl: FaceLocalDataSourceImpl
    ): com.azuratech.azuratime.data.local.FaceLocalDataSource

    @Binds
    @Singleton
    abstract fun bindFaceRemoteDataSource(
        impl: FaceRemoteDataSourceImpl
    ): com.azuratech.azuratime.data.remote.FaceRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindCheckInLocalDataSource(
        impl: CheckInLocalDataSourceImpl
    ): com.azuratech.azuratime.features.attendance.data.local.CheckInLocalDataSource

    @Binds
    @Singleton
    abstract fun bindCheckInRemoteDataSource(
        impl: CheckInRemoteDataSourceImpl
    ): com.azuratech.azuratime.features.attendance.data.remote.CheckInRemoteDataSource
}
