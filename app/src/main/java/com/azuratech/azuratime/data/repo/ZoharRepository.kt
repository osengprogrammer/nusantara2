package com.azuratech.azuratime.data.repo

import com.azuratech.azuratime.BuildConfig
import com.azuratech.azuratime.data.local.AppDatabase
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🤖 ZOHAR REPOSITORY
 * Otak AI untuk Azura Time. Mengolah data absensi lokal menjadi wawasan cerdas.
 * 🔥 UPDATE: Sinkronisasi dengan CheckInRecordEntity v2 (LocalDateTime & Long Timestamp)
 */
@Singleton
class ZoharRepository @Inject constructor(
    private val database: AppDatabase
) {
    // Inisialisasi Gemini 1.5 Flash (Cepat & Hemat Token)
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun generateAttendanceInsight(schoolId: String): String = withContext(Dispatchers.IO) {
        try {
            // 1. Ambil data record terbaru dari Room
            val allRecords = database.checkInRecordDao().getAllRecords(schoolId).first() 
            
            if (allRecords.isEmpty()) {
                return@withContext "Belum ada data absensi untuk dianalisis, brother. Semangat terus buat guru-guru di Banyuwangi!"
            }

            // 2. Ambil 30 data terakhir & format agar Zohar mudah membaca
            val recentRecords = allRecords.take(30)
            
            // Format waktu agar lebih manusiawi untuk AI
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val dataContext = recentRecords.joinToString("\n") { record ->
                val timeStr = record.checkInTime?.format(timeFormatter) ?: "Jam tidak tercatat"
                "- Siswa: ${record.name}, Kelas: ${record.className}, Jam: $timeStr, Status: ${record.status}"
            }

            // 3. Prompt Mbois dengan kepribadian Zohar
            val prompt = """
                Halo Gemini, namamu sekarang adalah Zohar, asisten AI cerdas untuk aplikasi Azura Time di Indonesia. 
                Tugasmu adalah menganalisis data absensi mentah di bawah ini. 
                
                Mohon berikan:
                1. Ringkasan singkat kehadiran hari ini (jumlah hadir/terlambat).
                2. Analisis tren (apakah ada kelas yang sering terlambat?).
                3. Satu pesan motivasi mbois untuk guru yang bertugas.
                
                Gunakan gaya bahasa Indonesia yang akrab, santai, tapi tetap sopan.
                
                Data Absensi:
                $dataContext
            """.trimIndent()

            // 4. Kirim ke Langit (Gemini Cloud)
            val response = generativeModel.generateContent(prompt)
            response.text ?: "Zohar sedang termenung melihat kode, coba tanya lagi nanti ya."
            
        } catch (e: Exception) {
            "🚨 Zohar Error: ${e.message}"
        }
    }
}