package duit.server.domain.event.dto

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.dto.HostResponse
import io.swagger.v3.oas.annotations.media.Schema
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
    val eventType: EventType,
    // TODO(event-hosts): 구 클라이언트 호환용 (= hosts 의 첫 번째). 모든 클라이언트가 hosts 로 전환되면 제거
    @field:Schema(description = "[Deprecated: hosts 사용] 첫 번째 주최", deprecated = true)
    val host: HostResponse,
    @field:Schema(description = "주최 목록 (등록 순서)")
    val hosts: List<HostResponse>,
    val viewCount: Int,
    val isBookmarked: Boolean = false
) {
    companion object {
        fun from(event: Event, isBookmarked: Boolean = false): EventResponse {
            val hosts = event.hosts.map(HostResponse::from)
            return EventResponse(
                id = event.id!!,
                title = event.title,
                startAt = event.startAt,
                endAt = event.endAt,
                recruitmentStartAt = event.recruitmentStartAt,
                recruitmentEndAt = event.recruitmentEndAt,
                uri = event.uri,
                thumbnail = event.thumbnail,
                eventType = event.eventType,
                host = hosts.first(),
                hosts = hosts,
                viewCount = event.view.count,
                isBookmarked = isBookmarked
            )
        }
    }
}
