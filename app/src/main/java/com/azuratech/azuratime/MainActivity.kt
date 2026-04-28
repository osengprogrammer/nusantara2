package com.azuratech.azuratime

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.azuratech.azuratime.ui.theme.AzuraTheme
import com.azuratech.azuratime.core.sync.SyncWorker
import com.azuratech.azuratime.domain.classes.usecase.BackfillOrphanedClassesUseCase
import dagger.hilt.android.AndroidEntryPoint // 🔥 Import Hilt ditambahkan
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint // 🔥 Anotasi krusial agar UI Compose di bawahnya bisa menggunakan hiltViewModel()
class MainActivity : ComponentActivity() {

    @Inject lateinit var backfillUseCase: BackfillOrphanedClassesUseCase

    // Menggunakan variabel biasa agar lebih responsif di level sistem
    private var isBootReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔥 1. Pasang Splash Screen
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)

        // 🔥 4. Backfill orphaned classes (Debug Only)
        if (BuildConfig.DEBUG) {
            val prefs = getSharedPreferences("azura_dev_prefs", android.content.Context.MODE_PRIVATE)
            if (!prefs.getBoolean("backfill_v1_done", false)) {
                lifecycleScope.launch {
                    backfillUseCase.execute()
                    prefs.edit().putBoolean("backfill_v1_done", true).apply()
                }
            }
        }

        // 🔥 2. Tahan Splash Screen dengan kondisi yang stabil
        splashScreen.setKeepOnScreenCondition { !isBootReady }

        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setupFullscreen()

        // 🔥 3. Pindahkan Background Sync agar tidak berebut CPU saat Start-up
        // Kita beri jeda 2 detik setelah UI tampil
        window.decorView.postDelayed({
            setupBackgroundSync()
        }, 2000)

        setContent {
            AzuraTheme {
                MainApp(onBootReady = { 
                    // Panggil ini saat BootState sudah bukan Loading
                    isBootReady = true 
                })
            }
        }
    }

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "AzuraAutoSync",
            ExistingPeriodicWorkPolicy.KEEP, 
            syncRequest
        )
    }

    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}