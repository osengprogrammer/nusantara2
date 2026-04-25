package com.azuratech.azuratime.domain.sync

import com.azuratech.azuraengine.core.StorageProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupUtils @Inject constructor(
    private val storageProvider: StorageProvider
) {
    fun backupAndShareDatabase() {
        try {
            // 1. Dapatkan lokasi database original
            val dbPath = storageProvider.getDatabasePath(DB_NAME)
            
            // 2. Buat file copy di folder cache agar bisa di-share tanpa permission storage ribet
            val backupFileName = "BACKUP_AZURA_${System.currentTimeMillis()}.db"
            val backupPath = storageProvider.save(ByteArray(0), backupFileName, "cache")
            
            if (storageProvider.copyFile(dbPath, backupPath)) {
                // 3. Share via StorageProvider
                storageProvider.shareFile(backupPath, "Simpan Backup Azura Ke...", "application/octet-stream")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 🔥 RESTORE DATABASE FROM FILE
     * Mengambil file .db dari penyimpanan eksternal/URI dan menimpa database lokal.
     */
    fun restoreDatabase(backupUriString: String, onComplete: () -> Unit) {
        try {
            // 1. Dapatkan jalur database tujuan
            val dbPath = storageProvider.getDatabasePath(DB_NAME)
            
            // 2. Lakukan penyalinan file dengan membaca URI lewat StorageProvider
            val backupBytes = storageProvider.read(backupUriString)
            if (backupBytes.isNotEmpty()) {
                storageProvider.save(backupBytes, DB_NAME, "databases") // Need to ensure \"databases\" is handled or use absolute path
                // Actually, saving directly to dbPath is better.
                java.io.File(dbPath).writeBytes(backupBytes)
                onComplete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val DB_NAME = "azura.db" // Nama database sesuai AppDatabase.kt
    }
}
