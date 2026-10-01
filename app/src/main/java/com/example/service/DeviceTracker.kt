package com.example.service

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DeviceTracker(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var trackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startTracking(firebaseUrl: String) {
        if (firebaseUrl.isBlank()) return
        trackingJob?.cancel()
        trackingJob = scope.launch {
            while (isActive) {
                try {
                    sendTelemetry(firebaseUrl)
                } catch (e: Exception) {
                    Log.e("DeviceTracker", "Error sending telemetry", e)
                }
                delay(30_000L) // Every 30 seconds
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
    }

    private fun sendTelemetry(baseUrl: String) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".capitalize()
        val battery = getBatteryPercentage(context)
        val timestamp = System.currentTimeMillis()

        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("battery", battery)
            put("lastSeen", timestamp)
            put("status", "Online")
        }

        // Clean up base url and construct REST endpoint
        val cleanUrl = baseUrl.trimEnd('/')
        val url = "$cleanUrl/devices/$deviceId.json"

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("DeviceTracker", "Failed to update telemetry: ${response.code}")
            }
        }
    }

    private fun getBatteryPercentage(context: Context): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            -1
        }
    }
}
