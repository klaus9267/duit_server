package duit.server.domain.subscription.service

import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.alarm.repository.AlarmRepository
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.host.entity.Host
import duit.server.domain.job.entity.JobPosting
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.user.entity.AlarmSettings
import duit.server.support.IntegrationTestSupport
import duit.server.support.fixture.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

@DisplayName("SubscriptionNotificationService 통합 테스트")
class SubscriptionNotificationServiceIntegrationTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var notificationService: SubscriptionNotificationService

    @Autowired
    private lateinit var alarmRepository: AlarmRepository

    private val alarmSettings = AlarmSettings(push = true, bookmark = true, calendar = false, marketing = false)

    @Nested
    @DisplayName("notifyOnEventApproved - 행사 승인 알림")
    inner class EventNotificationTests {

        @Test
        @DisplayName("EVENT_HOST 구독자에게 EVENT_SUBSCRIPTION_HOST 알람이 생성된다")
        fun `EVENT_HOST 매칭 알람 생성`() {
            val host = TestFixtures.host(name = "구독 주최")
            entityManager.persist(host)

            val subscriber = TestFixtures.user(
                nickname = "구독자",
                providerId = "sub-1",
                deviceToken = "tok-sub-1",
                alarmSettings = alarmSettings,
            )
            entityManager.persist(subscriber)
            entityManager.persist(Subscription(user = subscriber, type = SubscriptionType.EVENT_HOST, host = host))

            val event = persistApprovedEvent(host = host, title = "테스트 행사")

            entityManager.flush()
            entityManager.clear()

            notificationService.notifyOnEventApproved(
                entityManager.find(Event::class.java, event.id!!)
            )
            entityManager.flush()
            entityManager.clear()

            val alarms = alarmRepository.findAll()
                .filter { it.user.id == subscriber.id }
            assertEquals(1, alarms.size)
            assertEquals(AlarmType.EVENT_SUBSCRIPTION_HOST, alarms[0].type)
            assertEquals(event.id, alarms[0].event!!.id)
        }

        @Test
        @DisplayName("EVENT_KEYWORD 구독자 — 제목에 키워드 포함된 행사 매칭")
        fun `EVENT_KEYWORD 키워드 부분일치 매칭`() {
            val host = TestFixtures.host(name = "주최A")
            entityManager.persist(host)

            val matched = TestFixtures.user(nickname = "키워드유저", providerId = "kw-1", deviceToken = "tok-kw-1", alarmSettings = alarmSettings)
            val notMatched = TestFixtures.user(nickname = "다른키워드유저", providerId = "kw-2", deviceToken = "tok-kw-2", alarmSettings = alarmSettings)
            entityManager.persist(matched)
            entityManager.persist(notMatched)

            entityManager.persist(Subscription(user = matched, type = SubscriptionType.EVENT_KEYWORD, keyword = "AI"))
            entityManager.persist(Subscription(user = notMatched, type = SubscriptionType.EVENT_KEYWORD, keyword = "블록체인"))

            val event = persistApprovedEvent(host = host, title = "2026 AI 컨퍼런스")

            entityManager.flush()
            entityManager.clear()

            notificationService.notifyOnEventApproved(
                entityManager.find(Event::class.java, event.id!!)
            )
            entityManager.flush()
            entityManager.clear()

            val alarms = alarmRepository.findAll().filter { it.event?.id == event.id }
            assertEquals(1, alarms.size)
            assertEquals(matched.id, alarms[0].user.id)
            assertEquals(AlarmType.EVENT_SUBSCRIPTION_KEYWORD, alarms[0].type)
        }

        @Test
        @DisplayName("같은 행사로 두 번 호출해도 알람은 dedup 되어 1개")
        fun `중복 호출 dedup`() {
            val host = TestFixtures.host(name = "중복테스트주최")
            entityManager.persist(host)

            val user = TestFixtures.user(nickname = "dup-user", providerId = "dup-1", deviceToken = "tok-dup", alarmSettings = alarmSettings)
            entityManager.persist(user)
            entityManager.persist(Subscription(user = user, type = SubscriptionType.EVENT_HOST, host = host))

            val event = persistApprovedEvent(host = host, title = "중복 행사")

            entityManager.flush()
            entityManager.clear()

            val managedEvent = entityManager.find(Event::class.java, event.id!!)
            notificationService.notifyOnEventApproved(managedEvent)
            entityManager.flush()
            notificationService.notifyOnEventApproved(managedEvent)
            entityManager.flush()
            entityManager.clear()

            val alarms = alarmRepository.findAll().filter { it.user.id == user.id && it.event?.id == event.id }
            assertEquals(1, alarms.size)
        }
    }

    @Nested
    @DisplayName("notifyOnJobPostingCreated - 채용공고 알림")
    inner class JobNotificationTests {

        @Test
        @DisplayName("JOB_KEYWORD 구독자 — 제목 부분일치 매칭")
        fun `JOB_KEYWORD 매칭 알람 생성`() {
            val user = TestFixtures.user(nickname = "잡유저", providerId = "job-1", deviceToken = "tok-job-1", alarmSettings = alarmSettings)
            entityManager.persist(user)
            entityManager.persist(Subscription(user = user, type = SubscriptionType.JOB_KEYWORD, keyword = "간호사"))

            val jobPosting = TestFixtures.jobPosting(title = "신규 간호사 채용")
            entityManager.persist(jobPosting)

            entityManager.flush()
            entityManager.clear()

            notificationService.notifyOnJobPostingCreated(
                entityManager.find(JobPosting::class.java, jobPosting.id!!)
            )
            entityManager.flush()
            entityManager.clear()

            val alarms = alarmRepository.findAll().filter { it.jobPosting?.id == jobPosting.id }
            assertEquals(1, alarms.size)
            assertEquals(user.id, alarms[0].user.id)
            assertEquals(AlarmType.JOB_SUBSCRIPTION_KEYWORD, alarms[0].type)
            assertTrue(alarms[0].event == null)
        }
    }

    private fun persistApprovedEvent(host: Host, title: String): Event {
        val event = TestFixtures.event(
            host = host,
            title = title,
            startAt = LocalDateTime.now().plusDays(7),
            recruitmentStartAt = LocalDateTime.now().minusDays(1),
            recruitmentEndAt = LocalDateTime.now().plusDays(3),
            status = EventStatus.RECRUITING,
            statusGroup = EventStatusGroup.ACTIVE,
        )
        entityManager.persist(event)
        return event
    }
}
