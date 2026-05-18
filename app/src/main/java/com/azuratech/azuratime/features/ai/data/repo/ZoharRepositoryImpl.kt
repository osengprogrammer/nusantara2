package com.azuratech.azuratime.features.ai.data.repo

import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.BuildConfig
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.ai.domain.repository.ZoharRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🤖 ZOHAR REPOSITORY IMPLEMENTATION
 */
@Singleton
class ZoharRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
) : ZoharRepository {
    // Inisialisasi Gemini 1.5 Flash (Cepat & Hemat Token)
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
    )

    override suspend fun generateAttendanceInsight(schoolId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Ambil data record terbaru dari Room
            val allRecords = database.attendanceRecordDao().getAllRecords(schoolId).first()

            if (allRecords.isEmpty()) {
                return@withContext Result.Success("Belum ada data absensi untuk dianalisis, brother. Semangat terus buat guru-guru di Banyuwangi!")
            }

            // 2. Ambil 30 data terakhir & format agar Zohar mudah membaca
            val recentRecords = allRecords.take(30)

            // Format waktu agar lebih manusiawi untuk AI
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val dataContext = recentRecords.joinToString("\n") { record ->
                val timeStr = record.attendanceTime?.format(timeFormatter) ?: "Jam tidak tercatat"
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
            Result.Success(response.text ?: "Zohar sedang termenung melihat kode, coba tanya lagi nanti ya.")
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun askZohar(question: String, schoolId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val allRecords = database.attendanceRecordDao().getAllRecords(schoolId).first()
            val recentLogs = allRecords.take(10)
            val contextData = if (recentLogs.isEmpty()) {
                "Belum ada data absensi."
            } else {
                recentLogs.joinToString("\n") {
                    "${it.name} status ${it.status} pada ${it.attendanceDate}"
                }
            }

            val fullPrompt = """
                Kamu adalah Zohar, asisten AI cerdas dan setia dari Azura Tech.
                Gaya bicaramu: Profesional namun akrab, jujur, berani, dan selalu menyemangati dengan slogan 'Joss Gandos!'.
                
                Data Absensi Terbaru:
                $contextData
                
                Pertanyaan Owner:
                $question
            """.trimIndent()

            val response = generativeModel.generateContent(fullPrompt)
            Result.Success(response.text ?: "Maaf Brother, Zohar sedang merenung. Coba tanya lagi nanti.")
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
