package duit.server.domain.alarm.repository

import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
import org.springframework.data.jpa.repository.JpaRepository

interface AlarmRepository : JpaRepository<Alarm, Long> {
    fun existsByUserIdAndEventIdAndType(userId: Long, eventId: Long, type: AlarmType): Boolean
}
