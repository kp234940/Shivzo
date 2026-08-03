package com.shivzo.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.shivzo.app.MainActivity
import com.shivzo.app.R
import com.shivzo.app.repository.NotificationRepository
import com.shivzo.app.repository.ReceivedNotification

class ShivzoFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "ShivzoFCMService"
        const val CHANNEL_NAME = "Shivzo Real-Time Ringtone Notifications"
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New Firebase Messaging Token: $token")
        NotificationRepository.init(this)
        NotificationRepository.updateToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")
        NotificationRepository.init(this)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Shivzo Notification"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "New message received from Firebase Cloud Messaging"

        val payloadMap = remoteMessage.data

        val targetUrl = payloadMap["url"] ?: payloadMap["target_url"]

        val notificationItem = ReceivedNotification(
            title = title,
            body = body,
            payload = payloadMap,
            timestamp = System.currentTimeMillis()
        )

        // 1. Send live update to Compose UI via StateFlow
        NotificationRepository.addNotification(notificationItem)

        // 2. Post Android System Notification (handles sound & vibration cleanly via NotificationChannel)
        showNotification(title, body, targetUrl)
    }

    private fun playCustomRingtoneSound() {
        try {
            val ringtoneUri = getCustomRingtoneUri()
            val ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = false
            }
            ringtone?.play()

            // Vibrate device
            vibrateDevice()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing custom ringtone", e)
        }
    }

    private fun vibrateDevice() {
        try {
            val pattern = longArrayOf(0, 600, 300, 600, 300, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating device", e)
        }
    }

    private fun getCustomRingtoneUri(): Uri {
        val mode = NotificationRepository.ringtoneMode.value
        return when (mode) {
            "ALARM" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            "NOTIFICATION" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    private fun showNotification(title: String, message: String, targetUrl: String?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val mode = NotificationRepository.ringtoneMode.value
        val channelId = NotificationRepository.getChannelId(mode)
        val ringtoneUri = getCustomRingtoneUri()
        val vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioUsage = if (mode == "ALARM") {
                AudioAttributes.USAGE_ALARM
            } else {
                AudioAttributes.USAGE_NOTIFICATION_RINGTONE
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(audioUsage)
                .build()

            val channel = NotificationChannel(
                channelId,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Custom ringtone notifications for Shivzo"
                enableVibration(true)
                this.vibrationPattern = vibrationPattern
                setSound(ringtoneUri, audioAttributes)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!targetUrl.isNullOrBlank()) {
                putExtra("EXTRA_TARGET_URL", targetUrl)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSound(ringtoneUri)
            .setVibrate(vibrationPattern)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
