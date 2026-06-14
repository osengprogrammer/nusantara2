package com.azuratech.azuratime.features.ai.data.repo

import android.content.Context
import com.azuratech.azuraengine.result.Result
import com.azuratech.azuraengine.result.AppError
import com.azuratech.azuratime.BuildConfig
import com.azuratech.azuratime.R
import com.azuratech.azuratime.core.data.local.AppDatabase
import com.azuratech.azuratime.features.ai.domain.repository.ZoharRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
) : ZoharRepository {
    // Inisialisasi Gemini 1.5 Flash (Cepat & Hemat Token)
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        requestOptions = RequestOptions(apiVersion = "v1beta"),
    )

    override suspend fun generateAttendanceInsight(schoolId: String): Result<String> = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return@withContext Result.Failure(AppError.Network(context.getString(R.string.gemini_api_key_missing)))
        }
        try {
            // 1. Ambil data context sekolah (Kelas & Siswa)
            val allRecords = database.attendanceRecordDao().getAllRecords(schoolId).first()
            val classes = database.schoolClassDao().getClassesFlow(schoolId).first()
            val studentProfiles = database.studentDao().getStudentProfilesFlow(schoolId).first()

            if (allRecords.isEmpty()) {
                return@withContext Result.Success(context.getString(R.string.zohar_no_data))
            }

            // 2. Ambil 50 data terakhir untuk analisis tren lebih dalam
            val recentRecords = allRecords.take(50)
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val recordSummary = recentRecords.joinToString("\n") { record ->
                val timeStr = record.attendanceTime?.format(timeFormatter) ?: "Time not recorded"
                "- ${record.name} (${record.className}): $timeStr [$record.status]"
            }

            val schoolContext = """
                School Information:
                - Total Registered Students: ${studentProfiles.size}
                - Total Classes: ${classes.size}
                - Class List: ${classes.joinToString(", ") { it.name }}
            """.trimIndent()

            // 3. Prompt Mbois Proaktif dengan kepribadian Zohar
            val prompt = """
                Halo Gemini, your name is now Zohar, a smart, loyal, and brave AI assistant from Azura Tech for the Azura Time application in Indonesia.

                Your task is to analyze raw attendance data and provide "Mbois" (cool) Insights to the Owner/Principal.

                $schoolContext

                Last 50 Attendance Records:
                $recordSummary

                Please provide output in the following format:
                1. *Short Summary*: (Example: Total present, late, and comparison with total students).
                2. *Trend & Issue Analysis*: (Example: Is there a specific class with notable lateness? Peak hours?).
                3. *Proactive Recommendations*: (Example: Suggestions to move scan times or evaluate transportation in certain classes).
                4. *Motivational Slogan*: (One characteristic Zohar motivational sentence that is brave and friendly).

                Use a friendly, relaxed Indonesian-style language (using terms like 'Brother', 'Mbois', 'Joss Gandos'), but keep the data analysis sharp.
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            Result.Success(response.text ?: "AI service temporarily unavailable. Please try again later.")
        } catch (e: Exception) {
            android.util.Log.e("ZoharRepo", "AI Error: ${e.message}", e)
            Result.Failure(AppError.Network("AI service temporarily unavailable. Please try again later."))
        }
    }

    override suspend fun askZohar(question: String, schoolId: String): Result<String> = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return@withContext Result.Failure(AppError.Network(context.getString(R.string.gemini_api_key_missing)))
        }
        try {
            val allRecords = database.attendanceRecordDao().getAllRecords(schoolId).first()
            val studentProfiles = database.studentDao().getStudentProfilesFlow(schoolId).first()
            val recentLogs = allRecords.take(20)

            val contextData = if (recentLogs.isEmpty()) {
                "No attendance data yet."
            } else {
                recentLogs.joinToString("\n") {
                    "${it.name} (${it.className}) status ${it.status} on ${it.attendanceDate} at ${it.attendanceTime}"
                }
            }

            val fullPrompt = """
                You are Zohar, a smart, loyal, and brave AI assistant from Azura Tech.
                Owner is calling you for a discussion. Use speaking style: Professional yet very friendly, honest, brave, and always encouraging.

                School Context:
                - Total Students: ${studentProfiles.size}
                - Last 20 Attendance Records:
                $contextData

                Owner's Question:
                $question

                Provide a helpful answer based on the data above (if relevant), and don't forget the 'Joss Gandos!' slogan at the end.
            """.trimIndent()

            val response = generativeModel.generateContent(fullPrompt)
            Result.Success(response.text ?: "AI service temporarily unavailable. Please try again later.")
        } catch (e: Exception) {
            android.util.Log.e("ZoharRepo", "AI Error: ${e.message}", e)
            Result.Failure(AppError.Network("AI service temporarily unavailable. Please try again later."))
        }
    }
}
