package duit.server.infrastructure.external.firebase
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FCMService {
    private val log = LoggerFactory.getLogger(javaClass)
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

        try {
            val response = FirebaseMessaging.getInstance().sendEach(messages)
            if (response.failureCount > 0) {
                response.responses.forEachIndexed { index, sendResponse ->
                    if (!sendResponse.isSuccessful) {
                        log.warn(
                            "FCM 발송 실패 - token: {}, error: {}",
                            deviceTokens[index].take(20) + "...",
                            sendResponse.exception?.message
                        )
                    }
                }
                log.warn(
                    "FCM 발송 결과 - 총: {}, 성공: {}, 실패: {}",
                    response.responses.size,
                    response.successCount,
                    response.failureCount
                )
            }
        } catch (e: Exception) {
            log.error("FCM 발송 중 예외 발생 - tokens: {}개, title: {}", deviceTokens.size, title, e)
        }
    }
}