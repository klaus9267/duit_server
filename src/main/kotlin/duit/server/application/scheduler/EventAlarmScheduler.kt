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
import java.time.Instant
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
    @Scheduled(cron = "0 0 0 * * *")
    fun scheduleDailyAlarms() {
        scheduleAlarmsByType(EventDate.RECRUITMENT_START_AT, AlarmType.RECRUITMENT_START)
        scheduleAlarmsByType(EventDate.RECRUITMENT_END_AT, AlarmType.RECRUITMENT_END)
        scheduleAlarmsByType(EventDate.START_AT, AlarmType.EVENT_START)
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        scheduleDailyAlarms()
    }

    private fun scheduleAlarmsByType(eventDate: EventDate, alarmType: AlarmType) {
        val alarmInfos = eventRepositoryCustom.findEventAlarmInfoByDateField(eventDate)

        alarmInfos.forEach { info ->
            val alarmTime = info.targetDateTime.minusHours(24)
            if (alarmTime.isAfter(LocalDateTime.now())) {
                val instant: Instant = alarmTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()

                taskScheduler.schedule({
                    val event = eventRepository.findById(info.id).orElse(null) ?: return@schedule
                    alarmService.sendAlarm(alarmType, event)
                }, instant)
            }
        }
    }
}