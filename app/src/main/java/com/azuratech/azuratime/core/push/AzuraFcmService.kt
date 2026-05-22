package com.azuratech.azuratime.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.azuratech.azuratime.MainActivity
import com.azuratech.azuratime.features.account.domain.repository.AccountRepository
import com.azuratech.azuratime.features.update.ui.UpdateEventBus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚀 AZURA FCM SERVICE (v3.2.0-ai-native)
 * Unified Push Notification Service for attendance and system updates.
 */
@AndroidEntryPoint
class AzuraFcmService : FirebaseMessagingService() {

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var updateEventBus: UpdateEventBus

    private val TAG = "AzuraFCM"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val EXTRA_TRIGGER_UPDATE = "trigger_app_update"
        const val CHANNEL_ID = "azura_system_channel"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Token FCM Baru: $token")

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            serviceScope.launch {
                accountRepository.updateFcmToken(currentUid, token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Pesan masuk dari: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Notifikasi Azura"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Ada pembaruan baru."

        // 🔥 AI Native: Check for update trigger in data payload
        val isUpdateTrigger = remoteMessage.data["type"] == "FORCE_UPDATE" ||
            remoteMessage.data[EXTRA_TRIGGER_UPDATE] == "true"

        if (isUpdateTrigger) {
            updateEventBus.triggerUpdateCheck()
        }

        tampilkanNotifikasi(title, body, isUpdateTrigger)
    }

    private fun tampilkanNotifikasi(title: String, message: String, isUpdateTrigger: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (isUpdateTrigger) {
                putExtra(EXTRA_TRIGGER_UPDATE, true)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Notifications",
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
