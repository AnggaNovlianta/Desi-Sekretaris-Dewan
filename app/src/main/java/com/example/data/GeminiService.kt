package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Menggunakan Gemini 3.5 Flash untuk menghasilkan teks sesuai prompt dan instruksi sistem.
     */
    suspend fun generateContent(
        prompt: String,
        systemInstruction: String = "Anda adalah Desi (Asisten Sekretaris Dewan DPRD Kota Prabumulih). Bantu sekretaris dewan dalam merumuskan draf undangan resmi, notulen rapat, ringkasan, atau agenda dengan format formal, santun, dan profesional dalam Bahasa Indonesia yang baik dan benar."
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is default or empty!")
            return@withContext "Kunci API Gemini tidak dikonfigurasi dengan benar. Silakan atur GEMINI_API_KEY di panel Secrets."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        try {
            // Membangun payload JSON menggunakan JSONObject bawaan Android
            val root = JSONObject()
            
            // Contents list
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            // System Instruction
            if (systemInstruction.isNotEmpty()) {
                val sysInstObj = JSONObject()
                val sysPartsArray = JSONArray()
                val sysPartObj = JSONObject()
                sysPartObj.put("text", systemInstruction)
                sysPartsArray.put(sysPartObj)
                sysInstObj.put("parts", sysPartsArray)
                root.put("systemInstruction", sysInstObj)
            }

            // Generation Config
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.3) // Suhu rendah agar format rapi dan tidak terlalu kreatif
            root.put("generationConfig", generationConfig)

            val requestBody = root.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request Gagal: Kode ${response.code}, Body: $errorBody")
                    return@withContext "Gagal terhubung ke Gemini AI: Kode ${response.code}. Pastikan API Key valid."
                }

                val responseBodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Response: $responseBodyStr")
                
                val jsonResponse = JSONObject(responseBodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "Tidak ada konten yang dihasilkan.")
                        }
                    }
                }
                
                return@withContext "Format respon Gemini AI tidak dikenal."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during generation", e)
            return@withContext "Error koneksi Gemini AI: ${e.localizedMessage ?: "Koneksi tidak diketahui"}"
        }
    }

    /**
     * Membantu memformat draf Undangan Rapat DPRD Kota Prabumulih.
     */
    suspend fun generateInvitationDraft(
        title: String,
        date: String,
        time: String,
        location: String,
        agenda: String,
        recipient: String
    ): String {
        val prompt = """
Buatkan draf surat undangan resmi formal dari Sekretariat Dewan Perwakilan Rakyat Daerah (DPRD) Kota Prabumulih berdasarkan data berikut:
- Perihal / Acara: $title
- Hari, Tanggal: $date
- Waktu: $time
- Tempat: $location
- Agenda Utama: $agenda
- Ditujukan Kepada: $recipient

Gunakan bahasa dinas pemerintahan yang resmi, formal, santun, lengkap dengan salam pembuka, rincian detail acara, salam penutup, dan tanda tangan Sekretaris DPRD Kota Prabumulih (nama dikosongkan/menggunakan placeholder). Harap sertakan kop surat formal DPRD Kota Prabumulih di bagian atas draf.
        """.trimIndent()
        
        return generateContent(prompt)
    }

    /**
     * Membantu merangkum Notulen Rapat menjadi ringkasan poin-poin keputusan dan daftar tindak lanjut (Action Items).
     */
    suspend fun generateMinutesSummary(
        title: String,
        date: String,
        attendees: String,
        rawNotes: String
    ): String {
        val prompt = """
Buatkan Ringkasan Notulen Rapat Resmi untuk DPRD Kota Prabumulih berdasarkan data rapat berikut:
- Nama Rapat: $title
- Tanggal Pelaksanaan: $date
- Daftar Hadir: $attendees
- Catatan Kasar / Jalannya Rapat: 
$rawNotes

Susun ringkasan ini dengan format yang sangat rapi dan formal pemerintahan:
1. **INFORMASI RAPAT** (Nama, Tanggal, Pimpinan Rapat, Peserta)
2. **POIN-POIN KEPUTUSAN UTAMA** (Poin penting yang disepakati secara berurutan)
3. **DAFTAR TINDAK LANJUT (ACTION ITEMS)** (Siapa melakukan apa, beserta perkiraan tenggat waktu jika ada)

Pastikan bahasa yang ditulis adalah formal, ringkas, mudah dibaca, dan berbobot profesional.
        """.trimIndent()

        return generateContent(prompt)
    }
}
