package duit.server.application.scheduler

import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.alarm.service.AlarmService
import duit.server.domain.event.entity.EventDate
import duit.server.domain.event.repository.EventRepository
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
    private val alarmService: AlarmService,
    private val taskScheduler: TaskScheduler
) {
    // 매일 자정에 알람 생성
    @Scheduled(cron = "0 0 0 * * *")
    fun createDailyAlarms() {
        createAlarmsByType(EventDate.RECRUITMENT_START_AT, AlarmType.RECRUITMENT_START)
        createAlarmsByType(EventDate.RECRUITMENT_END_AT, AlarmType.RECRUITMENT_END)
        createAlarmsByType(EventDate.START_AT, AlarmType.EVENT_START)
    }

//    @EventListener(ApplicationReadyEvent::class)
//    fun onApplicationReady() {
//        createDailyAlarms()
//    }

    private fun createAlarmsByType(eventDate: EventDate, alarmType: AlarmType) {
        val tomorrow = LocalDateTime.now().plusDays(1)
        val nextDay = tomorrow.plusDays(1)
        val events = eventRepository.findEventsByDateField(eventDate.name, tomorrow, nextDay)

        events.forEach { event ->
            val alarmTime = when (eventDate) {
                EventDate.START_AT -> event.startAt
                EventDate.RECRUITMENT_START_AT -> event.recruitmentStartAt!!
                EventDate.RECRUITMENT_END_AT -> event.recruitmentEndAt!!
            }.minusDays(1)

            if (alarmTime.isAfter(LocalDateTime.now())) {
                val instant: Instant = alarmTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()

                taskScheduler.schedule({
                    alarmService.createAlarms(alarmType, event.id!!)
                }, instant)
            }
        }
    }
}