package duit.server.domain.event.dto

import duit.server.domain.host.dto.HostResponse
import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import java.time.LocalDate
import java.time.LocalDateTime

data class EventResponse(
    val id: Long,
    val title: String,
    val startAt: LocalDate,
    val endAt: LocalDate?,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val uri: String,
    val thumbnail: String?,
    val isApproved: Boolean,
    val eventType: EventType,
    val host: HostResponse
) {
    companion object {
        fun from(event: Event) = EventResponse(
            id = event.id!!,
            title = event.title,
            startAt = event.startAt,
            endAt = event.endAt,
            recruitmentStartAt = event.recruitmentStartAt,
            recruitmentEndAt = event.recruitmentEndAt,
            uri = event.uri,
            thumbnail = event.thumbnail,
            isApproved = event.isApproved,
            eventType = event.eventType,
            host = HostResponse.from(event.host)
        )
    }
}
