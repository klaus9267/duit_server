package duit.server.domain.user.dto

import duit.server.domain.user.entity.AlarmSettings
import duit.server.domain.user.entity.User
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val email: String?,
    val nickname: String,
    val providerId: String,
    val autoAddBookmarkToCalendar: Boolean,
    val alarmSettings: AlarmSettings,
    @Schema(description = "마케팅 수신 동의 시각 (미동의면 null)", example = "2026-10-15T10:00:00")
    val marketingAgreedAt: LocalDateTime?,
) {
    companion object {
        fun from(user: User) =
            UserResponse(
                id = user.id!!,
                email = user.email,
                nickname = user.nickname,
                providerId = user.providerId!!,
                autoAddBookmarkToCalendar = user.autoAddBookmarkToCalendar,
                alarmSettings = user.alarmSettings,
                marketingAgreedAt = user.marketingAgreedAt,
            )
    }
}
