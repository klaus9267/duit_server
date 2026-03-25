package duit.server.application.scheduler

import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.alarm.service.AlarmService
import duit.server.domain.event.entity.EventDate
import duit.server.domain.event.repository.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Component
@EnableScheduling
@Profile("prod")
class EventAlarmScheduler(
    private val eventRepository: EventRepository,
    private val alarmService: AlarmService,
    private val taskScheduler: TaskScheduler
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val scheduledKeys = mutableSetOf<String>()
    private var lastScheduledDate: LocalDate? = null
    private val zoneId = ZoneId.of("Asia/Seoul")

    // 매일 새벽 4시에 당일 알람 스케줄 등록
    @Scheduled(cron = "0 0 4 * * *")
    fun createDailyAlarms() {
        synchronized(this) {
            val today = LocalDate.now()
            if (lastScheduledDate != today) {
                scheduledKeys.clear()
                lastScheduledDate = today
            }
            createAlarmsByType(EventDate.RECRUITMENT_START_AT, AlarmType.RECRUITMENT_START)
            createAlarmsByType(EventDate.RECRUITMENT_END_AT, AlarmType.RECRUITMENT_END)
            createAlarmsByType(EventDate.START_AT, AlarmType.EVENT_START)
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        createDailyAlarms()
    }

    private fun createAlarmsByType(eventDate: EventDate, alarmType: AlarmType) {
        val tomorrow = LocalDateTime.now().plusDays(1)
        val nextDay = tomorrow.plusDays(1)
        val events = eventRepository.findEventsByDateField(eventDate.name, tomorrow, nextDay)

        events.forEach { event ->
            val scheduleKey = "${event.id}-${alarmType}"
            if (!scheduledKeys.add(scheduleKey)) return@forEach

            val targetDateTime = when (eventDate) {
                EventDate.START_AT -> event.startAt
                EventDate.RECRUITMENT_START_AT -> event.recruitmentStartAt!!
                EventDate.RECRUITMENT_END_AT -> event.recruitmentEndAt!!
            }
            val alarmTime = calculateAlarmTime(targetDateTime)

            if (alarmTime.isAfter(LocalDateTime.now())) {
                val instant: Instant = alarmTime.atZone(zoneId).toInstant()

                taskScheduler.schedule({
                    try {
                        alarmService.createAlarms(alarmType, event.id!!)
                    } catch (_: DataIntegrityViolationException) {
                        log.debug("알람 중복 생성 무시 - eventId: {}, type: {} (동시 스케줄 실행)", event.id, alarmType)
                    } catch (e: Exception) {
                        log.error("알람 생성 실패 - eventId: {}, type: {}", event.id, alarmType, e)
                    }
                }, instant)
            }
        }
    }

    private fun calculateAlarmTime(targetDateTime: LocalDateTime): LocalDateTime {
        val targetDate = targetDateTime.toLocalDate()
        val targetTime = targetDateTime.toLocalTime()

        return when {
            targetTime >= LocalTime.of(20, 0) || targetTime <= LocalTime.of(7, 0) ->
                targetDate.minusDays(1).atTime(20, 0)
            else -> targetDateTime.minusDays(1)
        }
    }
}
