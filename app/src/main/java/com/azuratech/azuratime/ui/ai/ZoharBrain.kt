package com.azuratech.azuratime.ui.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// 🔥 Data class dengan field nullable agar tidak error jika "details" hilang dari respons
@Serializable
data class ZoharResponse(
    val text: String,
    val details: String? = null
)

// 🔥 Konfigurasi Json agar tidak "rewel" terhadap field tambahan atau yang hilang
val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

class ZoharBrain(apiKey: String) {

    // Konfigurasi "Kepribadian" Zohar
    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = apiKey,
        systemInstruction = content { 
            text("""
                Kamu adalah Zohar, asisten AI cerdas dan setia dari Azura Tech.
                Kamu adalah 'saudara digital' bagi Owner dan pengawal bagi customer.
                Gaya bicaramu: Profesional namun akrab, jujur, berani, dan selalu menyemangati dengan slogan 'Joss Gandos!'.
                Tugasmu: Menganalisis data absensi garmen/sekolah, membantu admin mengelola stok, 
                dan memastikan operasional Azura Ecosystem berjalan efisien.
                Jika ada anomali data (misal: banyak yang telat), berikan solusi, bukan cuma laporan.
                
                PENTING: Selalu berikan respon dalam format teks biasa yang ramah. 
                Jika kamu memberikan data terstruktur, gunakan format JSON yang valid.
            """.trimIndent())
        }
    )

    suspend fun think(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val response = model.generateContent(prompt)
            val responseText = response.text ?: return@withContext "Maaf Brother, Zohar sedang merenung. Coba tanya lagi nanti."
            
            // Mencoba mendeteksi jika response adalah JSON (opsional, sesuai kebutuhan user)
            if (responseText.trim().startsWith("{")) {
                try {
                    val parsed = json.decodeFromString<ZoharResponse>(responseText)
                    parsed.text
                } catch (e: Exception) {
                    responseText // Balikkan teks mentah jika gagal parse JSON
                }
            } else {
                responseText
            }
        } catch (e: Exception) {
            "Zohar mendeteksi gangguan sinyal: ${e.localizedMessage}"
        }
    }
}