package duit.server.application.scheduler

import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.alarm.service.AlarmService
import duit.server.domain.event.entity.EventDate
import duit.server.domain.event.repository.EventRepository
import duit.server.domain.event.repository.EventRepositoryCustom
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

@Component
@EnableScheduling
class EventAlarmScheduler(
    private val eventRepository: EventRepository,
    private val eventRepositoryCustom: EventRepositoryCustom,
    private val alarmService: AlarmService,
    private val taskScheduler: TaskScheduler
) {
    /**
     * 매일 자정에 오늘의 알림들을 스케줄링
     */
    @Scheduled(cron = "0 0 0 * * *")
    fun scheduleDailyAlarms() {
        scheduleRecruitmentStartAlarms()
        scheduleRecruitmentEndAlarms()
        scheduleEventStartAlarms()
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
    private fun scheduleRecruitmentStartAlarms() {
        val recruitmentEvents = eventRepositoryCustom.findEventsByDateField(EventDate.RECRUITMENT_START_AT)

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
    private fun scheduleRecruitmentEndAlarms() {
        val recruitmentEvents = eventRepositoryCustom.findEventsByDateField(EventDate.RECRUITMENT_END_AT)

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
    private fun scheduleEventStartAlarms() {
        val startingEvents = eventRepositoryCustom.findEventsByDateField(EventDate.START_AT)

        startingEvents.forEach { event ->
            val alarmTime = event.startAt
            if (alarmTime.isAfter(LocalDateTime.now())) {
                val instant = alarmTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()

                taskScheduler.schedule({
                    alarmService.sendAlarm(AlarmType.EVENT_START, event)
                }, instant)
            }
        }
    }
}