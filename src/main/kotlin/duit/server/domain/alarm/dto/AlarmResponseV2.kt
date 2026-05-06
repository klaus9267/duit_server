package duit.server.domain.alarm.dto

import duit.server.domain.alarm.entity.Alarm
import duit.server.domain.alarm.entity.AlarmType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 알람 응답 V2 — 이벤트/채용 알람 모두 폴리모픽 [AlarmTargetResponse] 로 노출.
 *
 * 기존 [AlarmResponse] (V1, 이벤트 전용) 는 후방 호환을 위해 유지.
 */
@Schema(description = "알람 응답 V2 — 폴리모픽 target 지원")
data class AlarmResponseV2(
    @field:Schema(description = "알람 ID", example = "42")
    val id: Long,

    @field:Schema(description = "알람 사유", example = "EVENT_SUBSCRIPTION_HOST")
    val type: AlarmType,

    @field:Schema(description = "대상 (target)")
    val target: AlarmTargetResponse,

    @field:Schema(description = "읽음 여부", example = "false")
    val isRead: Boolean,

    @field:Schema(description = "생성 시각")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(alarm: Alarm): AlarmResponseV2 = AlarmResponseV2(
            id = alarm.id!!,
            type = alarm.type,
            target = AlarmTargetResponseFactory.from(alarm),
            isRead = alarm.isRead,
            createdAt = alarm.createdAt,
        )
    }
}
