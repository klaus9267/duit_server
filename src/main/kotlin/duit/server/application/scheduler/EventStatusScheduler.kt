package duit.server.application.scheduler

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.event.service.EventService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

@Component
@EnableScheduling
class EventStatusScheduler(
    private val eventRepository: EventRepository,
    private val eventService: EventService,
    private val taskScheduler: TaskScheduler
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 매일 자정에 실행 - 오늘 상태 전환이 예정된 이벤트들을 스케줄링
     */
    @Scheduled(cron = "0 0 0 * * *")
    fun scheduleDailyStatusUpdates() {
        logger.info("Starting daily event status update scheduling")

        EventStatus.getTransitionableStatuses().forEach { status ->
            val nextStatus = status.nextStatus!!

            val events = eventRepository.findEventsForStatusTransition(status)
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
            EventStatus.ACTIVE -> event.startAt
            EventStatus.FINISHED -> event.endAt ?: event.startAt.plusDays(1)
            else -> null
        }

        transitionTime?.takeIf { it >= today }?.let { time ->
            val instant = time.atZone(ZoneId.of("Asia/Seoul")).toInstant()
            taskScheduler.schedule({
                logger.info("Updating event ${event.id} to $nextStatus")
                eventService.updateStatus(event.id!!, nextStatus)
            }, instant)
            logger.info("Scheduled $nextStatus status for event ${event.id} at $time")
        }
    }
}
