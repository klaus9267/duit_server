package duit.server.application.controller.dto.user

import duit.server.domain.user.entity.User

data class UserResponse(
    val id: Long,
    val email: String?,
    val nickname: String,
    val allowPushAlarm: Boolean,
    val allowMarketingAlarm: Boolean,
) {
    companion object {
        fun from(user: User) =
            UserResponse(
                id = user.id!!,
                email = user.email,
                nickname = user.nickname,
                allowPushAlarm = user.allowPushAlarm,
                allowMarketingAlarm = user.allowMarketingAlarm
            )
    }
}
