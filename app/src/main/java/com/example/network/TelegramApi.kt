package com.example.network

import android.util.Log
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object TelegramApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun sendMessageWithRetry(token: String, chatId: String, text: String, maxRetries: Int = 3): Boolean {
        var currentAttempt = 0
        val url = "https://api.telegram.org/bot$token/sendMessage"

        val json = JSONObject().apply {
            put("chat_id", chatId)
            put("text", text)
            put("parse_mode", "HTML")
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()

        while (currentAttempt < maxRetries) {
            try {
                val body = json.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("TelegramApiClient", "Message sent successfully")
                        return true
                    } else {
                        val errorBody = response.body?.string() ?: ""
                        Log.e("TelegramApiClient", "Telegram error: HTTP ${response.code} - $errorBody")
                    }
                }
            } catch (e: Exception) {
                Log.w("TelegramApiClient", "Network error on attempt ${currentAttempt + 1}: ${e.message}")
            }
            currentAttempt++
            if (currentAttempt < maxRetries) {
                delay(2000L * currentAttempt)
            }
        }
        return false
    }
}
