package duit.server.infrastructure.external.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FCMService {
    private val logger = LoggerFactory.getLogger(FCMService::class.java)

    /**
     * 단일 디바이스에 푸시 알림 전송
     */
    fun sendNotification(
        deviceToken: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean {
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
        return true
    }

    /**
     * 여러 디바이스에 푸시 알림 전송
     */
    fun sendNotificationToMultipleDevices(
        deviceTokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        if (deviceTokens.isEmpty()) {
            return
        }

        deviceTokens.forEach { token ->
            sendNotification(token, title, body, data)
        }
    }

    /**
     * 북마크한 행사 시작 알림
     */
    fun sendEventStartNotification(
        deviceToken: String,
        eventTitle: String,
        hostName: String,
        eventId: Long
    ): Boolean {
        val title = "북마크한 행사가 시작됩니다"
        val body = "[$hostName] $eventTitle"
        val data = mapOf(
            "type" to "event_start",
            "eventId" to eventId.toString(),
            "hostName" to hostName
        )

        return sendNotification(deviceToken, title, body, data)
    }

    /**
     * 행사 모집 시작 알림
     */
    fun sendRecruitmentStartNotification(
        deviceTokens: List<String>,
        eventTitle: String,
        hostName: String,
        eventId: Long
    ) {
        val title = "관심 행사 모집이 시작되었습니다"
        val body = "[$hostName] $eventTitle"
        val data = mapOf(
            "type" to "recruitment_start",
            "eventId" to eventId.toString(),
            "hostName" to hostName
        )

        sendNotificationToMultipleDevices(deviceTokens, title, body, data)
    }
}