package com.azuratech.azuratime.feature.auth.impl.di

import com.azuratech.azuratime.feature.auth.api.AuthRepository

/**
 * Dagger module that provides the concrete AuthRepository implementation.
 * The implementation class is instantiated directly; no generated code needed.
 */
@javax.inject.Singleton
@dagger.Module
object AuthImplModule {

    @dagger.Provides fun provideAuthRepository(): AuthRepository = com.azuratech.azuratime.feature.auth.impl.AuthRepositoryImpl()
}
