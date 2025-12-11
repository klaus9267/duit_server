package duit.server.application.scheduler

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.event.service.EventService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Component
@EnableScheduling
@Profile("prod")
class EventStatusScheduler(
    private val eventRepository: EventRepository,
    private val eventService: EventService,
    private val taskScheduler: TaskScheduler
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        scheduleDailyStatusUpdates()
    }

    /**
     * 매일 자정에 실행 - 놓친 업데이트 처리 및 오늘 상태 전환 스케줄링
     */
    @Scheduled(cron = "0 0 0 * * *")
    fun scheduleDailyStatusUpdates() {
        logger.info("=== Starting daily event status batch job at 00:00 ===")

        try {
            // STEP 1: 놓친 상태 업데이트 일괄 처리 (catch-up)
            logger.info("Step 1: Processing missed status updates")
            processMissedStatusUpdates()

            // STEP 2: 오늘 전환될 이벤트 스케줄링 (기존 동작 유지)
            logger.info("Step 2: Scheduling today's status transitions")
            scheduleTodayTransitions()

            logger.info("=== Daily status batch job completed successfully ===")
        } catch (e: Exception) {
            logger.error("Error during daily status batch job", e)
        }
    }

    /**
     * 오늘 상태 전환이 예정된 이벤트들을 정확한 시각에 스케줄링
     */
    private fun scheduleTodayTransitions() {
        EventStatus.schedulable().forEach { status ->
            val nextStatus = status.nextStatus!!

            val events = eventRepository.findEventsForScheduler(status)
            logger.info("Found ${events.size} events for $status -> $nextStatus transition today")

            events.forEach { scheduleStatusTransition(it, status, nextStatus) }
        }
    }

    /**
     * 개별 이벤트의 상태 전환을 정확한 시각에 스케줄링
     */
    private fun scheduleStatusTransition(
        event: Event,
        status: EventStatus,
        nextStatus: EventStatus
    ) {
        val today = LocalDate.now().atStartOfDay()
        val transitionTime = when (status) {
            EventStatus.RECRUITMENT_WAITING -> event.recruitmentStartAt
            EventStatus.RECRUITING -> event.recruitmentEndAt
            EventStatus.EVENT_WAITING -> event.startAt
            EventStatus.ACTIVE -> event.startAt
            EventStatus.FINISHED -> event.endAt ?: event.startAt.plusDays(1)
            else -> null
        }

        transitionTime?.takeIf { it >= today }?.let { time ->
            val instant = time.atZone(ZoneId.of("Asia/Seoul")).toInstant()
            taskScheduler.schedule({
                logger.info("Updating event ${event.id} to $nextStatus")
                eventService.updateStatus(event.id!!)
            }, instant)
        }
    }

    /**
     * 놓친 상태 업데이트를 일괄 처리 (catch-up mechanism)
     * 서버 재시작, 장애 등으로 인해 제때 업데이트되지 못한 이벤트들을 수정
     */
    fun processMissedStatusUpdates() {
        val now = LocalDateTime.now()
        val eventsNeedingUpdate = eventRepository.findEventsWithIncorrectStatus(now)

        logger.info("Found ${eventsNeedingUpdate.size} events with incorrect status")

        eventsNeedingUpdate.forEach { event ->
            val oldStatus = event.status
            event.updateStatus(now)
            eventRepository.save(event)
            logger.info("Updated event ${event.id}: $oldStatus -> ${event.status}")
        }
    }
}
