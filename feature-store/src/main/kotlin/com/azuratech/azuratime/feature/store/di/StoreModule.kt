package com.azuratech.azuratime.feature.store.di

import com.azuratech.azuratime.core.api.store.StoreRepository
import com.azuratech.azuratime.feature.store.data.StoreRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StoreModule {

    @Binds
    @Singleton
    abstract fun bindStoreRepository(
        storeRepositoryImpl: StoreRepositoryImpl
    ): StoreRepository
}
