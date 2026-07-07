package com.azuratech.azuratime.features.payment.di

import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.core.data.local.StudentWalletDao
import com.azuratech.azuratime.features.payment.data.local.PaymentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PaymentDaoModule {
    @Provides
    @Singleton
    fun providePaymentDao(db: AppDatabase): PaymentDao = db.paymentDao()

    @Provides
    @Singleton
    fun provideStudentWalletDao(db: AppDatabase): StudentWalletDao = db.studentWalletDao()
}
