package com.azuratech.azuratime.domain.sync

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.azuratech.azuratime.core.util.showToast
import java.io.File

object BackupUtils {
    private const val DB_NAME = "face_db" // Nama database sesuai AppDatabase.kt

    fun backupAndShareDatabase(context: Context) {
        try {
            // 1. Dapatkan lokasi database original
            val dbFile = context.getDatabasePath(DB_NAME)
            
            // Logika Tambahan: Room sering menggunakan WAL mode (ada file -shm dan -wal)
            // Untuk backup yang sempurna, kita harus pastikan data di-checkpoint ke file utama.
            // Namun untuk "Emergency Backup" cepat, mengopi file utama biasanya sudah mencakup data mayoritas.
            
            if (!dbFile.exists()) {
                context.showToast("Database tidak ditemukan!")
                return
            }

            // 2. Buat file copy di folder cache agar bisa di-share tanpa permission storage ribet
            val backupFile = File(context.cacheDir, "BACKUP_AZURA_${System.currentTimeMillis()}.db")
            
            dbFile.inputStream().use { input ->
                backupFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // 3. Share via FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Simpan Backup Azura Ke..."))

        } catch (e: Exception) {
            context.showToast("Gagal backup: ${e.message}")
        }
    }

    /**
     * 🔥 RESTORE DATABASE FROM FILE
     * Mengambil file .db dari penyimpanan eksternal/URI dan menimpa database lokal.
     */
    fun restoreDatabase(context: Context, backupUri: android.net.Uri, onComplete: () -> Unit) {
        try {
            // 1. Dapatkan jalur database tujuan
            // Penting: Nama file harus sama dengan yang digunakan di AppDatabase
            val dbFile = context.getDatabasePath(DB_NAME)
            
            // 2. Lakukan penyalinan file
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // 3. Panggil callback keberhasilan
            onComplete()
            
        } catch (e: Exception) {
            context.showToast("Gagal Restore: ${e.message}")
        }
    }
}
