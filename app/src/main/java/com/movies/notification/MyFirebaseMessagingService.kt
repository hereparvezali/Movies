package com.movies.notification

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token generated: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received. Data: ${remoteMessage.data}")

        // Retrieve notification title and body from notification payload or data payload
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Movie Explorer"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "Tap to view movie"

        // Extract movie ID from data payload
        val movieId = remoteMessage.data["movie_id"]?.toIntOrNull()
            ?: remoteMessage.data["movieId"]?.toIntOrNull()
            ?: remoteMessage.data["id"]?.toIntOrNull()

        // Display the notification on device
        NotificationHelper.showNotification(applicationContext, title, body, movieId)
    }

    companion object {
        private const val TAG = "FCM_SERVICE"
    }
}
