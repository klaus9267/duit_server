package duit.server.domain.subscription.dto

import duit.server.domain.event.entity.EventType
import duit.server.domain.subscription.entity.SubscriptionType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(
    description = """
        구독 생성 요청 — type 별 필수 필드:
        EVENT_KEYWORD/JOB_KEYWORD → keyword,  EVENT_HOST → hostId,  EVENT_TYPE → eventType,  JOB_COMPANY → companyId
    """
)
data class SubscriptionCreateRequest(
    @field:NotNull(message = "type 은 필수입니다")
    @field:Schema(description = "구독 종류", example = "EVENT_HOST")
    val type: SubscriptionType,

    @field:Schema(description = "키워드 (KEYWORD 타입 전용)", example = "AI 컨퍼런스")
    val keyword: String? = null,

    @field:Schema(description = "주최 ID (EVENT_HOST 전용)", example = "1")
    val hostId: Long? = null,

    @field:Schema(description = "행사 유형 (EVENT_TYPE 전용)", example = "CONFERENCE")
    val eventType: EventType? = null,

    @field:Schema(description = "회사 ID (JOB_COMPANY 전용)", example = "42")
    val companyId: Long? = null,
)
