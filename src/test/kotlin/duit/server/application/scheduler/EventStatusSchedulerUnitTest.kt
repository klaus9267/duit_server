package duit.server.application.scheduler

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.event.service.EventService
import duit.server.domain.event.service.EventCacheService
import duit.server.domain.host.entity.Host
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.scheduling.TaskScheduler
import java.time.LocalDateTime

@DisplayName("EventStatusScheduler 단위 테스트")
class EventStatusSchedulerUnitTest {

    private lateinit var eventRepository: EventRepository
    private lateinit var eventService: EventService
    private lateinit var taskScheduler: TaskScheduler
    private lateinit var scheduler: EventStatusScheduler
    private lateinit var eventCacheService: EventCacheService

    private val host = Host(id = 1L, name = "테스트")

    @BeforeEach
    fun setUp() {
        eventCacheService = mockk(relaxed = true)
        eventRepository = mockk()
        eventService = mockk(relaxed = true)
        taskScheduler = mockk(relaxed = true)
        scheduler = EventStatusScheduler(eventRepository, eventService, taskScheduler, eventCacheService)
    }

    private fun createEvent(
        id: Long,
        status: EventStatus,
        startAt: LocalDateTime,
        endAt: LocalDateTime? = null,
        recruitmentStartAt: LocalDateTime? = null,
        recruitmentEndAt: LocalDateTime? = null,
    ) = Event(
        id = id,
        title = "테스트 행사 $id",
        startAt = startAt,
        endAt = endAt,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
        uri = "https://example.com",
        thumbnail = null,
        eventType = EventType.CONFERENCE,
        status = status,
        statusGroup = if (status == EventStatus.FINISHED) EventStatusGroup.FINISHED else EventStatusGroup.ACTIVE,
        host = host
    )

    @Nested
    @DisplayName("processMissedStatusUpdates")
    inner class ProcessMissedStatusUpdatesTests {

        @Test
        @DisplayName("상태가 변경된 이벤트만 저장한다")
        fun `상태가 실제로 변경된 이벤트만 save 호출`() {
            val now = LocalDateTime.now()
            val shouldChange = createEvent(
                id = 1L,
                status = EventStatus.ACTIVE,
                startAt = now.minusDays(3),
                endAt = now.minusDays(1)
            )
            val shouldNotChange = createEvent(
                id = 2L,
                status = EventStatus.RECRUITING,
                startAt = now.plusDays(10),
                endAt = now.plusDays(11),
                recruitmentStartAt = now.minusDays(1),
                recruitmentEndAt = now.plusDays(3)
            )
            every { eventRepository.findEventsWithIncorrectStatus(any()) } returns listOf(shouldChange, shouldNotChange)
            every { eventRepository.save(any<Event>()) } answers { firstArg() }

            scheduler.processMissedStatusUpdates()

            verify(exactly = 1) { eventRepository.save(match { it.id == 1L }) }
            verify(exactly = 0) { eventRepository.save(match { it.id == 2L }) }
        }

        @Test
        @DisplayName("잘못된 상태가 없으면 save를 호출하지 않는다")
        fun `빈 목록이면 save 미호출`() {
            every { eventRepository.findEventsWithIncorrectStatus(any()) } returns emptyList()

            scheduler.processMissedStatusUpdates()

            verify(exactly = 0) { eventRepository.save(any<Event>()) }
        }

        @Test
        @DisplayName("여러 이벤트의 상태가 모두 변경되면 모두 저장한다")
        fun `모든 이벤트 상태 변경 시 모두 save`() {
            val now = LocalDateTime.now()
            val event1 = createEvent(
                id = 1L,
                status = EventStatus.ACTIVE,
                startAt = now.minusDays(3),
                endAt = now.minusDays(1)
            )
            val event2 = createEvent(
                id = 2L,
                status = EventStatus.RECRUITING,
                startAt = now.minusDays(1),
                endAt = null,
                recruitmentStartAt = now.minusDays(5),
                recruitmentEndAt = now.minusDays(2)
            )
            every { eventRepository.findEventsWithIncorrectStatus(any()) } returns listOf(event1, event2)
            every { eventRepository.save(any<Event>()) } answers { firstArg() }

            scheduler.processMissedStatusUpdates()

            verify(exactly = 1) { eventRepository.save(match { it.id == 1L }) }
            verify(exactly = 1) { eventRepository.save(match { it.id == 2L }) }
        }
    }
}
