package duit.server.domain.user.dto

import duit.server.domain.user.entity.AlarmSettings
import jakarta.validation.constraints.NotNull

data class UpdateUserSettingsRequest(
    @field:NotNull
    val autoAddBookmarkToCalendar: Boolean,
    @field:NotNull
    val alarmSettings: AlarmSettingsDto
) {
    data class AlarmSettingsDto(
        @field:NotNull
        val push: Boolean,
        @field:NotNull
        val bookmark: Boolean,
        @field:NotNull
        val calendar: Boolean,
        @field:NotNull
        val marketing: Boolean
    ) {
        fun toAlarmSettings() = AlarmSettings(
            push = push,
            bookmark = bookmark,
            calendar = calendar,
            marketing = marketing
        )
    }
}