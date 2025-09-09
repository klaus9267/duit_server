package duit.server.domain.user.dto

import duit.server.domain.user.entity.AlarmSettings
import duit.server.domain.user.entity.User

data class UserResponse(
    val id: Long,
    val email: String?,
    val nickname: String,
    val providerId: String,
    val autoAddBookmarkToCalendar: Boolean,
    val alarmSettings: AlarmSettings,
) {
    companion object {
        fun from(user: User) =
            UserResponse(
                id = user.id!!,
                email = user.email,
                nickname = user.nickname,
                providerId = user.providerId!!,
                autoAddBookmarkToCalendar = user.autoAddBookmarkToCalendar,
                alarmSettings = user.alarmSettings
            )
    }
}
