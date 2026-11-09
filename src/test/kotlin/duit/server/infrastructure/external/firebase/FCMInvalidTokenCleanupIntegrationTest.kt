package duit.server.infrastructure.external.firebase

import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.SendResponse
import com.ninjasquad.springmockk.MockkBean
import duit.server.domain.user.entity.User
import duit.server.domain.user.repository.UserDeviceTokenRepository
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

/**
 * 구독 푸시는 커밋 뒤 푸시 스레드에서 트랜잭션 없이 보낸다 (D-023).
 * 풀이 auto-commit=false 라서 트랜잭션 밖 쓰기는 조용히 롤백되므로, 그 경로에서도 무효 토큰 삭제가 커밋되는지 본다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("FCM 무효 토큰 정리")
class FCMInvalidTokenCleanupIntegrationTest : IntegrationTestSupport() {

    @MockkBean
    private lateinit var firebaseMessaging: FirebaseMessaging

    @Autowired
    private lateinit var fcmService: FCMService

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var userDeviceTokenRepository: UserDeviceTokenRepository

    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        user = transactionTemplate.execute {
            TestFixtures.user(
                providerId = "fcm-cleanup",
                nickname = "토큰정리",
                deviceTokens = listOf("live-token", "dead-token"),
            ).also(entityManager::persist)
        }!!
    }

    @AfterEach
    fun tearDown() {
        transactionTemplate.executeWithoutResult { entityManager.remove(entityManager.find(User::class.java, user.id)) }
    }

    @Test
    fun `트랜잭션 밖에서 보내도 앱이 삭제된 기기의 토큰 삭제가 커밋된다`() {
        val unregistered = mockk<FirebaseMessagingException> {
            every { messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
            every { message } returns "Requested entity was not found."
        }
        val responses = listOf<SendResponse>(
            mockk { every { isSuccessful } returns true },
            mockk {
                every { isSuccessful } returns false
                every { exception } returns unregistered
            },
        )
        every { firebaseMessaging.sendEach(any<List<Message>>()) } returns mockk<BatchResponse> {
            every { this@mockk.responses } returns responses
            every { successCount } returns 1
            every { failureCount } returns 1
        }

        fcmService.sendAlarms(listOf("live-token", "dead-token"), "제목", "본문")

        assertEquals(listOf("live-token"), userDeviceTokenRepository.findTokensByUserId(user.id!!))
    }
}
