package duit.server.domain.subscription.service

import com.ninjasquad.springmockk.MockkBean
import duit.server.domain.event.entity.Event
import duit.server.domain.event.service.EventService
import duit.server.domain.host.entity.Host
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.user.entity.AlarmSettings
import duit.server.domain.user.entity.User
import duit.server.infrastructure.external.firebase.FCMService
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

/**
 * C-1: 행사 승인 시 구독 푸시(FCM)는 승인 트랜잭션이 커밋된 뒤 별도 스레드에서 보낸다 (performance/decisions.md D-023).
 * 운영 승인 API p95 1,628ms 는 트랜잭션 안에서 구독자마다 FCM 을 동기로 호출한 탓이다(prod-profile §2).
 * 여기서는 FCM 지연을 [FCM_DELAY_MS] 로 고정한 통제 실험이다. 커밋 시점을 관찰해야 하므로 테스트 트랜잭션 없이 돌린다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("승인 푸시는 커밋 뒤 비동기 (C-1)")
class ApprovalPushAfterCommitIntegrationTest : IntegrationTestSupport() {

    @MockkBean
    private lateinit var fcmService: FCMService

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var notificationService: SubscriptionNotificationService

    @Autowired
    private lateinit var eventService: EventService

    private lateinit var host: Host
    private lateinit var event: Event
    private lateinit var subscribers: List<User>

    @BeforeEach
    fun setUp() {
        every { fcmService.sendAlarms(any(), any(), any(), any()) } answers { Thread.sleep(FCM_DELAY_MS) }
        transactionTemplate.executeWithoutResult {
            host = TestFixtures.host(name = "푸시 테스트 주최").also(entityManager::persist)
            event = TestFixtures.event(title = "푸시 테스트 행사", host = host).also(entityManager::persist)
            subscribers = (1..SUBSCRIBERS).map { i ->
                TestFixtures.user(
                    providerId = "push-subscriber-$i",
                    nickname = "푸시구독자$i",
                    deviceToken = "push-token-$i",
                    alarmSettings = AlarmSettings(push = true, bookmark = true, calendar = false, marketing = false),
                ).also(entityManager::persist)
            }
            subscribers.forEach { entityManager.persist(Subscription(user = it, type = SubscriptionType.EVENT_HOST, host = host)) }
        }
    }

    @AfterEach
    fun tearDown() {
        transactionTemplate.executeWithoutResult {
            val userIds = subscribers.map { it.id }
            entityManager.createQuery("DELETE FROM Subscription s WHERE s.user.id IN :ids").setParameter("ids", userIds).executeUpdate()
            entityManager.createQuery("DELETE FROM Alarm a WHERE a.event.id = :id").setParameter("id", event.id).executeUpdate()
            userIds.forEach { id -> entityManager.remove(entityManager.find(User::class.java, id)) }
            entityManager.createQuery("DELETE FROM View v WHERE v.event.id = :id").setParameter("id", event.id).executeUpdate()
            entityManager.createQuery("DELETE FROM Event e WHERE e.id = :id").setParameter("id", event.id).executeUpdate()
            entityManager.createQuery("DELETE FROM Host h WHERE h.id = :id").setParameter("id", host.id).executeUpdate()
        }
    }

    @Test
    fun `승인은 구독자 푸시 발송을 기다리지 않는다`() {
        val startedAt = System.nanoTime()
        eventService.updateStatus(event.id!!) // PATCH /api/v1/events/{id}/approve 의 본체 (관리자 인가는 이 실험과 무관)
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        println("[C-1] 승인 소요 ${elapsedMs}ms (구독자 $SUBSCRIBERS, FCM 지연 ${FCM_DELAY_MS}ms 고정)")

        // 동기로 보내면 구독자 수 × FCM 지연(5 × 300ms = 1.5초)을 기다린다
        assertTrue(elapsedMs < FCM_DELAY_MS, "승인에 ${elapsedMs}ms 걸림")
        verify(timeout = 5_000, exactly = SUBSCRIBERS) { fcmService.sendAlarms(any(), any(), any(), any()) }
        assertEquals(SUBSCRIBERS.toLong(), alarmCount(), "알람 행은 승인 트랜잭션과 함께 커밋된다")
    }

    @Test
    fun `승인 트랜잭션이 롤백되면 푸시를 보내지 않는다`() {
        transactionTemplate.executeWithoutResult { tx ->
            notificationService.notifyOnEventApproved(entityManager.find(Event::class.java, event.id))
            tx.setRollbackOnly()
        }

        Thread.sleep(ROLLBACK_SETTLE_MS) // 비동기로 새어 나가는 발송이 있다면 이 사이에 일어난다
        verify(exactly = 0) { fcmService.sendAlarms(any(), any(), any(), any()) }
        assertEquals(0L, alarmCount())
    }

    private fun alarmCount(): Long = transactionTemplate.execute {
        entityManager.createQuery("SELECT COUNT(a) FROM Alarm a WHERE a.event.id = :id", Long::class.javaObjectType)
            .setParameter("id", event.id).singleResult
    }!!

    companion object {
        private const val SUBSCRIBERS = 5
        private const val FCM_DELAY_MS = 300L
        private const val ROLLBACK_SETTLE_MS = 500L
    }
}
