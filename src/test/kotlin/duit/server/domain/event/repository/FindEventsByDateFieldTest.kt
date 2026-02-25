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
import duit.server.application.config.QueryDslConfig
import org.springframework.context.annotation.Import
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@Import(QueryDslConfig::class)
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("findEventsByDateField 통합 테스트")
class FindEventsByDateFieldTest {

    @Autowired
    private lateinit var eventRepository: EventRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var host: Host

    private val tomorrow = LocalDateTime.of(2026, 6, 16, 0, 0)
    private val nextDay = LocalDateTime.of(2026, 6, 17, 0, 0)

    @BeforeEach
    fun setUp() {
        host = TestFixtures.host()
        entityManager.persist(host)
        entityManager.flush()
    }

    private fun persistEvent(
        status: EventStatus = EventStatus.RECRUITING,
        startAt: LocalDateTime = tomorrow.plusDays(10),
        endAt: LocalDateTime? = tomorrow.plusDays(11),
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
    @DisplayName("START_AT 필드")
    inner class StartAtTests {

        @Test
        @DisplayName("startAt이 내일 범위 내인 이벤트를 조회한다")
        fun withinRange() {
            val event = persistEvent(startAt = tomorrow.plusHours(10))
            val result = eventRepository.findEventsByDateField("START_AT", tomorrow, nextDay)
            assertEquals(listOf(event.id), result.map { it.id })
        }

        @Test
        @DisplayName("startAt이 내일 범위 밖이면 조회하지 않는다")
        fun outsideRange() {
            persistEvent(startAt = nextDay.plusHours(1))
            val result = eventRepository.findEventsByDateField("START_AT", tomorrow, nextDay)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("startAt이 정확히 tomorrow이면 조회한다 (>= tomorrow)")
        fun exactlyTomorrow() {
            val event = persistEvent(startAt = tomorrow)
            val result = eventRepository.findEventsByDateField("START_AT", tomorrow, nextDay)
            assertEquals(listOf(event.id), result.map { it.id })
        }

        @Test
        @DisplayName("startAt이 정확히 nextDay이면 조회하지 않는다 (< nextDay)")
        fun exactlyNextDay() {
            persistEvent(startAt = nextDay)
            val result = eventRepository.findEventsByDateField("START_AT", tomorrow, nextDay)
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("RECRUITMENT_START_AT 필드")
    inner class RecruitmentStartAtTests {

        @Test
        @DisplayName("recruitmentStartAt이 내일 범위 내인 이벤트를 조회한다")
        fun withinRange() {
            val event = persistEvent(recruitmentStartAt = tomorrow.plusHours(9))
            val result = eventRepository.findEventsByDateField("RECRUITMENT_START_AT", tomorrow, nextDay)
            assertEquals(listOf(event.id), result.map { it.id })
        }

        @Test
        @DisplayName("recruitmentStartAt이 범위 밖이면 조회하지 않는다")
        fun outsideRange() {
            persistEvent(recruitmentStartAt = tomorrow.minusDays(1))
            val result = eventRepository.findEventsByDateField("RECRUITMENT_START_AT", tomorrow, nextDay)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("recruitmentStartAt이 null이면 조회하지 않는다")
        fun nullField() {
            persistEvent(recruitmentStartAt = null)
            val result = eventRepository.findEventsByDateField("RECRUITMENT_START_AT", tomorrow, nextDay)
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("RECRUITMENT_END_AT 필드")
    inner class RecruitmentEndAtTests {

        @Test
        @DisplayName("recruitmentEndAt이 내일 범위 내인 이벤트를 조회한다")
        fun withinRange() {
            val event = persistEvent(
                recruitmentStartAt = tomorrow.minusDays(5),
                recruitmentEndAt = tomorrow.plusHours(18)
            )
            val result = eventRepository.findEventsByDateField("RECRUITMENT_END_AT", tomorrow, nextDay)
            assertEquals(listOf(event.id), result.map { it.id })
        }

        @Test
        @DisplayName("recruitmentEndAt이 범위 밖이면 조회하지 않는다")
        fun outsideRange() {
            persistEvent(
                recruitmentStartAt = tomorrow.minusDays(5),
                recruitmentEndAt = nextDay.plusHours(1)
            )
            val result = eventRepository.findEventsByDateField("RECRUITMENT_END_AT", tomorrow, nextDay)
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("공통 필터")
    inner class CommonFilterTests {

        @Test
        @DisplayName("PENDING 이벤트는 조회하지 않는다")
        fun pendingExcluded() {
            persistEvent(
                status = EventStatus.PENDING,
                startAt = tomorrow.plusHours(10)
            )
            val result = eventRepository.findEventsByDateField("START_AT", tomorrow, nextDay)
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("다른 fieldName의 날짜는 무시한다")
        fun fieldNameMismatch() {
            persistEvent(
                startAt = tomorrow.plusDays(10),
                recruitmentStartAt = tomorrow.plusHours(9)
            )
            val result = eventRepository.findEventsByDateField("START_AT", tomorrow, nextDay)
            assertTrue(result.isEmpty(), "START_AT 조회인데 startAt이 범위 밖이면 recruitmentStartAt이 범위 내여도 조회 안 됨")
        }
    }
}
