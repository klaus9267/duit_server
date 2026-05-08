package duit.server.domain.subscription.dto

import com.fasterxml.jackson.annotation.JsonInclude
import duit.server.domain.event.entity.EventType
import duit.server.domain.subscription.entity.SubscriptionType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 구독 응답 폴리모픽 — type 별 자식 클래스로 노출.
 *
 * 새 [SubscriptionType] 추가 시 자식 data class 1개 추가하면 끝 (기존 코드 수정 X).
 */
@Schema(
    description = "구독 응답 — type 에 따라 자식 클래스가 달라짐. type 필드로 클라이언트 분기.",
    oneOf = [
        EventKeywordSubscriptionResponse::class,
        EventHostSubscriptionResponse::class,
        EventTypeSubscriptionResponse::class,
        JobKeywordSubscriptionResponse::class,
        JobCompanySubscriptionResponse::class,
    ],
)
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed interface SubscriptionResponse {
    val id: Long
    val type: SubscriptionType
    val createdAt: LocalDateTime
}

@Schema(description = "행사 키워드 구독")
data class EventKeywordSubscriptionResponse(
    @field:Schema(description = "구독 ID", example = "1")
    override val id: Long,

    @field:Schema(description = "키워드", example = "AI 컨퍼런스")
    val keyword: String,

    @field:Schema(description = "생성 시각")
    override val createdAt: LocalDateTime,
) : SubscriptionResponse {
    override val type: SubscriptionType = SubscriptionType.EVENT_KEYWORD
}

@Schema(description = "채용 키워드 구독")
data class JobKeywordSubscriptionResponse(
    @field:Schema(description = "구독 ID", example = "2")
    override val id: Long,

    @field:Schema(description = "키워드", example = "간호사")
    val keyword: String,

    @field:Schema(description = "생성 시각")
    override val createdAt: LocalDateTime,
) : SubscriptionResponse {
    override val type: SubscriptionType = SubscriptionType.JOB_KEYWORD
}

@Schema(description = "행사 주최 구독")
data class EventHostSubscriptionResponse(
    @field:Schema(description = "구독 ID", example = "3")
    override val id: Long,

    @field:Schema(description = "구독한 주최")
    val host: SubscriptionTargetHost,

    @field:Schema(description = "생성 시각")
    override val createdAt: LocalDateTime,
) : SubscriptionResponse {
    override val type: SubscriptionType = SubscriptionType.EVENT_HOST
}

@Schema(description = "행사 유형 구독")
data class EventTypeSubscriptionResponse(
    @field:Schema(description = "구독 ID", example = "4")
    override val id: Long,

    @field:Schema(description = "구독한 행사 유형", example = "CONFERENCE")
    val eventType: EventType,

    @field:Schema(description = "생성 시각")
    override val createdAt: LocalDateTime,
) : SubscriptionResponse {
    override val type: SubscriptionType = SubscriptionType.EVENT_TYPE
}

@Schema(description = "회사 구독")
data class JobCompanySubscriptionResponse(
    @field:Schema(description = "구독 ID", example = "5")
    override val id: Long,

    @field:Schema(description = "구독한 회사")
    val company: SubscriptionTargetCompany,

    @field:Schema(description = "생성 시각")
    override val createdAt: LocalDateTime,
) : SubscriptionResponse {
    override val type: SubscriptionType = SubscriptionType.JOB_COMPANY
}

@Schema(description = "구독 대상 — 주최 요약")
data class SubscriptionTargetHost(
    @field:Schema(description = "주최 ID", example = "1") val id: Long,
    @field:Schema(description = "주최 이름", example = "테크 컨퍼런스") val name: String,
)

@Schema(description = "구독 대상 — 회사 요약")
data class SubscriptionTargetCompany(
    @field:Schema(description = "회사 ID", example = "42") val id: Long,
    @field:Schema(description = "회사명", example = "테스트병원") val name: String?,
)
