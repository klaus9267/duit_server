package duit.server.infrastructure.external.firebase

import com.google.firebase.messaging.*
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

        // ios 설정
        val apnsConfig = ApnsConfig.builder()
            .putHeader("apns-push-type", "alert")
            .putHeader("apns-priority", "10")
            .setAps(
                Aps.builder()
                    .setContentAvailable(true)
                    .setSound("default")
                    .build()
            )
            .build()

        // android 설정
        val androidConfig = AndroidConfig.builder()
            .setPriority(AndroidConfig.Priority.HIGH)
            .build()

        val notification = Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build()

        val messages = deviceTokens.map { token ->
            Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putAllData(data)
                .setApnsConfig(apnsConfig)
                .setAndroidConfig(androidConfig)
                .build()
        }

        FirebaseMessaging.getInstance().sendEach(messages)
    }
}