package com.shivzo.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.shivzo.app.repository.NotificationRepository
import com.shivzo.app.service.ShivzoFirebaseMessagingService
import com.shivzo.app.ui.screens.MainFcmScreen
import com.shivzo.app.ui.screens.SplashScreen
import com.shivzo.app.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d("MainActivity", "Notification permission status: $isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createNotificationChannel()
        askNotificationPermission()
        fetchRealFcmToken()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(
                            onSplashTimeout = { showSplash = false }
                        )
                    } else {
                        MainFcmScreen(
                            onRefreshFcmToken = { fetchRealFcmToken(forceRefresh = true) }
                        )
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun fetchRealFcmToken(forceRefresh: Boolean = false) {
        NotificationRepository.setTokenLoading(true)
        try {
            ShivzoApplication.ensureFirebaseInitialized(application)
            val firebaseMessaging = FirebaseMessaging.getInstance()
            firebaseMessaging.isAutoInitEnabled = true

            firebaseMessaging.token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val errorMsg = task.exception?.localizedMessage ?: "Failed to retrieve FCM token"
                    Log.e("MainActivity", "Error fetching FCM registration token", task.exception)
                    NotificationRepository.setTokenError(errorMsg)
                } else {
                    val token = task.result
                    Log.d("MainActivity", "Fetched FCM Token: $token")
                    if (!token.isNullOrBlank()) {
                        NotificationRepository.updateToken(token)
                    } else {
                        NotificationRepository.setTokenError("Received empty FCM token from Firebase")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Exception during FCM token retrieval", e)
            NotificationRepository.setTokenError(e.localizedMessage ?: "Firebase FCM error")
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = "android.permission.POST_NOTIFICATIONS"
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationRepository.init(this)
            val mode = NotificationRepository.ringtoneMode.value
            val channelId = NotificationRepository.getChannelId(mode)

            val soundUri = when (mode) {
                "ALARM" -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                "NOTIFICATION" -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                else -> android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                    ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            }

            val audioUsage = if (mode == "ALARM") {
                android.media.AudioAttributes.USAGE_ALARM
            } else {
                android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE
            }

            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(audioUsage)
                .build()

            val channel = NotificationChannel(
                channelId,
                ShivzoFirebaseMessagingService.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Custom ringtone notifications for Shivzo App"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
