package duit.server.domain.event.entity

import duit.server.domain.host.entity.Host
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("Event 엔티티 단위 테스트")
class EventUnitTest {

    private lateinit var host: Host
    private val baseTime = LocalDateTime.of(2026, 6, 15, 12, 0)

    @BeforeEach
    fun setUp() {
        host = Host(id = 1L, name = "테스트 주최")
    }

    private fun createEvent(
        startAt: LocalDateTime = baseTime.plusDays(7),
        endAt: LocalDateTime? = baseTime.plusDays(8),
        recruitmentStartAt: LocalDateTime? = null,
        recruitmentEndAt: LocalDateTime? = null,
    ) = Event(
        id = 1L,
        title = "테스트 행사",
        startAt = startAt,
        endAt = endAt,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
        uri = "https://example.com",
        thumbnail = null,
        eventType = EventType.CONFERENCE,
        host = host
    )

    @Nested
    @DisplayName("updateStatus - 상태 전이 로직")
    inner class UpdateStatusTests {

        @Test
        @DisplayName("행사 종료 후 → FINISHED")
        fun finishedAfterEndAt() {
            val event = createEvent(
                startAt = baseTime.minusDays(2),
                endAt = baseTime.minusDays(1)
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.FINISHED, event.status)
            assertEquals(EventStatusGroup.FINISHED, event.statusGroup)
        }

        @Test
        @DisplayName("endAt이 null이고 startAt 지남 → FINISHED")
        fun finishedWhenEndAtNull() {
            val event = createEvent(
                startAt = baseTime.minusDays(1),
                endAt = null
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.FINISHED, event.status)
            assertEquals(EventStatusGroup.FINISHED, event.statusGroup)
        }

        @Test
        @DisplayName("startAt ~ endAt 사이 → ACTIVE")
        fun activeBetweenStartAndEnd() {
            val event = createEvent(
                startAt = baseTime.minusHours(1),
                endAt = baseTime.plusHours(5)
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.ACTIVE, event.status)
            assertEquals(EventStatusGroup.ACTIVE, event.statusGroup)
        }

        @Test
        @DisplayName("endAt이 null이고 정확히 startAt → ACTIVE")
        fun activeExactlyAtStartWhenEndAtNull() {
            val event = createEvent(
                startAt = baseTime,
                endAt = null
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.ACTIVE, event.status)
            assertEquals(EventStatusGroup.ACTIVE, event.statusGroup)
        }

        @Test
        @DisplayName("모집 없음 (both null), 행사 전 → EVENT_WAITING")
        fun eventWaitingNoRecruitment() {
            val event = createEvent(
                startAt = baseTime.plusDays(3),
                endAt = baseTime.plusDays(4),
                recruitmentStartAt = null,
                recruitmentEndAt = null
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.EVENT_WAITING, event.status)
            assertEquals(EventStatusGroup.ACTIVE, event.statusGroup)
        }

        @Test
        @DisplayName("모집 종료 후, 행사 전 → EVENT_WAITING")
        fun eventWaitingAfterRecruitmentEnd() {
            val event = createEvent(
                startAt = baseTime.plusDays(5),
                endAt = baseTime.plusDays(6),
                recruitmentStartAt = baseTime.minusDays(3),
                recruitmentEndAt = baseTime.minusDays(1)
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.EVENT_WAITING, event.status)
            assertEquals(EventStatusGroup.ACTIVE, event.statusGroup)
        }

        @Test
        @DisplayName("모집 시작/종료 모두 있고, 모집 기간 내 → RECRUITING")
        fun recruitingWithBothDates() {
            val event = createEvent(
                startAt = baseTime.plusDays(10),
                endAt = baseTime.plusDays(11),
                recruitmentStartAt = baseTime.minusDays(1),
                recruitmentEndAt = baseTime.plusDays(3)
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.RECRUITING, event.status)
            assertEquals(EventStatusGroup.ACTIVE, event.statusGroup)
        }

        @Test
        @DisplayName("모집 종료만 있고, 종료 전 → RECRUITING")
        fun recruitingOnlyEndDate() {
            val event = createEvent(
                startAt = baseTime.plusDays(10),
                endAt = baseTime.plusDays(11),
                recruitmentStartAt = null,
                recruitmentEndAt = baseTime.plusDays(3)
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.RECRUITING, event.status)
            assertEquals(EventStatusGroup.ACTIVE, event.statusGroup)
        }

        @Test
        @DisplayName("모집 시작만 있고, 시작 후 행사 전 → RECRUITING")
        fun recruitingOnlyStartDate() {
            val event = createEvent(
                startAt = baseTime.plusDays(10),
                endAt = baseTime.plusDays(11),
                recruitmentStartAt = baseTime.minusDays(1),
                recruitmentEndAt = null
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.RECRUITING, event.status)
            assertEquals(EventStatusGroup.ACTIVE, event.statusGroup)
        }

        @Test
        @DisplayName("모집 시작 전 → RECRUITMENT_WAITING")
        fun recruitmentWaiting() {
            val event = createEvent(
                startAt = baseTime.plusDays(10),
                endAt = baseTime.plusDays(11),
                recruitmentStartAt = baseTime.plusDays(1),
                recruitmentEndAt = baseTime.plusDays(5)
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.RECRUITMENT_WAITING, event.status)
            assertEquals(EventStatusGroup.ACTIVE, event.statusGroup)
        }

        @Test
        @DisplayName("정확히 모집 시작 시각 → RECRUITING (time in recruitmentStartAt..recruitmentEndAt)")
        fun recruitingExactlyAtStart() {
            val event = createEvent(
                startAt = baseTime.plusDays(10),
                endAt = baseTime.plusDays(11),
                recruitmentStartAt = baseTime,
                recruitmentEndAt = baseTime.plusDays(5)
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.RECRUITING, event.status)
        }

        @Test
        @DisplayName("정확히 endAt 시각 → ACTIVE (time in startAt..endAt)")
        fun activeExactlyAtEndAt() {
            val event = createEvent(
                startAt = baseTime.minusHours(2),
                endAt = baseTime
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.ACTIVE, event.status)
        }

        @Test
        @DisplayName("정확히 recruitmentEndAt 시각 → EVENT_WAITING (recruitmentEndAt <= time)")
        fun eventWaitingExactlyAtRecruitmentEnd() {
            val event = createEvent(
                startAt = baseTime.plusDays(5),
                endAt = baseTime.plusDays(6),
                recruitmentStartAt = baseTime.minusDays(3),
                recruitmentEndAt = baseTime
            )
            event.updateStatus(baseTime)

            assertEquals(EventStatus.EVENT_WAITING, event.status)
        }
    }
}
