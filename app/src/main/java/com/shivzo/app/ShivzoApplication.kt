package com.shivzo.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.shivzo.app.repository.NotificationRepository

class ShivzoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationRepository.init(this)
        ensureFirebaseInitialized(this)
    }

    companion object {
        fun ensureFirebaseInitialized(application: Application) {
            try {
                if (FirebaseApp.getApps(application).isEmpty()) {
                    val app = FirebaseApp.initializeApp(application)
                    if (app == null) {
                        initializeWithExplicitOptions(application)
                    }
                }
            } catch (e: Exception) {
                Log.w("ShivzoApp", "Default FirebaseApp init failed, using explicit options: ${e.message}")
                initializeWithExplicitOptions(application)
            }
        }

        private fun initializeWithExplicitOptions(application: Application) {
            try {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:307746251164:android:d129d1add27daf627ca859")
                    .setApiKey("AIzaSyDU6cFCi6SN6SCKSmBbp4Q76aggcc1VZX4")
                    .setProjectId("shivzo-notification")
                    .setGcmSenderId("307746251164")
                    .setStorageBucket("shivzo-notification.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(application, options)
                Log.d("ShivzoApp", "Firebase initialized with explicit options")
            } catch (ex: Exception) {
                Log.e("ShivzoApp", "Explicit Firebase initialization failed", ex)
            }
        }
    }
}
