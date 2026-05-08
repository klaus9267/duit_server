package duit.server.domain.alarm.dto

import com.fasterxml.jackson.annotation.JsonInclude
import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.event.dto.EventResponse
import duit.server.domain.job.dto.JobPostingResponse
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 알람 대상 폴리모픽 응답.
 *
 * Jackson 은 sealed interface 의 각 구현체를 자기 shape 으로 직렬화하므로,
 * `targetType` 필드만 보면 클라이언트가 어떤 타깃인지 분기 가능.
 *
 * JSON 예:
 * ```
 * { "targetType": "EVENT", "event": { ... } }
 * { "targetType": "JOB_POSTING", "jobPosting": { ... } }
 * ```
 */
@Schema(
    description = "알람이 가리키는 대상 (폴리모픽). targetType 으로 어떤 타입인지 식별.",
    oneOf = [EventAlarmTargetResponse::class, JobPostingAlarmTargetResponse::class],
)
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed interface AlarmTargetResponse {
    val targetType: AlarmTargetType
}

@Schema(description = "행사 타깃")
data class EventAlarmTargetResponse(
    @field:Schema(description = "행사 정보")
    val event: EventResponse,
) : AlarmTargetResponse {
    override val targetType: AlarmTargetType = AlarmTargetType.EVENT
}

@Schema(description = "채용공고 타깃")
data class JobPostingAlarmTargetResponse(
    @field:Schema(description = "채용공고 정보")
    val jobPosting: JobPostingResponse,
) : AlarmTargetResponse {
    override val targetType: AlarmTargetType = AlarmTargetType.JOB_POSTING
}

/**
 * Alarm 엔티티 → [AlarmTargetResponse] 변환.
 *
 * 새 대상 추가 시 분기 1줄만 추가하면 끝 (기존 코드 수정 X).
 */
object AlarmTargetResponseFactory {
    fun from(alarm: Alarm): AlarmTargetResponse = when {
        alarm.event != null -> EventAlarmTargetResponse(EventResponse.from(alarm.event!!))
        alarm.jobPosting != null -> JobPostingAlarmTargetResponse(JobPostingResponse.from(alarm.jobPosting!!))
        else -> error("Alarm id=${alarm.id} 에 target 이 없습니다")
    }
}
