package com.example.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface TelegramApiService {
    @FormUrlEncoded
    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Field("chat_id") chatId: String,
        @Field("text") text: String,
        @Field("parse_mode") parseMode: String = "HTML"
    ): Response<ResponseBody>

    @POST("bot{token}/getMe")
    suspend fun getMe(
        @Path("token") token: String
    ): Response<ResponseBody>
}

object TelegramApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.telegram.org/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val api: TelegramApiService = retrofit.create(TelegramApiService::class.java)

    suspend fun sendMessageWithRetry(token: String, chatId: String, text: String, maxRetries: Int = 3): Boolean {
        var currentAttempt = 0
        while (currentAttempt < maxRetries) {
            try {
                val response = api.sendMessage(token, chatId, text)
                if (response.isSuccessful) {
                    return true
                }
            } catch (e: Exception) {
                Log.w("TelegramApiClient", "Network error on attempt ${currentAttempt + 1}: ${e.message}")
            }
            currentAttempt++
            if (currentAttempt < maxRetries) {
                kotlinx.coroutines.delay(2000L * currentAttempt)
            }
        }
        return false
    }
}
