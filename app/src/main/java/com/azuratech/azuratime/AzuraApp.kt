package com.azuratech.azuratime

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AzuraApp : Application(), Configuration.Provider { // 🔥 Tambahkan Provider

    @Inject lateinit var workerFactory: HiltWorkerFactory // 🔥 Suntikkan Factory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory) // 🔥 Sambungkan ke Hilt
            .build()
}