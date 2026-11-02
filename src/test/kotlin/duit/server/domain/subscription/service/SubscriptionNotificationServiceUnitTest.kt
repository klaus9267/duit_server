package duit.server.domain.subscription.service

import duit.server.domain.alarm.repository.AlarmRepository
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.entity.Host
import duit.server.domain.subscription.entity.Subscription
import duit.server.domain.subscription.entity.SubscriptionType
import duit.server.domain.subscription.service.matcher.EventSubscriptionMatcher
import duit.server.domain.user.entity.User
import duit.server.infrastructure.external.firebase.FCMService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("SubscriptionNotificationService 단위 테스트")
class SubscriptionNotificationServiceUnitTest {

    @Test
    @DisplayName("EVENT_HOST 알림 본문에는 행사의 첫 번째 주최가 아니라 사용자가 구독한 주최 이름이 들어간다")
    fun hostAlarmBodyUsesSubscribedHostName() {
        val firstHost = Host(id = 1L, name = "첫번째 주최")
        val secondHost = Host(id = 2L, name = "두번째 주최")
        val event = Event(
            id = 100L,
            title = "공동 주최 행사",
            startAt = LocalDateTime.now().plusDays(7),
            endAt = null,
            recruitmentStartAt = null,
            recruitmentEndAt = null,
            uri = "https://example.com",
            thumbnail = null,
            eventType = EventType.CONFERENCE,
            hosts = listOf(firstHost, secondHost),
        )
        val user = User(id = 10L, nickname = "구독자").apply { registerDeviceToken("tok-1") }
        val subscription = Subscription(id = 1000L, user = user, type = SubscriptionType.EVENT_HOST, host = secondHost)

        val matcher = mockk<EventSubscriptionMatcher>()
        val alarmRepository = mockk<AlarmRepository>()
        val fcmService = mockk<FCMService>()
        val body = slot<String>()
        every { matcher.findMatchedSubscriptions(event) } returns listOf(subscription)
        every { alarmRepository.existsByUserIdAndEventIdAndType(any(), any(), any()) } returns false
        every { alarmRepository.save(any()) } answers { firstArg() }
        every { fcmService.sendAlarms(any(), any(), capture(body), any()) } returns Unit

        SubscriptionNotificationService(matcher, mockk(), alarmRepository, fcmService).notifyOnEventApproved(event)

        assertTrue(body.captured.startsWith("두번째 주최에서"), "body: ${body.captured}")
    }
}
