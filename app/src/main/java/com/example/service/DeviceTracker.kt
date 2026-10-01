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
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var trackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startTracking(firebaseUrl: String) {
        if (firebaseUrl.isBlank()) {
            Log.w("DeviceTracker", "Firebase URL is blank, tracking not started")
            return
        }
        trackingJob?.cancel()
        trackingJob = scope.launch {
            // Send immediately on start
            try {
                sendTelemetry(firebaseUrl)
            } catch (e: Exception) {
                Log.e("DeviceTracker", "Initial telemetry send error", e)
            }

            while (isActive) {
                delay(30_000L) // Every 30 seconds
                try {
                    sendTelemetry(firebaseUrl)
                } catch (e: Exception) {
                    Log.e("DeviceTracker", "Periodic telemetry send error", e)
                }
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
    }

    private fun sendTelemetry(baseUrl: String) {
        val rawDeviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        // Sanitize deviceId for Firebase key (remove invalid chars: . # $ [ ] /)
        val deviceId = rawDeviceId.replace(Regex("[.#$\\[\\]/]"), "_")

        val manufacturer = Build.MANUFACTURER ?: ""
        val model = Build.MODEL ?: ""
        val rawName = "$manufacturer $model".trim()
        val deviceName = if (rawName.isEmpty()) "Android Device" else rawName.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase() else it.toString() 
        }

        val battery = getBatteryPercentage(context)
        val timestamp = System.currentTimeMillis()

        val json = JSONObject().apply {
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("battery", battery)
            put("lastSeen", timestamp)
            put("status", "Online")
        }

        val cleanUrl = baseUrl.trimEnd('/')
        val url = "$cleanUrl/devices/$deviceId.json"

        Log.d("DeviceTracker", "Sending telemetry to: $url")

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("DeviceTracker", "Telemetry successfully sent for device: $deviceId")
                } else {
                    Log.e("DeviceTracker", "Failed to update telemetry. Code: ${response.code}, Message: ${response.message}, Body: ${response.body?.string()}")
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceTracker", "Exception sending telemetry", e)
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
