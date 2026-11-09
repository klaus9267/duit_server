package duit.server.infrastructure.external.firebase

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import duit.server.domain.user.service.UserDeviceTokenService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FCMService(
    private val firebaseMessaging: FirebaseMessaging,
    private val userDeviceTokenService: UserDeviceTokenService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendAlarms(
        deviceTokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        val invalidTokens = deviceTokens.distinct()
            .chunked(MAX_MESSAGES_PER_REQUEST)
            .flatMap { tokens -> send(tokens, title, body, data) }

        deleteInvalidTokens(invalidTokens)
    }

    /** 다시 보낼 수 없는 토큰을 돌려준다 */
    private fun send(tokens: List<String>, title: String, body: String, data: Map<String, String>): List<String> {
        val messages = tokens.map { token -> buildMessage(token, title, body, data) }
        val response = try {
            firebaseMessaging.sendEach(messages)
        } catch (e: Exception) {
            log.error("FCM 발송 중 예외 발생 - tokens: {}개, title: {}", tokens.size, title, e)
            return emptyList()
        }
        if (response.failureCount == 0) return emptyList()

        val invalidTokens = response.responses.withIndex()
            .filterNot { (_, result) -> result.isSuccessful }
            .mapNotNull { (index, result) ->
                val errorCode = result.exception?.messagingErrorCode
                log.warn(
                    "FCM 발송 실패 - token: {}, code: {}, error: {}",
                    tokens[index].take(20) + "...",
                    errorCode,
                    result.exception?.message
                )
                tokens[index].takeIf { errorCode in INVALID_TOKEN_ERRORS }
            }
        log.warn(
            "FCM 발송 결과 - 총: {}, 성공: {}, 실패: {}",
            response.responses.size,
            response.successCount,
            response.failureCount
        )
        return invalidTokens
    }

    private fun deleteInvalidTokens(tokens: List<String>) {
        if (tokens.isEmpty()) return
        try {
            val deleted = userDeviceTokenService.deleteTokens(tokens)
            log.info("FCM 무효 토큰 삭제 - 대상: {}개, 삭제: {}개", tokens.size, deleted)
        } catch (e: Exception) {
            log.warn("FCM 무효 토큰 삭제 실패 - 대상: {}개", tokens.size, e)
        }
    }

    private fun buildMessage(token: String, title: String, body: String, data: Map<String, String>): Message =
        Message.builder()
            .setToken(token)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .putAllData(data)
            .setApnsConfig(APNS_CONFIG)
            .setAndroidConfig(ANDROID_CONFIG)
            .build()

    companion object {
        // FirebaseMessaging.sendEach 가 한 번에 받는 최대 메시지 수
        const val MAX_MESSAGES_PER_REQUEST = 500

        // 앱 삭제 등으로 다시 쓸 수 없는 토큰. 일시 오류(UNAVAILABLE, INTERNAL 등)는 지우지 않는다
        private val INVALID_TOKEN_ERRORS = setOf(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.SENDER_ID_MISMATCH)

        private val APNS_CONFIG = ApnsConfig.builder()
            .putHeader("apns-push-type", "alert")
            .putHeader("apns-priority", "10")
            .setAps(
                Aps.builder()
                    .setContentAvailable(true)
                    .setSound("default")
                    .build()
            )
            .build()

        private val ANDROID_CONFIG = AndroidConfig.builder()
            .setPriority(AndroidConfig.Priority.HIGH)
            .build()
    }
}
