package com.example.service

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.SettingsManager
import com.example.network.TelegramApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TelegramNotificationListenerService : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(applicationContext)
        Log.d("TelegramNS", "NotificationListener created")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // Ignore our own notifications to avoid infinite loops
        if (sbn.packageName == packageName) return

        scope.launch {
            try {
                val notificationEnabled = settingsManager.notificationEnabledFlow.first()
                if (!notificationEnabled) return@launch

                val token = settingsManager.botTokenFlow.first()
                val chatId = settingsManager.chatIdFlow.first()

                if (token.isBlank() || chatId.isBlank()) return@launch

                val extras = sbn.notification.extras
                val title = extras.getString(Notification.EXTRA_TITLE) ?: "No Title"
                val text = extras.getString(Notification.EXTRA_TEXT) ?: extras.getString(Notification.EXTRA_BIG_TEXT) ?: "No Content"
                
                val pm = packageManager
                val appLabel = try {
                    val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    sbn.packageName
                }

                val message = "🔔 <b>New Notification</b>\n\n" +
                        "📱 <b>App:</b> $appLabel\n" +
                        "📌 <b>Title:</b> $title\n" +
                        "💬 <b>Text:</b> $text"

                val success = TelegramApiClient.sendMessageWithRetry(token, chatId, message)
                if (success) {
                    Log.d("TelegramNS", "Notification forwarded successfully")
                } else {
                    Log.e("TelegramNS", "Failed to forward notification after retries")
                }
            } catch (e: Exception) {
                Log.e("TelegramNS", "Error forwarding notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
