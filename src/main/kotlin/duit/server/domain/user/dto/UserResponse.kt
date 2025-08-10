package duit.server.domain.user.dto

import duit.server.domain.user.entity.User

data class UserResponse(
    val id: Long,
    val email: String?,
    val nickname: String,
    val providerId: String,
    val allowPushAlarm: Boolean,
    val allowMarketingAlarm: Boolean,
) {
    companion object {
        fun from(user: User) =
            UserResponse(
                id = user.id!!,
                email = user.email,
                nickname = user.nickname,
                providerId = user.providerId!!,
                allowPushAlarm = user.allowPushAlarm,
                allowMarketingAlarm = user.allowMarketingAlarm
            )
    }
}
