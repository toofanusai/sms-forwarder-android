package com.smsforwarder.forwarder

import android.util.Log
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WebhookForwarder {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * POSTs an SMS event to the given webhook URL.
     * Returns true on success, false on failure.
     */
    suspend fun send(
        webhookUrl: String,
        sender: String,
        body: String,
        simSlot: Int,
        dateMillis: Long = System.currentTimeMillis()
    ): Boolean = withContext(Dispatchers.IO) {
        if (webhookUrl.isBlank()) {
            Log.w(TAG, "Webhook URL is empty, skipping")
            return@withContext false
        }

        val isoDate = iso8601Format.format(Date(dateMillis))

        val jsonBody = JsonObject().apply {
            addProperty("address", sender)
            addProperty("body", body)
            addProperty("sim", simSlot)
            addProperty("date", isoDate)
        }

        val request = Request.Builder()
            .url(webhookUrl)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            if (!success) {
                Log.e(TAG, "Webhook error: ${response.code} – ${response.body?.string()}")
            }
            response.close()
            success
        } catch (e: Exception) {
            Log.e(TAG, "Webhook send failed for $webhookUrl", e)
            false
        }
    }

    companion object {
        private const val TAG = "WebhookForwarder"
    }
}
