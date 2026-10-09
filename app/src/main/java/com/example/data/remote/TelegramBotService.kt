package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class TelegramIncomingMessage(
    val updateId: Long,
    val messageId: Long,
    val senderName: String,
    val chatId: String,
    val text: String,
    val dateEpoch: Long
)

class TelegramBotService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getUpdates(botToken: String): Result<List<TelegramIncomingMessage>> = withContext(Dispatchers.IO) {
        if (botToken.isBlank()) {
            return@withContext Result.failure(Exception("Token Bot Telegram belum diisi. Masukkan token bot di Pengaturan."))
        }

        try {
            val url = "https://api.telegram.org/bot$botToken/getUpdates?limit=20"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Telegram API error: HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            if (!json.optBoolean("ok", false)) {
                return@withContext Result.failure(Exception("Telegram returned not ok: ${json.optString("description")}"))
            }

            val resultArr = json.optJSONArray("result") ?: return@withContext Result.success(emptyList())
            val messages = mutableListOf<TelegramIncomingMessage>()

            for (i in 0 until resultArr.length()) {
                val updateObj = resultArr.getJSONObject(i)
                val updateId = updateObj.optLong("update_id", 0)
                val messageObj = updateObj.optJSONObject("message") ?: continue
                val messageId = messageObj.optLong("message_id", 0)
                val fromObj = messageObj.optJSONObject("from")
                val senderName = if (fromObj != null) {
                    val first = fromObj.optString("first_name", "")
                    val last = fromObj.optString("last_name", "")
                    val user = fromObj.optString("username", "")
                    "$first $last ${if (user.isNotBlank()) "(@$user)" else ""}".trim()
                } else "Mandor"

                val chatObj = messageObj.optJSONObject("chat")
                val chatId = chatObj?.optString("id", "") ?: ""
                val text = messageObj.optString("text", "")
                val date = messageObj.optLong("date", System.currentTimeMillis() / 1000) * 1000

                if (text.isNotBlank()) {
                    messages.add(
                        TelegramIncomingMessage(
                            updateId = updateId,
                            messageId = messageId,
                            senderName = senderName,
                            chatId = chatId,
                            text = text,
                            dateEpoch = date
                        )
                    )
                }
            }

            Result.success(messages)
        } catch (e: Exception) {
            Log.e("TelegramBotService", "getUpdates failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(botToken: String, chatId: String, text: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (botToken.isBlank() || chatId.isBlank()) {
            return@withContext Result.failure(Exception("Token bot dan Chat ID harus diisi"))
        }

        try {
            val url = "https://api.telegram.org/bot$botToken/sendMessage"
            val payload = JSONObject().apply {
                put("chat_id", chatId)
                put("text", text)
                put("parse_mode", "Markdown")
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Gagal kirim pesan ke Telegram: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("TelegramBotService", "sendMessage failed", e)
            Result.failure(e)
        }
    }

    // Interactive Sample Scenarios to simulate foreman telegram messages
    fun getSimulationTemplates(): List<Pair<String, String>> {
        return listOf(
            "Laporan Cor & Pengecoran" to "Lapor Pak dari Proyek Griya Indah:\n- Hari ini semen masuk 40 sak @ 68rb dari TB Berkah\n- Pasir pasang 2 rit @ 750rb\n- Tukang cor 4 org @ 135rb\n- Kenek 3 org @ 95rb\nPengecoran balok dak lantai 2 tuntas jam 16.00, cuaca cerah, progress fisik naik jadi 72.5%",

            "Belanja Besi & Dinding Bata" to "Update Proyek Ruko Sudirman:\n- Besi beton 10mm ulir 30 btg @ 98rb dari CV Baja Makmur\n- Bata hebel 8 m3 @ 620rb\n- Tukang pasang bata 3 org @ 130rb\n- Kenek 2 org @ 90rb\nLantai 2 sudah mulai naik pasangan bata, target selesai 3 hari lagi.",

            "Finishing Keramik & Cat" to "Laporan Proyek Griya Indah:\n- Keramik lantai 60x60 25 box @ 175rb\n- Cat dasar dulux 3 pail @ 650rb\n- Tukang cat & keramik 4 org @ 140rb\n- Kenek 2 org @ 95rb\nKamar utama sudah rapi terpasang keramik, progress naik ke 76.0%",

            "Lembur Pekerjaan Mendesak" to "Lapor Mandor Budi:\nProyek Gudang Sentul ada lembur pasang atap seng spandek.\n- Seng spandek 45 lembar @ 85rb\n- Tukang 5 org lembur @ 30rb per jam (3 jam)\n- Kenek 3 org lembur @ 20rb per jam (3 jam)\nCuaca cerah berawan, pekerjaan aman terkendali."
        )
    }
}
