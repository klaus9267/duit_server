package duit.server.domain.event.dto

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.dto.HostResponse
import java.time.LocalDateTime

data class EventResponse(
    val id: Long,
    val title: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime?,
    val recruitmentStartAt: LocalDateTime?,
    val recruitmentEndAt: LocalDateTime?,
    val uri: String,
    val thumbnail: String?,
    val isApproved: Boolean,
    val eventType: EventType,
    val host: HostResponse,
    val viewCount: Int,
    val isBookmarked: Boolean = false
) {
    companion object {
        fun from(event: Event, isBookmarked: Boolean = false) = EventResponse(
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
            host = HostResponse.from(event.host),
            viewCount = event.view?.count ?: 0,
            isBookmarked = isBookmarked
        )
    }
}
