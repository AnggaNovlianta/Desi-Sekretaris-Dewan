package com.example.data

import android.util.Log
import com.example.ui.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_SERVICE", "Token FCM Baru: $token")
        // Can be stored in database or processed later
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_SERVICE", "Pesan FCM Diterima dari: ${remoteMessage.from}")

        // 1. Try to extract notification payload
        var title = remoteMessage.notification?.title
        var body = remoteMessage.notification?.body

        // 2. If empty, check data payload
        if (title.isNullOrEmpty() && body.isNullOrEmpty()) {
            title = remoteMessage.data["title"]
            body = remoteMessage.data["body"]
        }

        // If we found any title or body, show a real Android notification channel
        if (!title.isNullOrEmpty() && !body.isNullOrEmpty()) {
            NotificationHelper.showNotification(applicationContext, title, body)
        }
    }
}
