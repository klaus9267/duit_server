package duit.server.domain.event.repository

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.host.entity.Host
import duit.server.support.fixture.TestFixtures
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("findEventsWithIncorrectStatus 통합 테스트")
class FindEventsWithIncorrectStatusTest {

    @Autowired
    private lateinit var eventRepository: EventRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var host: Host
    private val now = LocalDateTime.of(2026, 6, 15, 12, 0)

    @BeforeEach
    fun setUp() {
        host = TestFixtures.host()
        entityManager.persist(host)
        entityManager.flush()
    }

    private fun persistEvent(
        status: EventStatus,
        startAt: LocalDateTime,
        endAt: LocalDateTime? = null,
        recruitmentStartAt: LocalDateTime? = null,
        recruitmentEndAt: LocalDateTime? = null,
    ): Event {
        val statusGroup = when (status) {
            EventStatus.PENDING -> EventStatusGroup.PENDING
            EventStatus.FINISHED -> EventStatusGroup.FINISHED
            else -> EventStatusGroup.ACTIVE
        }
        val event = TestFixtures.event(
            host = host,
            status = status,
            statusGroup = statusGroup,
            startAt = startAt,
            endAt = endAt,
            recruitmentStartAt = recruitmentStartAt,
            recruitmentEndAt = recruitmentEndAt,
        )
        entityManager.persist(event)
        entityManager.flush()
        return event
    }

    @Nested
    @DisplayName("올바른 상태인 이벤트는 조회하지 않는다")
    inner class CorrectStatusTests {

        @Test
        @DisplayName("FINISHED - endAt < now")
        fun finishedWithEndAt() {
            persistEvent(
                status = EventStatus.FINISHED,
                startAt = now.minusDays(3),
                endAt = now.minusDays(1)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("FINISHED - endAt null, startAt < now")
        fun finishedWithoutEndAt() {
            persistEvent(
                status = EventStatus.FINISHED,
                startAt = now.minusDays(1),
                endAt = null
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("ACTIVE - startAt <= now <= endAt")
        fun active() {
            persistEvent(
                status = EventStatus.ACTIVE,
                startAt = now.minusHours(1),
                endAt = now.plusHours(5)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("EVENT_WAITING - 모집 정보 없음, 행사 시작 전")
        fun eventWaitingNoRecruitment() {
            persistEvent(
                status = EventStatus.EVENT_WAITING,
                startAt = now.plusDays(3),
                endAt = now.plusDays(4)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("EVENT_WAITING - 모집 종료 후, 행사 시작 전")
        fun eventWaitingAfterRecruitmentEnd() {
            persistEvent(
                status = EventStatus.EVENT_WAITING,
                startAt = now.plusDays(5),
                endAt = now.plusDays(6),
                recruitmentStartAt = now.minusDays(3),
                recruitmentEndAt = now.minusDays(1)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("RECRUITING - Case 1: 시작/종료일 모두 있고 모집 기간 내")
        fun recruitingCase1() {
            persistEvent(
                status = EventStatus.RECRUITING,
                startAt = now.plusDays(10),
                endAt = now.plusDays(11),
                recruitmentStartAt = now.minusDays(1),
                recruitmentEndAt = now.plusDays(3)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("RECRUITING - Case 2: 종료일만 있고 종료 전")
        fun recruitingCase2() {
            persistEvent(
                status = EventStatus.RECRUITING,
                startAt = now.plusDays(10),
                endAt = now.plusDays(11),
                recruitmentStartAt = null,
                recruitmentEndAt = now.plusDays(3)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("RECRUITING - Case 3: 시작일만 있고 시작 후 행사 전")
        fun recruitingCase3() {
            persistEvent(
                status = EventStatus.RECRUITING,
                startAt = now.plusDays(10),
                endAt = now.plusDays(11),
                recruitmentStartAt = now.minusDays(1),
                recruitmentEndAt = null
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("RECRUITMENT_WAITING - 모집 시작 전")
        fun recruitmentWaiting() {
            persistEvent(
                status = EventStatus.RECRUITMENT_WAITING,
                startAt = now.plusDays(10),
                endAt = now.plusDays(11),
                recruitmentStartAt = now.plusDays(1),
                recruitmentEndAt = now.plusDays(5)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("PENDING 이벤트는 항상 제외")
        fun pendingAlwaysExcluded() {
            persistEvent(
                status = EventStatus.PENDING,
                startAt = now.minusDays(1),
                endAt = null
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("잘못된 상태인 이벤트를 감지한다")
    inner class IncorrectStatusTests {

        @Test
        @DisplayName("ACTIVE인데 종료되어야 함 (endAt < now)")
        fun shouldBeFinishedWithEndAt() {
            val event = persistEvent(
                status = EventStatus.ACTIVE,
                startAt = now.minusDays(3),
                endAt = now.minusDays(1)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertEquals(listOf(event.id), result.map { it.id })
        }

        @Test
        @DisplayName("RECRUITING인데 종료되어야 함 (endAt null, startAt < now)")
        fun shouldBeFinishedWithoutEndAt() {
            val event = persistEvent(
                status = EventStatus.RECRUITING,
                startAt = now.minusDays(1),
                endAt = null,
                recruitmentStartAt = now.minusDays(5),
                recruitmentEndAt = now.minusDays(2)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertEquals(listOf(event.id), result.map { it.id })
        }

        @Test
        @DisplayName("EVENT_WAITING인데 진행 중이어야 함 (startAt <= now <= endAt)")
        fun shouldBeActive() {
            val event = persistEvent(
                status = EventStatus.EVENT_WAITING,
                startAt = now.minusHours(1),
                endAt = now.plusHours(5)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertEquals(listOf(event.id), result.map { it.id })
        }

        @Test
        @DisplayName("RECRUITING인데 모집 정보 없어 EVENT_WAITING이어야 함")
        fun shouldBeEventWaitingNoRecruitment() {
            val event = persistEvent(
                status = EventStatus.RECRUITING,
                startAt = now.plusDays(3),
                endAt = now.plusDays(4)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertEquals(listOf(event.id), result.map { it.id })
        }

        @Test
        @DisplayName("RECRUITING인데 모집 종료되어 EVENT_WAITING이어야 함")
        fun shouldBeEventWaitingRecruitmentEnded() {
            val event = persistEvent(
                status = EventStatus.RECRUITING,
                startAt = now.plusDays(5),
                endAt = now.plusDays(6),
                recruitmentStartAt = now.minusDays(5),
                recruitmentEndAt = now.minusDays(1)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertEquals(listOf(event.id), result.map { it.id })
        }

        @Test
        @DisplayName("RECRUITMENT_WAITING인데 모집 중이어야 함 - Case 1: 시작/종료일 모두 존재")
        fun shouldBeRecruitingCase1() {
            val event = persistEvent(
                status = EventStatus.RECRUITMENT_WAITING,
                startAt = now.plusDays(10),
                endAt = now.plusDays(11),
                recruitmentStartAt = now.minusDays(1),
                recruitmentEndAt = now.plusDays(3)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertEquals(listOf(event.id), result.map { it.id })
        }

        @Test
        @DisplayName("RECRUITMENT_WAITING인데 모집 중이어야 함 - Case 2: 종료일만 존재")
        fun shouldBeRecruitingCase2() {
            val event = persistEvent(
                status = EventStatus.RECRUITMENT_WAITING,
                startAt = now.plusDays(10),
                endAt = now.plusDays(11),
                recruitmentStartAt = null,
                recruitmentEndAt = now.plusDays(3)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertEquals(listOf(event.id), result.map { it.id })
        }

        @Test
        @DisplayName("EVENT_WAITING인데 모집 중이어야 함 - Case 3: 시작일만 존재")
        fun shouldBeRecruitingCase3() {
            val event = persistEvent(
                status = EventStatus.EVENT_WAITING,
                startAt = now.plusDays(10),
                endAt = now.plusDays(11),
                recruitmentStartAt = now.minusDays(1),
                recruitmentEndAt = null
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertEquals(listOf(event.id), result.map { it.id })
        }

        @Test
        @DisplayName("RECRUITING인데 모집 시작 전이어야 함")
        fun shouldBeRecruitmentWaiting() {
            val event = persistEvent(
                status = EventStatus.RECRUITING,
                startAt = now.plusDays(10),
                endAt = now.plusDays(11),
                recruitmentStartAt = now.plusDays(1),
                recruitmentEndAt = now.plusDays(5)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertEquals(listOf(event.id), result.map { it.id })
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    inner class BoundaryTests {

        @Test
        @DisplayName("endAt == now → ACTIVE (FINISHED 아님)")
        fun endAtEqualsNow() {
            persistEvent(
                status = EventStatus.ACTIVE,
                startAt = now.minusHours(2),
                endAt = now
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty(), "endAt == now인 ACTIVE는 올바른 상태")
        }

        @Test
        @DisplayName("recruitmentEndAt == now → EVENT_WAITING (RECRUITING 아님)")
        fun recruitmentEndAtEqualsNow() {
            persistEvent(
                status = EventStatus.EVENT_WAITING,
                startAt = now.plusDays(5),
                endAt = now.plusDays(6),
                recruitmentStartAt = now.minusDays(3),
                recruitmentEndAt = now
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty(), "recruitmentEndAt == now인 EVENT_WAITING은 올바른 상태")
        }

        @Test
        @DisplayName("recruitmentStartAt == now → RECRUITING (RECRUITMENT_WAITING 아님)")
        fun recruitmentStartAtEqualsNow() {
            persistEvent(
                status = EventStatus.RECRUITING,
                startAt = now.plusDays(10),
                endAt = now.plusDays(11),
                recruitmentStartAt = now,
                recruitmentEndAt = now.plusDays(5)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty(), "recruitmentStartAt == now인 RECRUITING은 올바른 상태")
        }

        @Test
        @DisplayName("startAt == now, endAt 있음 → ACTIVE (EVENT_WAITING 아님)")
        fun startAtEqualsNow() {
            persistEvent(
                status = EventStatus.ACTIVE,
                startAt = now,
                endAt = now.plusHours(5)
            )
            val result = eventRepository.findEventsWithIncorrectStatus(now)
            assertTrue(result.isEmpty(), "startAt == now인 ACTIVE는 올바른 상태")
        }
    }

    @Nested
    @DisplayName("복합 시나리오")
    inner class MixedScenarioTests {

        @Test
        @DisplayName("여러 이벤트 중 잘못된 상태인 것만 정확히 감지한다")
        fun detectsOnlyIncorrectAmongMultiple() {
            val correctFinished = persistEvent(
                status = EventStatus.FINISHED,
                startAt = now.minusDays(3),
                endAt = now.minusDays(1)
            )
            val correctRecruiting = persistEvent(
                status = EventStatus.RECRUITING,
                startAt = now.plusDays(10),
                endAt = now.plusDays(11),
                recruitmentStartAt = now.minusDays(1),
                recruitmentEndAt = now.plusDays(3)
            )
            val wrongShouldBeFinished = persistEvent(
                status = EventStatus.ACTIVE,
                startAt = now.minusDays(5),
                endAt = now.minusDays(2)
            )
            val wrongShouldBeRecruiting = persistEvent(
                status = EventStatus.RECRUITMENT_WAITING,
                startAt = now.plusDays(10),
                endAt = now.plusDays(11),
                recruitmentStartAt = now.minusDays(1),
                recruitmentEndAt = now.plusDays(3)
            )
            val pending = persistEvent(
                status = EventStatus.PENDING,
                startAt = now.minusDays(1),
                endAt = null
            )

            val result = eventRepository.findEventsWithIncorrectStatus(now)
            val resultIds = result.map { it.id }.toSet()

            assertEquals(2, result.size)
            assertTrue(resultIds.contains(wrongShouldBeFinished.id))
            assertTrue(resultIds.contains(wrongShouldBeRecruiting.id))
        }
    }
}
