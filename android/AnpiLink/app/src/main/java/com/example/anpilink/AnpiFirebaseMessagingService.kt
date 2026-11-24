package com.example.anpilink

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCMメッセージを受信し、通知を表示する。
 */
class AnpiFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("AnpiLinkFCM", "FCMメッセージ受信")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "AnpiLink"

        val body = message.notification?.body
            ?: message.data["body"]

        if (body.isNullOrBlank()) {
            return
        }

        createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager =
            NotificationManagerCompat.from(this)

        if (!notificationManager.areNotificationsEnabled()) {
            return
        }

        // 通知を押すとアプリを開く。
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(body)
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(
            message.messageId?.hashCode() ?: 1001,
            notification
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // トークン自体はログや作業データへ保存しない。
        Log.d(
            "AnpiLinkFCM",
            "FCMトークン更新。バックグラウンド登録を予約します。"
        )

        FcmTokenSyncWorker.enqueue(applicationContext)
    }

    companion object {
        const val CHANNEL_ID = "anpilink_notifications"

        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AnpiLinkのお知らせ",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "ログイン期限などのお知らせ"
            }

            val manager = context.getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }
}