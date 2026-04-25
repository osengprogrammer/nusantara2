package com.azuratech.azuratime.di

import com.azuratech.azuratime.data.core.AndroidImageProcessor
import com.azuratech.azuratime.data.core.AndroidStorageProvider
import com.azuratech.azuratime.data.local.CheckInLocalDataSource
import com.azuratech.azuratime.data.local.CheckInLocalDataSourceImpl
import com.azuratech.azuratime.data.local.FaceLocalDataSource
import com.azuratech.azuratime.data.local.FaceLocalDataSourceImpl
import com.azuratech.azuratime.data.remote.CheckInRemoteDataSource
import com.azuratech.azuratime.data.remote.CheckInRemoteDataSourceImpl
import com.azuratech.azuratime.data.remote.FaceRemoteDataSource
import com.azuratech.azuratime.data.remote.FaceRemoteDataSourceImpl
import com.azuratech.azuraengine.core.ImageProcessor
import com.azuratech.azuraengine.core.StorageProvider
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
    abstract fun bindFaceLocalDataSource(
        impl: FaceLocalDataSourceImpl
    ): FaceLocalDataSource

    @Binds
    @Singleton
    abstract fun bindFaceRemoteDataSource(
        impl: FaceRemoteDataSourceImpl
    ): FaceRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindCheckInLocalDataSource(
        impl: CheckInLocalDataSourceImpl
    ): CheckInLocalDataSource

    @Binds
    @Singleton
    abstract fun bindCheckInRemoteDataSource(
        impl: CheckInRemoteDataSourceImpl
    ): CheckInRemoteDataSource
}
