package com.smsforwarder.forwarder

import android.util.Log
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class TelegramForwarder {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Sends a message to the specified Telegram chat.
     * Returns true on success, false on failure.
     */
    suspend fun send(
        botToken: String,
        chatId: String,
        simSlot: Int,
        sender: String,
        body: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (botToken.isBlank() || chatId.isBlank()) {
            Log.w(TAG, "Telegram token or chatId is empty, skipping")
            return@withContext false
        }

        val text = "📱 SIM$simSlot\n📞 $sender\n💬 $body"

        val jsonBody = JsonObject().apply {
            addProperty("chat_id", chatId)
            addProperty("text", text)
        }

        val url = "https://api.telegram.org/bot$botToken/sendMessage"

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            if (!success) {
                Log.e(TAG, "Telegram API error: ${response.code} – ${response.body?.string()}")
            }
            response.close()
            success
        } catch (e: Exception) {
            Log.e(TAG, "Telegram send failed", e)
            false
        }
    }

    companion object {
        private const val TAG = "TelegramForwarder"
    }
}
