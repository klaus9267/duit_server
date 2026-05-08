package duit.server.domain.alarm.dto

import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
import duit.server.domain.event.dto.EventResponse
import java.time.LocalDateTime

/**
 * V1 응답 — 이벤트 알람 전용. [duit.server.domain.alarm.service.AlarmService.getAlarms] 가 event IS NOT NULL 로 필터해서 호출.
 * 채용/구독 알람의 폴리모픽 응답은 V2 (Phase 3) 에서 추가.
 */
data class AlarmResponse(
    val id: Long,
    val type: AlarmType,
    val event: EventResponse,
    val isRead: Boolean,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(alarm: Alarm): AlarmResponse {
            val event = alarm.event
                ?: error("AlarmResponse(V1) 은 event 알람만 변환 가능합니다 — alarmId=${alarm.id}")
            return AlarmResponse(
                id = alarm.id!!,
                type = alarm.type,
                event = EventResponse.from(event),
                isRead = alarm.isRead,
                createdAt = alarm.createdAt
            )
        }
    }
}
