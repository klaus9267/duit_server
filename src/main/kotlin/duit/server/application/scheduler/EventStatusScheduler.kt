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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

@Component
@EnableScheduling
@Profile("prod")
class EventStatusScheduler(
    private val eventRepository: EventRepository,
    private val eventService: EventService,
    private val taskScheduler: TaskScheduler
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // 스케줄된 작업 추적용 맵 (eventId -> 작업 정보)
    private val scheduledTasks = ConcurrentHashMap<Long, ScheduledTaskInfo>()

    data class ScheduledTaskInfo(
        val eventId: Long,
        val eventTitle: String,
        val currentStatus: EventStatus,
        val nextStatus: EventStatus,
        val scheduledTime: LocalDateTime,
        val future: ScheduledFuture<*>
    )

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
            scheduleTodaysTransitions()

            logger.info("=== Daily status batch job completed successfully ===")
        } catch (e: Exception) {
            logger.error("Error during daily status batch job", e)
        }
    }

    /**
     * 오늘 상태 전환이 예정된 이벤트들을 정확한 시각에 스케줄링
     */
    private fun scheduleTodaysTransitions() {
        // 기존 스케줄 정리 (완료된 작업 제거)
        cleanupCompletedTasks()

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
            EventStatus.ACTIVE -> event.startAt
            EventStatus.FINISHED -> event.endAt ?: event.startAt.plusDays(1)
            else -> null
        }

        transitionTime?.takeIf { it >= today }?.let { time ->
            val instant = time.atZone(ZoneId.of("Asia/Seoul")).toInstant()
            val future = taskScheduler.schedule({
                logger.info("Updating event ${event.id} to $nextStatus")
                eventService.updateStatus(event.id!!, nextStatus)
                // 실행 완료 후 맵에서 제거
                scheduledTasks.remove(event.id)
            }, instant)

            // 스케줄 정보 저장
            future?.let {
                val taskInfo = ScheduledTaskInfo(
                    eventId = event.id!!,
                    eventTitle = event.title,
                    currentStatus = status,
                    nextStatus = nextStatus,
                    scheduledTime = time,
                    future = it
                )
                scheduledTasks[event.id!!] = taskInfo
                logger.info("Scheduled $nextStatus status for event ${event.id} at $time")
            }
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
            event.updateStatus()
            eventRepository.save(event)
            logger.info("Updated event ${event.id}: $oldStatus -> ${event.status}")
        }
    }

    /**
     * 완료되거나 취소된 작업 정리
     */
    private fun cleanupCompletedTasks() {
        val iterator = scheduledTasks.entries.iterator()
        var removedCount = 0

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.future.isDone || entry.value.future.isCancelled) {
                iterator.remove()
                removedCount++
            }
        }

        if (removedCount > 0) {
            logger.info("Cleaned up $removedCount completed tasks")
        }
    }

    /**
     * 현재 스케줄된 작업 목록 조회
     */
    fun getScheduledTasks(): List<ScheduledTaskSummary> {
        cleanupCompletedTasks()

        return scheduledTasks.values
            .sortedBy { it.scheduledTime }
            .map { taskInfo ->
                ScheduledTaskSummary(
                    eventId = taskInfo.eventId,
                    eventTitle = taskInfo.eventTitle,
                    currentStatus = taskInfo.currentStatus.name,
                    nextStatus = taskInfo.nextStatus.name,
                    scheduledTime = taskInfo.scheduledTime,
                    remainingDelay = calculateRemainingDelay(taskInfo.future)
                )
            }
    }

    /**
     * 남은 대기 시간 계산
     */
    private fun calculateRemainingDelay(future: ScheduledFuture<*>): String {
        val delayMillis = future.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS)
        if (delayMillis < 0) {
            return "실행 대기 중"
        }

        val hours = delayMillis / (1000 * 60 * 60)
        val minutes = (delayMillis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (delayMillis % (1000 * 60)) / 1000

        return when {
            hours > 0 -> "${hours}시간 ${minutes}분"
            minutes > 0 -> "${minutes}분 ${seconds}초"
            else -> "${seconds}초"
        }
    }

    data class ScheduledTaskSummary(
        val eventId: Long,
        val eventTitle: String,
        val currentStatus: String,
        val nextStatus: String,
        val scheduledTime: LocalDateTime,
        val remainingDelay: String
    )
}
