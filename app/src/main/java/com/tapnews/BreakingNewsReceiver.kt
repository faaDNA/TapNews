package com.example.tapnews

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.tapnews.R

class BreakingNewsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Breaking News"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Berita penting baru!"
        showNotification(context, title, message)
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "breaking_news_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Breaking News", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_news)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
    }
}

class BreakingNewsFirebaseService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val title = data["title"] ?: "Breaking News"
        val message = data["message"] ?: "Berita penting baru!"
        val intent = Intent(this, BreakingNewsReceiver::class.java).apply {
            putExtra(BreakingNewsReceiver.EXTRA_TITLE, title)
            putExtra(BreakingNewsReceiver.EXTRA_MESSAGE, message)
        }
        sendBroadcast(intent)
    }
}
