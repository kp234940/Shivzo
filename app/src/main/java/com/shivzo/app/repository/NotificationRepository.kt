package com.shivzo.app.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

data class ReceivedNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val payload: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

object NotificationRepository {
    private const val PREFS_NAME = "shivzo_notification_prefs"
    private const val KEY_RINGTONE_MODE = "key_ringtone_mode"
    private const val KEY_FCM_TOKEN = "key_fcm_token"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    private val _notifications = MutableStateFlow<List<ReceivedNotification>>(emptyList())
    val notifications: StateFlow<List<ReceivedNotification>> = _notifications.asStateFlow()

    private val _latestNotification = MutableStateFlow<ReceivedNotification?>(null)
    val latestNotification: StateFlow<ReceivedNotification?> = _latestNotification.asStateFlow()

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _isTokenLoading = MutableStateFlow<Boolean>(false)
    val isTokenLoading: StateFlow<Boolean> = _isTokenLoading.asStateFlow()

    private val _tokenError = MutableStateFlow<String?>(null)
    val tokenError: StateFlow<String?> = _tokenError.asStateFlow()

    // Ringtone Sound Setting: "RINGTONE", "NOTIFICATION", "ALARM"
    private val _ringtoneMode = MutableStateFlow<String>("RINGTONE")
    val ringtoneMode: StateFlow<String> = _ringtoneMode.asStateFlow()

    fun init(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        val sp = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        val savedMode = sp.getString(KEY_RINGTONE_MODE, "RINGTONE") ?: "RINGTONE"
        _ringtoneMode.value = savedMode

        val savedToken = sp.getString(KEY_FCM_TOKEN, null)
        if (!savedToken.isNullOrEmpty()) {
            _fcmToken.value = savedToken
            sendTokenToServer(savedToken)
        }
    }

    fun getAndroidId(): String {
        return try {
            appContext?.let {
                android.provider.Settings.Secure.getString(it.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun setRingtoneMode(mode: String, context: Context? = null) {
        _ringtoneMode.value = mode
        context?.let {
            init(it)
        }
        prefs?.edit()?.putString(KEY_RINGTONE_MODE, mode)?.apply()
    }

    fun getChannelId(mode: String = _ringtoneMode.value): String {
        return "shivzo_ringtone_channel_${mode.lowercase()}"
    }

    fun updateToken(token: String?) {
        _fcmToken.value = token
        _tokenError.value = null
        _isTokenLoading.value = false
        if (!token.isNullOrEmpty()) {
            prefs?.edit()?.putString(KEY_FCM_TOKEN, token)?.apply()
            sendTokenToServer(token)
        }
    }

    private fun sendTokenToServer(token: String) {
        val androidId = getAndroidId()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://shivzo.in/save_token.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                val postData = "token=" + URLEncoder.encode(token, "UTF-8") +
                        "&fcm_token=" + URLEncoder.encode(token, "UTF-8") +
                        "&android_id=" + URLEncoder.encode(androidId, "UTF-8") +
                        "&device_id=" + URLEncoder.encode(androidId, "UTF-8")
                conn.outputStream.use { os ->
                    os.write(postData.toByteArray())
                }
                val responseCode = conn.responseCode
                Log.d("ShivzoToken", "Posted token and android_id ($androidId) to save_token.php: response $responseCode")
            } catch (e: Exception) {
                Log.e("ShivzoToken", "Error sending token to save_token.php", e)
            }
        }
    }

    fun setTokenLoading(isLoading: Boolean) {
        _isTokenLoading.value = isLoading
        if (isLoading) {
            _tokenError.value = null
        }
    }

    fun setTokenError(error: String?) {
        _tokenError.value = error
        _isTokenLoading.value = false
    }

    fun addNotification(notification: ReceivedNotification) {
        // Sirf latest notification memory me rahegi, disk storage pe kuch bhi save nahi hoga
        _latestNotification.value = notification
        _notifications.value = listOf(notification)
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
        _latestNotification.value = null
    }
}
