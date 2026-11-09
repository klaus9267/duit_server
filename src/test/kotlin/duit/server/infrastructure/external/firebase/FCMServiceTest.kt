package duit.server.infrastructure.external.firebase

import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.SendResponse
import duit.server.domain.user.service.UserDeviceTokenService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

@DisplayName("FCMService 단위 테스트")
class FCMServiceTest {

    private lateinit var firebaseMessaging: FirebaseMessaging
    private lateinit var userDeviceTokenService: UserDeviceTokenService
    private lateinit var fcmService: FCMService

    @BeforeEach
    fun setUp() {
        firebaseMessaging = mockk()
        userDeviceTokenService = mockk()
        every { userDeviceTokenService.deleteTokens(any()) } answers { firstArg<Collection<String>>().size }
        fcmService = FCMService(firebaseMessaging, userDeviceTokenService)
    }

    @Test
    @DisplayName("다시 쓸 수 없는 토큰(UNREGISTERED, SENDER_ID_MISMATCH)만 지우고 일시 오류 토큰은 남긴다")
    fun deletesOnlyInvalidTokens() {
        every { firebaseMessaging.sendEach(any<List<Message>>()) } returns batchResponse(
            success(),
            failure(MessagingErrorCode.UNREGISTERED),
            failure(MessagingErrorCode.UNAVAILABLE),
            failure(MessagingErrorCode.SENDER_ID_MISMATCH),
        )

        fcmService.sendAlarms(listOf("ok", "uninstalled", "busy", "other-project"), "제목", "본문")

        verify(exactly = 1) { userDeviceTokenService.deleteTokens(listOf("uninstalled", "other-project")) }
    }

    @Test
    @DisplayName("모두 성공하면 토큰을 지우지 않는다")
    fun keepsTokensWhenAllSucceed() {
        every { firebaseMessaging.sendEach(any<List<Message>>()) } returns batchResponse(success(), success())

        fcmService.sendAlarms(listOf("a", "b"), "제목", "본문")

        verify(exactly = 0) { userDeviceTokenService.deleteTokens(any()) }
    }

    @Test
    @DisplayName("500개씩 나눠 보내고 중복 토큰은 한 번만 보낸다")
    fun sendsInBatchesOf500() {
        val batchSizes = mutableListOf<Int>()
        val messages = slot<List<Message>>()
        every { firebaseMessaging.sendEach(capture(messages)) } answers {
            batchSizes += messages.captured.size
            batchResponse(*Array(messages.captured.size) { success() })
        }
        val tokens = (1..1001).map { "token-$it" }

        fcmService.sendAlarms(tokens + tokens.take(10), "제목", "본문")

        assertEquals(listOf(500, 500, 1), batchSizes)
    }

    @Test
    @DisplayName("발송 자체가 실패해도 예외를 던지지 않고 토큰도 지우지 않는다")
    fun swallowsSendException() {
        every { firebaseMessaging.sendEach(any<List<Message>>()) } throws IllegalStateException("network")

        assertDoesNotThrow { fcmService.sendAlarms(listOf("a"), "제목", "본문") }
        verify(exactly = 0) { userDeviceTokenService.deleteTokens(any()) }
    }

    @Test
    @DisplayName("토큰 삭제가 실패해도 예외를 던지지 않는다")
    fun swallowsDeleteException() {
        every { firebaseMessaging.sendEach(any<List<Message>>()) } returns
            batchResponse(failure(MessagingErrorCode.UNREGISTERED))
        every { userDeviceTokenService.deleteTokens(any()) } throws IllegalStateException("db")

        assertDoesNotThrow { fcmService.sendAlarms(listOf("a"), "제목", "본문") }
    }

    private fun success(): SendResponse = mockk { every { isSuccessful } returns true }

    private fun failure(errorCode: MessagingErrorCode): SendResponse {
        val exception = mockk<FirebaseMessagingException> {
            every { messagingErrorCode } returns errorCode
            every { message } returns errorCode.name
        }
        return mockk {
            every { isSuccessful } returns false
            every { this@mockk.exception } returns exception
        }
    }

    private fun batchResponse(vararg responses: SendResponse): BatchResponse = mockk {
        every { this@mockk.responses } returns responses.toList()
        every { successCount } returns responses.count { it.isSuccessful }
        every { failureCount } returns responses.count { !it.isSuccessful }
    }
}
