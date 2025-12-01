package duit.server.domain.event.dto

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventStatus
import duit.server.domain.event.entity.EventStatusGroup
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.dto.HostResponse
import java.time.LocalDateTime

data class EventResponseV2(
    val id: Long,
    val title: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val uri: String,
    val thumbnail: String?,
    val eventType: EventType,
    val eventStatus: EventStatus,
    val eventStatusGroup: EventStatusGroup,
    val host: HostResponse,
    val viewCount: Int,
    val isBookmarked: Boolean = false
) {
    companion object {
        fun from(event: Event, isBookmarked: Boolean = false) = EventResponseV2(
            id = event.id!!,
            title = event.title,
            startAt = event.startAt,
            endAt = event.endAt,
            recruitmentStartAt = event.recruitmentStartAt,
            recruitmentEndAt = event.recruitmentEndAt,
            uri = event.uri,
            thumbnail = event.thumbnail,
            eventType = event.eventType,
            eventStatus = event.status,
            eventStatusGroup = event.statusGroup,
            host = HostResponse.from(event.host),
            viewCount = event.view?.count ?: 0,
            isBookmarked = isBookmarked
        )
    }
}
