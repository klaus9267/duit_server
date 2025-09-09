package duit.server.infrastructure.external.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.springframework.stereotype.Service

@Service
class FCMService {

    fun sendAlarm(
        deviceToken: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        val notification = Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build()

        val message = Message.builder()
            .setToken(deviceToken)
            .setNotification(notification)
            .putAllData(data)
            .build()

        FirebaseMessaging.getInstance().send(message)
    }

    fun sendAlarms(
        deviceTokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        if (deviceTokens.isEmpty()) {
            return
        }

        deviceTokens.forEach { token ->
            val notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build()

            val message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putAllData(data)
                .build()

            FirebaseMessaging.getInstance().send(message)
        }
    }
}