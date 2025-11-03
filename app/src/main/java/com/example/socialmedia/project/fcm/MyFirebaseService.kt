package com.example.socialmedia.project

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.socialmedia.MainActivity
import com.example.socialmedia.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.socialmedia.project.Domain.Enum.MessageType
import com.example.socialmedia.project.Domain.Model.MessageModel
import java.util.*

class MyFirebaseService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "FCM received: ${remoteMessage.data}")

        // Lấy dữ liệu từ payload

        val content = remoteMessage.data["content"] ?: ""
        val senderId = remoteMessage.data["senderId"] ?: ""
        val senderName = remoteMessage.data["senderName"] ?: "Người dùng"
        val mediaUrl = remoteMessage.data["mediaUrl"] ?: ""
        val duration = remoteMessage.data["duration"] ?: ""

        val type = remoteMessage.data["type"]?.lowercase() ?: "text"

        val message = MessageModel(
            messageId = UUID.randomUUID().toString(),
            conversationId = remoteMessage.data["conversationId"] ?: "",
            senderId = senderId,
            senderName = senderName,
            content = if(type == "text") content else "",
            mediaUrl = if(type != "text") mediaUrl else null,
            messageType = when(type) {
                "image" -> MessageType.IMAGE
                "voice" -> MessageType.VOICE
                "text" -> MessageType.TEXT
                else -> MessageType.TEXT
            },
            duration = if(type=="voice") duration else null,
            createdAt = System.currentTimeMillis()
        )

        // Hiển thị notification
        sendNotification(message)
    }

    private fun sendNotification(message: MessageModel) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "chat_messages"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val contentText = when(message.messageType) {
            MessageType.TEXT -> message.content ?: ""
            MessageType.IMAGE -> "${message.senderName} đã gửi một hình ảnh"
            MessageType.VOICE -> "${message.senderName} đã gửi một tin nhắn thoại"
            MessageType.VIDEO -> "${message.senderName} đã gửi một video"
            MessageType.POST_SHARE -> "${message.senderName} đã chia sẻ một bài viết"
            MessageType.REEL_SHARE -> "${message.senderName} đã chia sẻ một reel"
            MessageType.STORY_REPLY -> "${message.senderName} đã trả lời story"
            MessageType.GIF -> "${message.senderName} đã gửi một GIF"
            MessageType.LOCATION -> "${message.senderName} đã gửi vị trí"
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(message.senderName)
            .setContentText(contentText)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo channel nếu Android >= O
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(message.messageId.hashCode(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token: $token")
        // Gửi token lên server nếu cần
    }
}
