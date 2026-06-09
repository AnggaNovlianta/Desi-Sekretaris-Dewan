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
            Log.e(TAG, "Gemini API key is default or empty! Falling back to simulated response.")
            return@withContext generateSimulatedResponse(prompt)
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
     * Membantu merangkum Notulen Rapat menjadi ringkasan sesuai dengan gaya yang dipilih.
     * Gaya yang tersedia: "Formal" (Resmi), "Humas" (Siaran Pers), atau "Aksi" (Butir Komitmen).
     */
    suspend fun generateMinutesSummary(
        title: String,
        date: String,
        attendees: String,
        rawNotes: String,
        style: String = "Formal"
    ): String {
        val styleInstruction = when (style) {
            "Humas" -> """
Susun ringkasan ini dalam gaya SIARAN PERS / HUMAS DPRD Kota Prabumulih:
- Gunakan bahasa jurnalistik publik yang menarik, informatif, aktif, dan humanis.
- Tulis dalam format tulisan berita formal yang terstruktur (mengandung Judul Berita Utama, Lead Berita yang menarik minat masyarakat, kutipan pernyataan, serta isi pembahasan rapat secara ringkas).
- Pastikan publik mengetahui apa yang diperjuangkan komisi atau legislatif demi kemaslahatan masyarakat Kota Prabumulih.
- Sediakan ringkasan eksekutif 2-3 baris di bagian paling atas.
            """.trimIndent()
            
            "Aksi" -> """
Susun ringkasan ini dalam gaya DAFTAR KOMITMEN & REKOMENDASI TUGAS (ACTION ITEMS):
- Fokuskan murni pada poin-poin tugas, siapa pelaksana tugas (komisi/dinas terkait), dan batas waktu pengerjaan.
- Tuliskan Butir Rekomendasi Kerja dalam format poin-poin yang tegas, jelas, dan berorientasi pada penyelesaian masalah (problem-solving).
- Sertakan tingkat prioritas (Tinggi / Sedang / Rendah) di setiap poin tugas demi pelaksanaan fungsi pengawasan DPRD secara maksimal.
            """.trimIndent()
            
            else -> """
Susun ringkasan ini dalam gaya NOTULEN RESMI SEKRETARIAT DEWAN:
- Gunakan format yang sangat rapi dan formal pemerintahan.
1. **INFORMASI RAPAT** (Nama, Tanggal, Pimpinan Rapat, Peserta)
2. **POIN-POIN KEPUTUSAN UTAMA** (Poin penting yang disepakati secara berurutan)
3. **DAFTAR TINDAK LANJUT (ACTION ITEMS)** (Siapa melakukan apa, beserta perkiraan tenggat waktu jika ada)
- Gunakan bahasa yang objektif, runut, dan berbobot hukum tata kota yang sesuai dengan tata tertib administrasi DPRD.
            """.trimIndent()
        }

        val prompt = """
Buatkan Ringkasan Rapat Resmi untuk DPRD Kota Prabumulih berdasarkan data rapat berikut:
- Nama Rapat: $title
- Tanggal Pelaksanaan: $date
- Daftar Hadir: $attendees
- Catatan Kasar / Jalannya Rapat: 
$rawNotes

Gaya Penulisan Dokumen: Pasifkan instruksi berikut:
$styleInstruction

Pastikan bahasa yang ditulis sepenuhnya dalam Bahasa Indonesia resmi yang santun, tertata, dan profesional.
        """.trimIndent()

        return generateContent(prompt)
    }

    private fun generateSimulatedResponse(prompt: String): String {
        // Extract values from prompt
        val title = "Perihal / Acara:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE).find(prompt)?.groupValues?.get(1)
            ?: "Nama Rapat:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE).find(prompt)?.groupValues?.get(1)
            ?: "Rapat Koordinasi DPRD"
            
        val date = "Hari,\\s*Tanggal:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE).find(prompt)?.groupValues?.get(1)
            ?: "Tanggal Pelaksanaan:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE).find(prompt)?.groupValues?.get(1)
            ?: "Hari Ini"
            
        val time = "Waktu:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE).find(prompt)?.groupValues?.get(1)
            ?: "09:00 WIB"
            
        val location = "Tempat:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE).find(prompt)?.groupValues?.get(1)
            ?: "Gedung Pertemuan Utama DPRD"
            
        val agenda = "Agenda Utama:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE).find(prompt)?.groupValues?.get(1)
            ?: "Pembahasan umum program daerah"
            
        val recipient = "Ditujukan Kepada:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE).find(prompt)?.groupValues?.get(1)
            ?: "Seluruh Anggota DPRD Prabumulih"
            
        val attendees = "Daftar Hadir:\\s*([^\\n]+)".toRegex(RegexOption.IGNORE_CASE).find(prompt)?.groupValues?.get(1)
            ?: "Para Anggota Komisi dan Fraksi DPRD"

        val rawNotes = "Catatan Kasar / Jalannya Rapat:\\s*([\\s\\S]+?)(?=\\n\\n|\\nStyle|\\nGaya|$)".toRegex(RegexOption.IGNORE_CASE).find(prompt)?.groupValues?.get(1)
            ?: "Membahas tindak lanjut program peningkatan layanan publik legislatif."

        val isUndangan = prompt.contains("undangan resmi", ignoreCase = true)
        
        return if (isUndangan) {
            """
            [DEMO MODE: Kunci API Gemini belum dipasang di panel Secrets. Menampilkan simulasi draf undangan...]
            
            DEWAN PERWAKILAN RAKYAT DAERAH KOTA PRABUMULIH
            Sekretariat DPRD, Jl. Jenderal Sudirman No. 1, Kota Prabumulih
            __________________________________________________________________
            
            Nomor  : 005/DPRD-PBM/2026
            Sifat  : Penting / Segera
            Lamp   : -
            Hal    : Undangan Resmi Rapat DPRD - $title
            
            Kepada Yth.
            $recipient
            di Tempat
            
            Dengan hormat,
            Sehubungan dengan kepentingan penyelenggaraan fungsi legislatif DPRD Kota Prabumulih, bersama ini kami mengundang Saudara/i untuk menghadiri Rapat Resmi yang akan dilaksanakan pada:
            
            Hari/Tanggal : $date
            Waktu        : $time
            Tempat       : $location
            Acara        : $title
            Agenda Utama : $agenda
            
            Mengingat pentingnya materi bahasan rapat demi kemajuan legislatif daerah dan kemaslahatan masyarakat Kota Prabumulih, kami sangat mengharapkan kehadiran Bapak/Ibu tepat pada waktunya.
            
            Demikian undangan ini kami sampaikan, atas kerja sama dan kehadirannya diucapkan terima kasih.
            
            Sekretaris DPRD Kota Prabumulih,
            
            
            (..........................................)
            NIP. 19740812 200312 1 002
            """.trimIndent()
        } else {
            val isHumas = prompt.contains("SIARAN PERS", ignoreCase = true)
            val isAksi = prompt.contains("DAFTAR KOMITMEN", ignoreCase = true)
            
            if (isHumas) {
                """
                [DEMO MODE: Kunci API Gemini belum dipasang di panel Secrets. Menampilkan simulasi ringkasan Humas...]
                
                SIARAN PERS: DPRD KOTA PRABUMULIH AKSELERASI KEGIATAN "${title.uppercase()}" DEMI RAKYAT
                
                PRABUMULIH, $date – Sekretariat Dewan Perwakilan Rakyat Daerah (DPRD) Kota Prabumulih secara resmi melangsungkan rapat kerja komprehensif terkait "$title". Pertemuan legislatif penting ini dihadiri oleh $attendees dalam rangka mempercepat penyelesaian isu-isu publik daerah.
                
                Rapat berlangsung interaktif dengan mendiskusikan berbagai perbaikan pelayanan administrasi kemasyarakatan Kota Prabumulih. Para pimpinan dewan menekankan pentingnya respons secepatnya terhadap aspirasi konstituen secara konkrit dan terukur.
                
                Kutipan Pembahasan Catatan Rapat: 
                "$rawNotes"
                
                DPRD Kota Prabumulih berkomitmen mengawal setiap butir rekomendasi dari masyarakat sehingga realisasi tata pemerintahan berjalan transparan dan berpihak kepada kepentingan khalayak umum.
                """.trimIndent()
            } else if (isAksi) {
                """
                [DEMO MODE: Kunci API Gemini belum dipasang di panel Secrets. Menampilkan simulasi Daftar Komitmen...]
                
                DAFTAR KOMITMEN & REKOMENDASI TUGAS (ACTION ITEMS) RAPAT:
                Nama Rapat  : $title
                Tanggal     : $date
                Daftar Hadir: $attendees
                
                Berikut adalah butir keputusan strategis dan daftar tugas (problem-solving) dari pembahasan rapat:
                
                1. Penanganan Segera Agenda Utama (PRIORITAS: TINGGI)
                   - Deskripsi: Melaksanakan percepatan penyesuaian materi pembicaraan "$title" dengan merujuk pada catatan diskusi: "$rawNotes".
                   - Penanggung Jawab: Fraksi & Komisi DPRD Prabumulih terkait
                   - Batas Waktu: Segera dalam minggu ini
                
                2. Sinergi Teknis Lintas Bidang (PRIORITAS: SEDANG)
                   - Deskripsi: Melakukan koordinasi teknis lanjutan bersama mitra kerja dinas eksekutif daerah.
                   - Penanggung Jawab: Sekretariat DPRD (Sekwan)
                   - Batas Waktu: 7 Hari Kerja
                
                3. Penyusunan Laporan Pertanggungjawaban (PRIORITAS: RENDAH)
                   - Deskripsi: Memformulasikan notula resmi ini ke dalam arsip digital sistem DESI.
                   - Penanggung Jawab: Staff Pelaksana Notulensi
                   - Batas Waktu: 3 Hari Kerja
                """.trimIndent()
            } else {
                """
                [DEMO MODE: Kunci API Gemini belum dipasang di panel Secrets. Menampilkan simulasi Notulen Resmi...]
                
                DOKUMEN NOTULEN RESMI SEKRETARIAT DEWAN
                DPRD KOTA PRABUMULIH
                
                1. INFORMASI RAPAT
                   - Nama Rapat : $title
                   - Tanggal    : $date
                   - Peserta    : $attendees
                
                2. POIN-POIN KEPUTUSAN UTAMA
                   - Pimpinan sidang mengesahkan agenda pembahasan terkait "$title".
                   - Para anggota legislatif yang hadir merespons aktif jalannya rapat dengan menyampaikan pokok pikiran berikut: "$rawNotes".
                   - Keputusan akhir disepakati secara musyawarah dan mufakat oleh segenap unsur pimpinan dan perwakilan fraksi dewan.
                
                3. DAFTAR TINDAK LANJUT (ACTION ITEMS)
                   - Sekretaris Dewan mengumumkan hasil kesepakatan notula kepada pihak terkait.
                   - Seluruh saran komisi dicatat secara administratif untuk diteruskan sebagai rekomendasi resmi parlemen DPRD Kota Prabumulih.
                
                Dokumen ini disusun dan dirangkum secara otomatis oleh Asisten AI DPRD Kota Prabumulih (Desi).
                """.trimIndent()
            }
        }
    }
}
