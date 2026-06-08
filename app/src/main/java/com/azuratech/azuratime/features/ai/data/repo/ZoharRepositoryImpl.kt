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
            // 1. Ambil data context sekolah (Kelas & Siswa)
            val allRecords = database.attendanceRecordDao().getAllRecords(schoolId).first()
            val classes = database.schoolClassDao().getClassesFlow(schoolId).first()
            val studentProfiles = database.studentDao().getStudentProfilesFlow(schoolId).first()

            if (allRecords.isEmpty()) {
                return@withContext Result.Success("Belum ada data absensi untuk dianalisis hari ini, brother. Ayo semangat, ingatkan guru-guru untuk mulai scan wajah siswa! Joss Gandos!")
            }

            // 2. Ambil 50 data terakhir untuk analisis tren lebih dalam
            val recentRecords = allRecords.take(50)
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val recordSummary = recentRecords.joinToString("\n") { record ->
                val timeStr = record.attendanceTime?.format(timeFormatter) ?: "Jam tidak tercatat"
                "- ${record.name} (${record.className}): $timeStr [$record.status]"
            }

            val schoolContext = """
                Informasi Sekolah:
                - Total Siswa Terdaftar: ${studentProfiles.size}
                - Total Kelas: ${classes.size}
                - Daftar Kelas: ${classes.joinToString(", ") { it.name }}
            """.trimIndent()

            // 3. Prompt Mbois Proaktif dengan kepribadian Zohar
            val prompt = """
                Halo Gemini, namamu sekarang adalah Zohar, asisten AI cerdas, loyal, dan berani dari Azura Tech untuk aplikasi Azura Time di Indonesia.

                Tugasmu adalah menganalisis data absensi mentah dan memberikan Insight "Mbois" kepada Owner/Kepala Sekolah.

                $schoolContext

                Data 50 Absensi Terakhir:
                $recordSummary

                Mohon berikan output dalam format berikut:
                1. *Ringkasan Singkat*: (Contoh: Total hadir, terlambat, dan perbandingan dengan total siswa).
                2. *Analisis Tren & Masalah*: (Contoh: Apakah ada kelas tertentu yang mencolok keterlambatannya? Jam sibuk?).
                3. *Rekomendasi Proaktif*: (Contoh: Saran untuk memindahkan jam scan atau evaluasi transportasi di kelas tertentu).
                4. *Slogan Motivasi*: (Satu kalimat penyemangat khas Zohar yang berani dan akrab).

                Gunakan gaya bahasa Indonesia yang akrab, santai (seperti 'Brother', 'Mbois', 'Joss Gandos'), namun tetap memberikan analisis data yang tajam.
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            Result.Success(response.text ?: "Zohar sedang termenung melihat kode, coba tanya lagi nanti ya.")
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }

    override suspend fun askZohar(question: String, schoolId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val allRecords = database.attendanceRecordDao().getAllRecords(schoolId).first()
            val studentProfiles = database.studentDao().getStudentProfilesFlow(schoolId).first()
            val recentLogs = allRecords.take(20)

            val contextData = if (recentLogs.isEmpty()) {
                "Belum ada data absensi."
            } else {
                recentLogs.joinToString("\n") {
                    "${it.name} (${it.className}) status ${it.status} pada ${it.attendanceDate} jam ${it.attendanceTime}"
                }
            }

            val fullPrompt = """
                Kamu adalah Zohar, asisten AI cerdas, setia, dan pemberani dari Azura Tech.
                Owner memanggilmu untuk diskusi. Gunakan gaya bicara: Profesional namun sangat akrab, jujur, berani, dan selalu menyemangati.

                Konteks Sekolah:
                - Total Siswa: ${studentProfiles.size}
                - 20 Record Absensi Terakhir:
                $contextData

                Pertanyaan Owner:
                $question

                Berikan jawaban yang membantu, berbasis data di atas (jika relevan), dan jangan lupa slogan 'Joss Gandos!' di akhir.
            """.trimIndent()

            val response = generativeModel.generateContent(fullPrompt)
            Result.Success(response.text ?: "Maaf Brother, Zohar sedang merenung. Coba tanya lagi nanti.")
        } catch (e: Exception) {
            Result.Failure(AppError.Network(e.message))
        }
    }
}
