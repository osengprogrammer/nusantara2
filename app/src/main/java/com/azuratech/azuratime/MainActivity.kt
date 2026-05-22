package com.azuratech.azuratime

import android.content.Intent
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
import com.azuratech.azuratime.core.ui.theme.AzuraTheme
import com.azuratech.azuratime.core.sync.SyncWorker
import com.azuratech.azuratime.core.push.AzuraFcmService
import com.azuratech.azuratime.features.update.ui.UpdateEventBus
import com.google.firebase.Firebase
import com.google.firebase.appdistribution.appDistribution
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var updateEventBus: UpdateEventBus

    // Menggunakan variabel biasa agar lebih responsif di level sistem
    private var isBootReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔥 1. Pasang Splash Screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        handleUpdateIntent(intent)

        // 🔥 2. Tahan Splash Screen dengan kondisi yang stabil
        splashScreen.setKeepOnScreenCondition { !isBootReady }

        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setupFullscreen()

        // 🔥 FIREBASE IN-APP UPDATES (FOR TESTERS)
        // Checks if a new version is available in App Distribution and prompts the tester.
        Firebase.appDistribution.updateIfNewReleaseAvailable()
            .addOnSuccessListener {
                android.util.Log.d("AzuraApp", "✅ App Distribution: Update check completed successfully.")
            }
            .addOnFailureListener { e ->
                val errorMsg = "Update Check Failed: ${e.message}"
                android.util.Log.e("AzuraApp", "❌ App Distribution Error", e)
                android.widget.Toast.makeText(this, errorMsg, android.widget.Toast.LENGTH_LONG).show()
            }
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
            syncRequest,
        )
    }

    private fun setupFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUpdateIntent(intent)
    }

    private fun handleUpdateIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(AzuraFcmService.EXTRA_TRIGGER_UPDATE, false) == true) {
            updateEventBus.triggerUpdateCheck()
        }
    }
}
