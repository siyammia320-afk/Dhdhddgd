package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.SettingsManager
import com.example.network.TelegramApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val settingsManager = SettingsManager(context.applicationContext)

            scope.launch {
                try {
                    val smsEnabled = settingsManager.smsEnabledFlow.first()
                    if (!smsEnabled) return@launch

                    val token = settingsManager.botTokenFlow.first()
                    val chatId = settingsManager.chatIdFlow.first()

                    if (token.isBlank() || chatId.isBlank()) return@launch

                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    for (sms in messages) {
                        val sender = sms.displayOriginatingAddress ?: "Unknown"
                        val body = sms.displayMessageBody ?: ""

                        val message = "📩 <b>New SMS Received</b>\n\n" +
                                "👤 <b>From:</b> $sender\n" +
                                "✉️ <b>Message:</b> $body"

                        val success = TelegramApiClient.sendMessageWithRetry(token, chatId, message)
                        if (success) {
                            Log.d("SmsReceiver", "SMS forwarded to Telegram successfully")
                        } else {
                            Log.e("SmsReceiver", "Failed to forward SMS after retries")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error processing SMS", e)
                }
            }
        }
    }
}
