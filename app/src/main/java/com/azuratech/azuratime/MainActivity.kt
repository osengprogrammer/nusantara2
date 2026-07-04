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
import com.azuratech.azuratime.feature.navigation.ui.AppNavigation
import com.azuratech.azuratime.core.sync.SyncWorker
import com.azuratech.azuratime.core.push.AzuraFcmService
import com.azuratech.azuratime.features.update.ui.UpdateEventBus
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), OnMapsSdkInitializedCallback {
    @Inject
    lateinit var syncManager: com.azuratech.azuratime.core.sync.SyncManager

    @Inject
    lateinit var updateEventBus: UpdateEventBus

    private var isBootReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        android.util.Log.i("AzuraApp", "✅ App Started - v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")

        MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)

        handleUpdateIntent(intent)

        splashScreen.setKeepOnScreenCondition { !isBootReady }

        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setupFullscreen()

        window.decorView.postDelayed({
            setupBackgroundSync()
            syncManager.enqueueSync() // 🔥 Trigger immediate sync on start
        }, 2000)

        setContent {
            AzuraTheme {
                AppNavigation()
            }
        }
    }

    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> android.util.Log.d("AzuraApp", "🗺️ Maps SDK: Latest renderer initialized.")
            MapsInitializer.Renderer.LEGACY -> android.util.Log.d("AzuraApp", "🗺️ Maps SDK: Legacy renderer initialized.")
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
