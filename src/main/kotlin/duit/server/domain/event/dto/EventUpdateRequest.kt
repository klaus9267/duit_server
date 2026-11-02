package duit.server.domain.event.dto

import duit.server.domain.event.entity.EventType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@ValidDateRange
data class EventUpdateRequest(
    @field:NotBlank(message = "행사 제목은 필수입니다")
    @field:Schema(description = "행사 제목", example = "2025 AI 컨퍼런스")
    val title: String,

    @field:NotNull(message = "행사 시작일은 필수입니다")
    @field:Schema(description = "행사 시작 일시", example = "2025-02-15T09:00:00")
    val startAt: LocalDateTime,

    @field:Schema(description = "행사 종료 일시", example = "2025-02-15T18:00:00")
    val endAt: LocalDateTime?,

    @field:Schema(description = "모집 시작 일시", example = "2025-01-10T00:00:00")
    val recruitmentStartAt: LocalDateTime?,

    @field:Schema(description = "모집 종료 일시", example = "2025-02-10T23:59:59")
    val recruitmentEndAt: LocalDateTime?,

    @field:NotBlank(message = "행사 URL은 필수입니다")
    @field:Schema(description = "행사 상세 정보 URL", example = "https://example.com/event/123")
    val uri: String,

    @field:NotNull(message = "행사 종류는 필수입니다")
    @field:Schema(description = "행사 종류", example = "CONFERENCE")
    val eventType: EventType,

    @field:Valid
    @field:Size(min = 1, max = MAX_EVENT_HOSTS, message = "주최는 1개 이상 {max}개 이하로 입력해야 합니다")
    @field:Schema(description = "주최 목록 (순서 유지, 첫 번째 주최가 응답의 host 로 사용됨). 기존 목록을 통째로 교체. hostId/hostName 과 함께 사용 불가")
    override val hosts: List<EventHostRequest>? = null,

    // TODO(event-hosts): 모든 클라이언트가 hosts 로 전환되면 hostId/hostName/deleteHostThumbnail 제거
    @field:Schema(description = "[Deprecated: hosts 사용] 주최 기관 ID (기존 기관 사용 시)", example = "1", deprecated = true)
    override val hostId: Long? = null,

    @field:Schema(description = "[Deprecated: hosts 사용] 주최 기관명 (신규 기관 생성 시)", example = "단국대학교 IT 대학", deprecated = true)
    override val hostName: String? = null,

    @field:Schema(description = "행사 썸네일 삭제 여부", example = "false", defaultValue = "false")
    val deleteEventThumbnail: Boolean = false,

    @field:Schema(
        description = "[Deprecated] hostName 으로 새 주최가 생성될 때 로고 없이 생성할지 여부. hosts 와 함께 사용 불가 — 기존 주최 로고 삭제는 PUT /api/v1/hosts/{hostId}",
        example = "false",
        defaultValue = "false",
        deprecated = true
    )
    val deleteHostThumbnail: Boolean = false
) : EventHostSource
