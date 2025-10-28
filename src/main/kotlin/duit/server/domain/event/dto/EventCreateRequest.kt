package duit.server.domain.event.dto

import duit.server.domain.event.entity.Event
import duit.server.domain.event.entity.EventType
import duit.server.domain.host.entity.Host
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@ValidDateRange
data class EventCreateRequest(
    @field:NotBlank(message = "행사 제목은 필수입니다")
    @field:Schema(description = "행사 제목", example = "2025 AI 컨퍼런스")
    val title: String,

    @field:Future(message = "행사 시작일은 미래여야 합니다")
    @field:NotNull(message = "행사 시작일은 필수입니다")
    @field:Schema(description = "행사 시작 일시", example = "2025-02-15T09:00:00")
    val startAt: LocalDateTime,

    @field:Future(message = "행사 종료일은 미래여야 합니다")
    @field:Schema(description = "행사 종료 일시", example = "2025-02-15T18:00:00")
    val endAt: LocalDateTime?,

    @field:Future(message = "모집 시작일은 미래여야 합니다")
    @field:Schema(description = "모집 시작 일시", example = "2025-01-10T00:00:00")
    val recruitmentStartAt: LocalDateTime?,

    @field:Future(message = "모집 종료일은 미래여야 합니다")
    @field:Schema(description = "모집 종료 일시", example = "2025-02-10T23:59:59")
    val recruitmentEndAt: LocalDateTime?,

    @field:NotBlank(message = "행사 URL은 필수입니다")
    @field:Schema(description = "행사 상세 정보 URL", example = "https://example.com/event/123")
    val uri: String,

    @field:NotNull(message = "행사 종류는 필수입니다")
    @field:Schema(description = "행사 종류", example = "CONFERENCE")
    val eventType: EventType,

    @field:Schema(description = "주최 기관 ID (기존 기관 사용 시)", example = "1")
    val hostId: Long? = null,

    @field:Schema(description = "주최 기관명 (신규 기관 생성 시)", example = "단국대학교 IT 대학")
    val hostName: String? = null
) {
    fun toEntity(host: Host): Event = Event(
        title = title,
        startAt = startAt,
        endAt = endAt,
        recruitmentStartAt = recruitmentStartAt,
        recruitmentEndAt = recruitmentEndAt,
        uri = uri,
        thumbnail = null,
        eventType = eventType,
        host = host
    )
}
