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
        val tomorrow = LocalDate.now().plusDays(1)

        scheduleRecruitmentStartAlarms(tomorrow)
        scheduleRecruitmentEndAlarms(tomorrow)
        scheduleEventStartAlarms(tomorrow)
    }

    /**
     * 애플리케이션 시작 시 오늘의 알림들을 스케줄링 (배포 복구용)
     */
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        scheduleDailyAlarms()
    }

    /**
     * 내일 모집 시작하는 행사들의 알림 스케줄링
     */
    private fun scheduleRecruitmentStartAlarms(tomorrow: LocalDate) {
        val startOfDay = tomorrow.atStartOfDay()
        val startOfNextDay = tomorrow.plusDays(1).atStartOfDay()
        val recruitmentEvents = eventRepository.findRecruitmentStartingTomorrow(startOfDay, startOfNextDay)

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
     * 내일 모집 마감하는 행사들의 알림 스케줄링
     */
    private fun scheduleRecruitmentEndAlarms(tomorrow: LocalDate) {
        val startOfDay = tomorrow.atStartOfDay()
        val startOfNextDay = tomorrow.plusDays(1).atStartOfDay()
        val recruitmentEvents = eventRepository.findRecruitmentEndingTomorrow(startOfDay, startOfNextDay)

        recruitmentEvents.forEach { event ->
            event.recruitmentEndAt?.let { recruitmentEndTime ->
                if (recruitmentEndTime.isAfter(LocalDateTime.now())) {
                    val instant = recruitmentEndTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()

                    taskScheduler.schedule({
                        alarmService.sendAlarm(AlarmType.RECRUITMENT_END, event)
                    }, instant)
                }
            }
        }
    }

    /**
     * 내일 시작하는 행사들의 알림 스케줄링
     */
    private fun scheduleEventStartAlarms(tomorrow: LocalDate) {
        val startingEvents = eventRepository.findEventsStartingTomorrow(tomorrow)

        startingEvents.forEach { event ->
            val alarmTime = tomorrow.atTime(9, 0)
            if (alarmTime.isAfter(LocalDateTime.now())) {
                val instant = alarmTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()

                taskScheduler.schedule({
                    alarmService.sendAlarm(AlarmType.EVENT_START, event)
                }, instant)
            }
        }
    }
}