package com.azuratech.azuratime.core.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.azuratech.azuratime.MainActivity
import com.azuratech.azuratime.R // Pastikan ini mengarah ke R project kamu
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AzuraMessagingService : FirebaseMessagingService() {

    private val TAG = "AzuraFCM"

    // Fungsi ini terpanggil kalau token HP berubah atau baru diinstal
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Token FCM Baru: $token")
        // Nanti kita akan kirim token ini ke Firestore agar sistem tahu alamat HP Ortu
    }

    // Fungsi ini menangkap notifikasi saat aplikasi sedang TERTUTUP atau TERBUKA
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "Pesan masuk dari: ${remoteMessage.from}")

        // Ambil data dari notifikasi
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Notifikasi Azura"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Ada pembaruan baru."

        tampilkanNotifikasi(title, body)
    }

    private fun tampilkanNotifikasi(title: String, message: String) {
        val channelId = "azura_parent_channel"
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // Ganti icon ini dengan icon logo Azura Parent kamu
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Untuk Android Oreo (8.0) ke atas, WAJIB buat Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifikasi Kehadiran Anak",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Tampilkan notifikasi dengan ID unik (waktu saat ini)
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}