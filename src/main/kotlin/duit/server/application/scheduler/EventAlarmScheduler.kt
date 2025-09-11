package duit.server.application.scheduler

import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.alarm.service.AlarmService
import duit.server.domain.event.repository.EventRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
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
class EventAlarmScheduler(
    private val eventRepository: EventRepository,
    private val alarmService: AlarmService,
    private val taskScheduler: TaskScheduler
) {
    /**
     * 매일 자정에 오늘의 알림들을 스케줄링
     */
    @Scheduled(cron = "0 0 0 * * *")
    fun scheduleDailyAlarms() {
        val today = LocalDate.now()

        scheduleRecruitmentStartAlarms(today)
        scheduleEventStartAlarms(today)
    }

    /**
     * 애플리케이션 시작 시 오늘의 알림들을 스케줄링 (배포 복구용)
     */
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        scheduleDailyAlarms()
    }

    /**
     * 오늘 모집 시작하는 행사들의 알림 스케줄링
     */
    private fun scheduleRecruitmentStartAlarms(today: LocalDate) {
        val startOfDay = today.atStartOfDay()
        val startOfNextDay = today.plusDays(1).atStartOfDay()
        val recruitmentEvents = eventRepository.findRecruitmentStartingToday(startOfDay, startOfNextDay)

        recruitmentEvents.forEach { event ->
            event.recruitmentStartAt?.let { recruitmentStartTime ->
                if (recruitmentStartTime.isAfter(LocalDateTime.now())) {
                    val instant = recruitmentStartTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()

                    taskScheduler.schedule({
                        alarmService.sendAlarm(AlarmType.RECRUITMENT_START, event)
                    }, instant)
                }
            }
        }
    }

    /**
     * 오늘 시작하는 행사들의 알림 스케줄링
     */
    private fun scheduleEventStartAlarms(today: LocalDate) {
        val startingEvents = eventRepository.findEventsStartingToday(today)

        startingEvents.forEach { event ->
            val alarmTime = today.atTime(9, 0)
            if (alarmTime.isAfter(LocalDateTime.now())) {
                val instant = alarmTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()

                taskScheduler.schedule({
                    alarmService.sendAlarm(AlarmType.EVENT_START, event)
                }, instant)
            }
        }
    }
}