package duit.server.infrastructure.external.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.springframework.stereotype.Service

@Service
class FCMService {
    fun sendAlarms(
        deviceTokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        if (deviceTokens.isEmpty()) {
            return
        }

        val notification = Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build()

        val messages = deviceTokens.map { token ->
            Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putAllData(data)
                .build()
        }

        FirebaseMessaging.getInstance().sendEach(messages)
    }
}
