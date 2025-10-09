package duit.server.domain.alarm.repository

import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface AlarmRepository : JpaRepository<Alarm, Long> {
    fun existsByUserIdAndEventIdAndType(userId: Long, eventId: Long, type: AlarmType): Boolean

    @EntityGraph(attributePaths = ["event", "event.host"])
    fun findByUserId(userId: Long, pageable: Pageable): Page<Alarm>
}
