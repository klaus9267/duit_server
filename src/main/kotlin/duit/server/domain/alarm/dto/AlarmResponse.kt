package duit.server.domain.alarm.dto

import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.event.dto.EventResponse
import java.time.LocalDateTime

data class AlarmResponse(
    val id: Long,
    val type: AlarmType,
    val event: EventResponse,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(alarm: Alarm): AlarmResponse {
            return AlarmResponse(
                id = alarm.id!!,
                type = alarm.type,
                event = EventResponse.from(alarm.event),
                createdAt = alarm.createdAt
            )
        }
    }
}
