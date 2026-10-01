package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "telegram_forwarder_prefs")

class SettingsManager(private val context: Context) {

    companion object {
        val BOT_TOKEN_KEY = stringPreferencesKey("bot_token")
        val CHAT_ID_KEY = stringPreferencesKey("chat_id")
        val FIREBASE_URL_KEY = stringPreferencesKey("firebase_url")
        val SMS_ENABLED_KEY = booleanPreferencesKey("sms_enabled")
        val NOTIFICATION_ENABLED_KEY = booleanPreferencesKey("notification_enabled")
        val SERVICE_RUNNING_KEY = booleanPreferencesKey("service_running")

        const val DEFAULT_BOT_TOKEN = "8442048589:AAFYCeNkTudjMKgj2R1e4SqZoAwJwu3uJwE"
        const val DEFAULT_CHAT_ID = "7787612625"
        const val DEFAULT_FIREBASE_URL = "https://smsmbot-default-rtdb.firebaseio.com"
    }

    val botTokenFlow: Flow<String> = context.dataStore.data
        .map { preferences -> 
            val token = preferences[BOT_TOKEN_KEY]
            if (token.isNullOrBlank()) DEFAULT_BOT_TOKEN else token
        }

    val chatIdFlow: Flow<String> = context.dataStore.data
        .map { preferences -> 
            val chatId = preferences[CHAT_ID_KEY]
            if (chatId.isNullOrBlank()) DEFAULT_CHAT_ID else chatId
        }

    val firebaseUrlFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            val url = preferences[FIREBASE_URL_KEY]
            if (url.isNullOrBlank()) DEFAULT_FIREBASE_URL else url
        }

    val smsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SMS_ENABLED_KEY] ?: true }

    val notificationEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[NOTIFICATION_ENABLED_KEY] ?: true }

    val serviceRunningFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SERVICE_RUNNING_KEY] ?: true }

    suspend fun setFirebaseUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[FIREBASE_URL_KEY] = url.trim()
        }
    }

    suspend fun setSmsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SMS_ENABLED_KEY] = enabled
        }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_ENABLED_KEY] = enabled
        }
    }

    suspend fun setServiceRunning(running: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SERVICE_RUNNING_KEY] = running
        }
    }
}
