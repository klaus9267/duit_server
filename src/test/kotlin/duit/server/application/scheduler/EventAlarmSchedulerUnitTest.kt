package duit.server.application.scheduler

import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.alarm.service.AlarmService
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.host.entity.Host
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.TaskScheduler
import java.time.Instant
import java.time.LocalDateTime

@DisplayName("EventAlarmScheduler 단위 테스트")
class EventAlarmSchedulerUnitTest {

    private lateinit var eventRepository: EventRepository
    private lateinit var alarmService: AlarmService
    private lateinit var taskScheduler: TaskScheduler
    private lateinit var scheduler: EventAlarmScheduler

    private val host = Host(id = 1L, name = "테스트")

    @BeforeEach
    fun setUp() {
        eventRepository = mockk()
        alarmService = mockk(relaxed = true)
        taskScheduler = mockk(relaxed = true)
        scheduler = EventAlarmScheduler(eventRepository, alarmService, taskScheduler)

        every { eventRepository.findEventsByDateField(any(), any(), any()) } returns emptyList()
    }

    private fun createEvent(
        id: Long,
        startAt: LocalDateTime,
        endAt: LocalDateTime? = startAt.plusDays(1),
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
        status = EventStatus.RECRUITING,
        statusGroup = EventStatusGroup.ACTIVE,
        host = host
    )

    @Nested
    @DisplayName("createDailyAlarms")
    inner class CreateDailyAlarmsTests {

        @Test
        @DisplayName("3가지 알람 타입 모두 조회한다")
        fun `3가지 알람 타입 모두 조회`() {
            scheduler.createDailyAlarms()

            verify(exactly = 1) { eventRepository.findEventsByDateField("START_AT", any(), any()) }
            verify(exactly = 1) { eventRepository.findEventsByDateField("RECRUITMENT_START_AT", any(), any()) }
            verify(exactly = 1) { eventRepository.findEventsByDateField("RECRUITMENT_END_AT", any(), any()) }
        }

        @Test
        @DisplayName("이벤트가 있으면 TaskScheduler에 스케줄한다")
        fun `미래 알람 시각이면 스케줄`() {
            val now = LocalDateTime.now()
            val event = createEvent(
                id = 1L,
                startAt = now.plusDays(1).plusHours(10)
            )
            every { eventRepository.findEventsByDateField("START_AT", any(), any()) } returns listOf(event)

            scheduler.createDailyAlarms()

            verify(exactly = 1) { taskScheduler.schedule(any<Runnable>(), any<Instant>()) }
        }

        @Test
        @DisplayName("알람 시각이 과거이면 스케줄하지 않는다")
        fun `과거 알람 시각이면 스케줄 안함`() {
            val now = LocalDateTime.now()
            val event = createEvent(
                id = 1L,
                startAt = now.minusHours(12)
            )
            every { eventRepository.findEventsByDateField("START_AT", any(), any()) } returns listOf(event)

            scheduler.createDailyAlarms()

            verify(exactly = 0) { taskScheduler.schedule(any<Runnable>(), any<Instant>()) }
        }

        @Test
        @DisplayName("이벤트가 없으면 스케줄하지 않는다")
        fun `빈 목록이면 스케줄 안함`() {
            scheduler.createDailyAlarms()

            verify(exactly = 0) { taskScheduler.schedule(any<Runnable>(), any<Instant>()) }
        }

        @Test
        @DisplayName("여러 타입에 이벤트가 있으면 각각 스케줄한다")
        fun `여러 타입 각각 스케줄`() {
            val now = LocalDateTime.now()
            val eventForStart = createEvent(
                id = 1L,
                startAt = now.plusDays(1).plusHours(10)
            )
            val eventForRecruitmentEnd = createEvent(
                id = 2L,
                startAt = now.plusDays(10),
                recruitmentStartAt = now.minusDays(5),
                recruitmentEndAt = now.plusDays(1).plusHours(15)
            )
            every { eventRepository.findEventsByDateField("START_AT", any(), any()) } returns listOf(eventForStart)
            every { eventRepository.findEventsByDateField("RECRUITMENT_END_AT", any(), any()) } returns listOf(eventForRecruitmentEnd)

            scheduler.createDailyAlarms()

            verify(exactly = 2) { taskScheduler.schedule(any<Runnable>(), any<Instant>()) }
        }
    }

    @Nested
    @DisplayName("스케줄된 Runnable 실행 검증")
    inner class ScheduledRunnableTests {

        @Test
        @DisplayName("스케줄된 Runnable이 실행되면 alarmService.createAlarms를 호출한다")
        fun `Runnable 실행 시 alarmService 호출`() {
            val now = LocalDateTime.now()
            val event = createEvent(id = 1L, startAt = now.plusDays(1).plusHours(10))
            every { eventRepository.findEventsByDateField("START_AT", any(), any()) } returns listOf(event)

            val capturedRunnables = mutableListOf<Runnable>()
            every { taskScheduler.schedule(capture(capturedRunnables), any<Instant>()) } returns mockk()

            scheduler.createDailyAlarms()

            assertEquals(1, capturedRunnables.size)
            capturedRunnables[0].run()

            verify(exactly = 1) { alarmService.createAlarms(AlarmType.EVENT_START, 1L) }
        }

        @Test
        @DisplayName("스케줄된 Runnable에서 DataIntegrityViolationException 발생 시 예외를 삼킨다")
        fun `Runnable에서 DataIntegrityViolationException은 무시된다`() {
            val now = LocalDateTime.now()
            val event = createEvent(id = 1L, startAt = now.plusDays(1).plusHours(10))
            every { eventRepository.findEventsByDateField("START_AT", any(), any()) } returns listOf(event)

            val capturedRunnables = mutableListOf<Runnable>()
            every { taskScheduler.schedule(capture(capturedRunnables), any<Instant>()) } returns mockk()
            every { alarmService.createAlarms(AlarmType.EVENT_START, 1L) } throws
                DataIntegrityViolationException("Duplicate entry")

            scheduler.createDailyAlarms()
            capturedRunnables[0].run()
        }

        @Test
        @DisplayName("스케줄된 Runnable에서 DataIntegrityViolationException 외 예외는 전파된다")
        fun `Runnable에서 다른 예외는 전파된다`() {
            val now = LocalDateTime.now()
            val event = createEvent(id = 1L, startAt = now.plusDays(1).plusHours(10))
            every { eventRepository.findEventsByDateField("START_AT", any(), any()) } returns listOf(event)

            val capturedRunnables = mutableListOf<Runnable>()
            every { taskScheduler.schedule(capture(capturedRunnables), any<Instant>()) } returns mockk()
            every { alarmService.createAlarms(AlarmType.EVENT_START, 1L) } throws
                RuntimeException("DB connection lost")

            scheduler.createDailyAlarms()

            org.junit.jupiter.api.assertThrows<RuntimeException> {
                capturedRunnables[0].run()
            }
        }

        @Test
        @DisplayName("RECRUITMENT_START 타입의 Runnable이 올바른 AlarmType으로 호출된다")
        fun `RECRUITMENT_START 타입 Runnable 검증`() {
            val now = LocalDateTime.now()
            val event = createEvent(
                id = 5L,
                startAt = now.plusDays(10),
                recruitmentStartAt = now.plusDays(1).plusHours(9)
            )
            every { eventRepository.findEventsByDateField("RECRUITMENT_START_AT", any(), any()) } returns listOf(event)

            val capturedRunnables = mutableListOf<Runnable>()
            every { taskScheduler.schedule(capture(capturedRunnables), any<Instant>()) } returns mockk()

            scheduler.createDailyAlarms()
            capturedRunnables[0].run()

            verify(exactly = 1) { alarmService.createAlarms(AlarmType.RECRUITMENT_START, 5L) }
        }

        @Test
        @DisplayName("RECRUITMENT_END 타입의 Runnable이 올바른 AlarmType으로 호출된다")
        fun `RECRUITMENT_END 타입 Runnable 검증`() {
            val now = LocalDateTime.now()
            val event = createEvent(
                id = 7L,
                startAt = now.plusDays(10),
                recruitmentStartAt = now.minusDays(5),
                recruitmentEndAt = now.plusDays(1).plusHours(18)
            )
            every { eventRepository.findEventsByDateField("RECRUITMENT_END_AT", any(), any()) } returns listOf(event)

            val capturedRunnables = mutableListOf<Runnable>()
            every { taskScheduler.schedule(capture(capturedRunnables), any<Instant>()) } returns mockk()

            scheduler.createDailyAlarms()
            capturedRunnables[0].run()

            verify(exactly = 1) { alarmService.createAlarms(AlarmType.RECRUITMENT_END, 7L) }
        }
    }

    @Nested
    @DisplayName("중복 스케줄 등록 시나리오")
    inner class DuplicateScheduleTests {

        @Test
        @DisplayName("createDailyAlarms를 2번 호출해도 같은 이벤트에 대해 Runnable이 1개만 등록되어야 한다")
        fun `중복 호출 시 동일 이벤트에 Runnable 1개만 등록`() {
            val now = LocalDateTime.now()
            val event = createEvent(id = 1L, startAt = now.plusDays(1).plusHours(10))
            every { eventRepository.findEventsByDateField("START_AT", any(), any()) } returns listOf(event)

            scheduler.createDailyAlarms()
            scheduler.createDailyAlarms()

            verify(exactly = 1) { taskScheduler.schedule(any<Runnable>(), any<Instant>()) }
        }

        @Test
        @DisplayName("createDailyAlarms를 2번 호출해도 alarmService는 이벤트당 1번만 호출되어야 한다")
        fun `중복 호출해도 Runnable 실행 시 createAlarms 1번만 호출`() {
            val now = LocalDateTime.now()
            val event = createEvent(id = 1L, startAt = now.plusDays(1).plusHours(10))
            every { eventRepository.findEventsByDateField("START_AT", any(), any()) } returns listOf(event)
            val capturedRunnables = mutableListOf<Runnable>()
            every { taskScheduler.schedule(capture(capturedRunnables), any<Instant>()) } returns mockk()
            scheduler.createDailyAlarms()
            scheduler.createDailyAlarms()

            capturedRunnables.forEach { it.run() }

            verify(exactly = 1) { alarmService.createAlarms(AlarmType.EVENT_START, 1L) }
        }
    }
}
